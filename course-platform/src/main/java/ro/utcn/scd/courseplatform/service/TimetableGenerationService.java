package ro.utcn.scd.courseplatform.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.utcn.scd.courseplatform.dto.GenerateTimetableRequest;
import ro.utcn.scd.courseplatform.dto.GenerationResultDto;
import ro.utcn.scd.courseplatform.dto.TimetableDto;
import ro.utcn.scd.courseplatform.model.*;
import ro.utcn.scd.courseplatform.model.enums.ConstraintType;
import ro.utcn.scd.courseplatform.model.enums.TimetableStatus;
import ro.utcn.scd.courseplatform.repository.*;
import ro.utcn.scd.courseplatform.scheduling.TimetableSolver;

import java.util.*;

/**
 * OPERATIE COMPLEXA: generarea automata a orarului.
 * Citeste asignarile, salile, sloturile si restrictiile profesorilor din baza de date,
 * ruleaza algoritmul (TimetableSolver) si salveaza rezultatul ca un orar nou in starea DRAFT.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TimetableGenerationService {

    private static final int DEFAULT_ATTEMPTS = 30;

    private final TeachingAssignmentRepository assignmentRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final RoomRepository roomRepository;
    private final ProfessorConstraintRepository constraintRepository;
    private final TimetableRepository timetableRepository;
    private final ScheduleEntryRepository entryRepository;
    private final AudienceResolver audienceResolver;

    @Transactional
    public GenerationResultDto generate(GenerateTimetableRequest request) {
        long start = System.currentTimeMillis();

        // 1. Datele de intrare pentru algoritm
        List<TeachingAssignment> assignments = assignmentRepository.findAll();
        Map<Long, TeachingAssignment> assignmentsById = new HashMap<>();
        AudienceResolver.Resolver resolver = audienceResolver.create();
        List<TimetableSolver.Task> tasks = new ArrayList<>();
        for (TeachingAssignment a : assignments) {
            assignmentsById.put(a.getId(), a);
            AudienceResolver.Audience audience = resolver.of(a);
            tasks.add(new TimetableSolver.Task(
                    a.getId(), a.getProfessor().getId(), a.getType(), a.getFrequency(),
                    a.getPreferredRoom() != null ? a.getPreferredRoom().getId() : null,
                    audience.subgroupIds(), audience.size(), label(a)));
        }

        Map<Long, TimeSlot> slotsById = new HashMap<>();
        List<TimetableSolver.Slot> slots = new ArrayList<>();
        for (TimeSlot ts : timeSlotRepository.findAll()) {
            slotsById.put(ts.getId(), ts);
            slots.add(new TimetableSolver.Slot(ts.getId(), ts.getDayOfWeek(), ts.getStartTime().getHour()));
        }

        Map<Long, Room> roomsById = new HashMap<>();
        List<TimetableSolver.RoomOption> rooms = new ArrayList<>();
        for (Room r : roomRepository.findAll()) {
            roomsById.put(r.getId(), r);
            rooms.add(new TimetableSolver.RoomOption(r.getId(), r.getCode(), r.getCapacity(), r.getType()));
        }

        Map<Long, Set<Long>> unavailable = new HashMap<>();
        Map<Long, Set<Long>> avoid = new HashMap<>();
        Map<Long, Set<Long>> preferred = new HashMap<>();
        for (ProfessorConstraint c : constraintRepository.findAll()) {
            Map<Long, Set<Long>> target = c.getType() == ConstraintType.PREFERRED ? preferred
                    : c.isMandatory() ? unavailable : avoid;
            target.computeIfAbsent(c.getProfessor().getId(), k -> new HashSet<>()).add(c.getTimeSlot().getId());
        }

        // 2. Rularea algoritmului
        int attempts = request.attempts() != null ? request.attempts() : DEFAULT_ATTEMPTS;
        long seed = request.seed() != null ? request.seed() : System.currentTimeMillis();
        TimetableSolver solver = new TimetableSolver(
                new TimetableSolver.Input(tasks, slots, rooms, unavailable, avoid, preferred));
        TimetableSolver.Result result = solver.solve(attempts, seed);

        // 3. Salvarea orarului (DRAFT) si a orelor plasate
        Timetable timetable = new Timetable();
        timetable.setName(request.name());
        timetable.setAcademicYear(request.academicYear());
        timetable.setSemester(request.semester());
        timetable.setStatus(TimetableStatus.DRAFT);
        timetable = timetableRepository.save(timetable);

        List<ScheduleEntry> entries = new ArrayList<>();
        for (TimetableSolver.Placement p : result.placements()) {
            ScheduleEntry e = new ScheduleEntry();
            e.setTimetable(timetable);
            e.setAssignment(assignmentsById.get(p.assignmentId()));
            e.setTimeSlot(slotsById.get(p.slotId()));
            e.setRoom(roomsById.get(p.roomId()));
            e.setParity(p.parity());
            entries.add(e);
        }
        entryRepository.saveAll(entries);

        long duration = System.currentTimeMillis() - start;
        log.info("Orar generat '{}': {}/{} activitati plasate in {} ms (scor {}).",
                timetable.getName(), result.placements().size(), tasks.size(), duration, result.score());

        List<String> unplaced = result.unplaced().stream()
                .map(u -> u.task().label() + " - " + u.reason())
                .toList();
        return new GenerationResultDto(
                TimetableDto.from(timetable, entries.size()),
                tasks.size(), result.placements().size(), unplaced,
                Math.round(result.score() * 10) / 10.0, result.attempts(), duration);
    }

    private String label(TeachingAssignment a) {
        String target = a.getSeries() != null ? a.getSeries().getName()
                : a.getGroup() != null ? a.getGroup().getCode()
                : a.getSubgroup() != null ? a.getSubgroup().getName() : "?";
        return a.getDiscipline().getCode() + " " + a.getType() + " (" + target + ")";
    }
}
