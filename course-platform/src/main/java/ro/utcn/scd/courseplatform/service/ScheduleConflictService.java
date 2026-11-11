package ro.utcn.scd.courseplatform.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.utcn.scd.courseplatform.dto.ConflictDto;
import ro.utcn.scd.courseplatform.dto.TimeSlotDto;
import ro.utcn.scd.courseplatform.model.*;
import ro.utcn.scd.courseplatform.model.enums.ConstraintType;
import ro.utcn.scd.courseplatform.model.enums.WeekParity;
import ro.utcn.scd.courseplatform.repository.ProfessorConstraintRepository;
import ro.utcn.scd.courseplatform.repository.ScheduleEntryRepository;

import java.util.*;

/**
 * OPERATIE COMPLEXA: detectarea conflictelor dintr-un orar si verificarea
 * unei mutari inainte de a fi aplicata (folosita la aprobarea cererilor de modificare).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleConflictService {

    private final ScheduleEntryRepository entryRepository;
    private final ProfessorConstraintRepository constraintRepository;
    private final AudienceResolver audienceResolver;

    /** Toate conflictele dintr-un orar: sala, profesor, studenti si capacitate. */
    public List<ConflictDto> findConflicts(Long timetableId) {
        List<ConflictDto> conflicts = new ArrayList<>();

        // Sala / profesor: direct din baza de date (query custom)
        for (Object[] pair : entryRepository.findRoomOrProfessorOverlaps(timetableId)) {
            ScheduleEntry e1 = (ScheduleEntry) pair[0];
            ScheduleEntry e2 = (ScheduleEntry) pair[1];
            if (e1.getRoom().getId().equals(e2.getRoom().getId())) {
                conflicts.add(new ConflictDto("ROOM",
                        "Sala " + e1.getRoom().getCode() + " este folosita simultan de "
                                + describe(e1) + " si " + describe(e2), e1.getId(), e2.getId()));
            }
            if (e1.getAssignment().getProfessor().getId().equals(e2.getAssignment().getProfessor().getId())) {
                conflicts.add(new ConflictDto("PROFESSOR",
                        e1.getAssignment().getProfessor().getUser().getFullName() + " are simultan "
                                + describe(e1) + " si " + describe(e2), e1.getId(), e2.getId()));
            }
        }

        // Studenti si capacitate: calculate in Java pe baza publicului fiecarei ore
        List<ScheduleEntry> entries = entryRepository.findAllOfTimetable(timetableId);
        AudienceResolver.Resolver resolver = audienceResolver.create();
        Map<Long, AudienceResolver.Audience> audiences = new HashMap<>();
        entries.forEach(e -> audiences.put(e.getId(), resolver.of(e.getAssignment())));

        for (ScheduleEntry e : entries) {
            int size = audiences.get(e.getId()).size();
            if (size > e.getRoom().getCapacity()) {
                conflicts.add(new ConflictDto("CAPACITY",
                        describe(e) + " are " + size + " studenti, dar sala " + e.getRoom().getCode()
                                + " are doar " + e.getRoom().getCapacity() + " locuri", e.getId(), null));
            }
        }
        for (int i = 0; i < entries.size(); i++) {
            for (int j = i + 1; j < entries.size(); j++) {
                ScheduleEntry a = entries.get(i);
                ScheduleEntry b = entries.get(j);
                if (overlapInTime(a.getTimeSlot(), a.getParity(), b.getTimeSlot(), b.getParity())
                        && intersects(audiences.get(a.getId()).subgroupIds(), audiences.get(b.getId()).subgroupIds())) {
                    conflicts.add(new ConflictDto("STUDENTS",
                            "Aceiasi studenti au simultan " + describe(a) + " si " + describe(b), a.getId(), b.getId()));
                }
            }
        }
        return conflicts;
    }

    /**
     * Verifica daca o ora poate fi mutata in alt slot / alta sala fara conflicte.
     * Intoarce lista de probleme (goala = mutarea este posibila).
     */
    public List<String> checkMove(ScheduleEntry entry, TimeSlot newSlot, Room newRoom) {
        List<String> problems = new ArrayList<>();
        TeachingAssignment assignment = entry.getAssignment();
        AudienceResolver.Resolver resolver = audienceResolver.create();
        AudienceResolver.Audience audience = resolver.of(assignment);

        if (audience.size() > newRoom.getCapacity()) {
            problems.add("Sala " + newRoom.getCode() + " are doar " + newRoom.getCapacity()
                    + " locuri pentru " + audience.size() + " studenti.");
        }
        boolean unavailable = constraintRepository.findByProfessorId(assignment.getProfessor().getId()).stream()
                .anyMatch(c -> c.getType() == ConstraintType.UNAVAILABLE && c.isMandatory()
                        && c.getTimeSlot().getId().equals(newSlot.getId()));
        if (unavailable) {
            problems.add("Profesorul este indisponibil " + TimeSlotDto.label(newSlot) + ".");
        }

        for (ScheduleEntry other : entryRepository.findAllOfTimetable(entry.getTimetable().getId())) {
            if (other.getId().equals(entry.getId())
                    || !overlapInTime(newSlot, entry.getParity(), other.getTimeSlot(), other.getParity())) {
                continue;
            }
            if (other.getRoom().getId().equals(newRoom.getId())) {
                problems.add("Sala " + newRoom.getCode() + " este ocupata de " + describe(other) + ".");
            }
            if (other.getAssignment().getProfessor().getId().equals(assignment.getProfessor().getId())) {
                problems.add("Profesorul are deja " + describe(other) + ".");
            }
            if (intersects(audience.subgroupIds(), resolver.of(other.getAssignment()).subgroupIds())) {
                problems.add("Studentii au deja " + describe(other) + ".");
            }
        }
        return problems;
    }

    /** Acelasi slot si paritati compatibile (ALL se suprapune cu orice). */
    static boolean overlapInTime(TimeSlot s1, WeekParity p1, TimeSlot s2, WeekParity p2) {
        return s1.getId().equals(s2.getId())
                && (p1 == WeekParity.ALL || p2 == WeekParity.ALL || p1 == p2);
    }

    private static boolean intersects(Set<Long> a, Set<Long> b) {
        for (Long x : a) {
            if (b.contains(x)) return true;
        }
        return false;
    }

    static String describe(ScheduleEntry e) {
        TeachingAssignment a = e.getAssignment();
        String target = a.getSeries() != null ? a.getSeries().getName()
                : a.getGroup() != null ? a.getGroup().getCode()
                : a.getSubgroup() != null ? a.getSubgroup().getName() : "";
        String parity = e.getParity() == WeekParity.ALL ? ""
                : e.getParity() == WeekParity.ODD ? " (sapt. impare)" : " (sapt. pare)";
        return a.getDiscipline().getCode() + " " + a.getType() + " " + target
                + " [" + TimeSlotDto.label(e.getTimeSlot()) + parity + "]";
    }
}
