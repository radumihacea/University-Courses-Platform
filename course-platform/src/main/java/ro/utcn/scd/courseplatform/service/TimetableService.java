package ro.utcn.scd.courseplatform.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.utcn.scd.courseplatform.dto.ConflictDto;
import ro.utcn.scd.courseplatform.dto.ScheduleEntryDto;
import ro.utcn.scd.courseplatform.dto.TimetableDto;
import ro.utcn.scd.courseplatform.exception.BadRequestException;
import ro.utcn.scd.courseplatform.exception.NotFoundException;
import ro.utcn.scd.courseplatform.model.ScheduleEntry;
import ro.utcn.scd.courseplatform.model.Timetable;
import ro.utcn.scd.courseplatform.model.enums.Role;
import ro.utcn.scd.courseplatform.model.enums.TimetableStatus;
import ro.utcn.scd.courseplatform.repository.ChangeRequestRepository;
import ro.utcn.scd.courseplatform.repository.ScheduleEntryRepository;
import ro.utcn.scd.courseplatform.repository.TimetableRepository;
import ro.utcn.scd.courseplatform.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

/** Gestionarea versiunilor de orar: listare, vizualizare filtrata, publicare, stergere, conflicte. */
@Service
@RequiredArgsConstructor
@Transactional
public class TimetableService {

    private final TimetableRepository timetableRepository;
    private final ScheduleEntryRepository entryRepository;
    private final ChangeRequestRepository changeRequestRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final ScheduleConflictService conflictService;

    @Transactional(readOnly = true)
    public List<TimetableDto> findAll() {
        return timetableRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(t -> TimetableDto.from(t, entryRepository.countByTimetableId(t.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public TimetableDto findById(Long id) {
        return TimetableDto.from(get(id), entryRepository.countByTimetableId(id));
    }

    /**
     * OPERATIE COMPLEXA: orarul filtrat dupa semigrupa, profesor sau sala.
     * Pentru semigrupa se includ si cursurile seriei si seminariile grupei (query custom).
     */
    @Transactional(readOnly = true)
    public List<ScheduleEntryDto> entries(Long timetableId, Long subgroupId, Long professorId, Long roomId) {
        get(timetableId);
        Stream<ScheduleEntry> stream = subgroupId != null
                ? entryRepository.findForSubgroup(timetableId, subgroupId).stream()
                : entryRepository.findAllOfTimetable(timetableId).stream();
        if (professorId != null) {
            stream = stream.filter(e -> e.getAssignment().getProfessor().getId().equals(professorId));
        }
        if (roomId != null) {
            stream = stream.filter(e -> e.getRoom().getId().equals(roomId));
        }
        return stream.map(ScheduleEntryDto::from).sorted(ScheduleEntryDto.CHRONOLOGICAL).toList();
    }

    @Transactional(readOnly = true)
    public List<ConflictDto> conflicts(Long timetableId) {
        get(timetableId);
        return conflictService.findConflicts(timetableId);
    }

    /**
     * OPERATIE COMPLEXA: publicarea unui orar.
     * Orarul publicat anterior pentru acelasi an si semestru este arhivat,
     * iar toti studentii si profesorii primesc o notificare.
     */
    public TimetableDto publish(Long id) {
        Timetable timetable = get(id);
        if (timetable.getStatus() == TimetableStatus.PUBLISHED) {
            throw new BadRequestException("Orarul este deja publicat.");
        }
        List<ConflictDto> conflicts = conflictService.findConflicts(id);
        if (!conflicts.isEmpty()) {
            throw new BadRequestException("Orarul are " + conflicts.size()
                    + " conflicte si nu poate fi publicat. Verifica /api/timetables/" + id + "/conflicts.");
        }
        for (Timetable other : timetableRepository.findByStatus(TimetableStatus.PUBLISHED)) {
            if (other.getAcademicYear().equals(timetable.getAcademicYear())
                    && other.getSemester().equals(timetable.getSemester())) {
                other.setStatus(TimetableStatus.ARCHIVED);
            }
        }
        timetable.setStatus(TimetableStatus.PUBLISHED);
        timetable.setPublishedAt(LocalDateTime.now());
        timetableRepository.saveAndFlush(timetable);

        notificationService.notifyUsers(
                userRepository.findByRoleIn(List.of(Role.STUDENT, Role.PROFESSOR)),
                "A fost publicat orarul \"" + timetable.getName() + "\" (" + timetable.getAcademicYear()
                        + ", semestrul " + timetable.getSemester() + "). Verifica-ti orarul!");
        return TimetableDto.from(timetable, entryRepository.countByTimetableId(id));
    }

    /** Sterge un orar DRAFT sau ARCHIVED, impreuna cu orele si cererile de modificare legate de el. */
    public void delete(Long id) {
        Timetable timetable = get(id);
        if (timetable.getStatus() == TimetableStatus.PUBLISHED) {
            throw new BadRequestException("Orarul publicat nu poate fi sters. Publica intai alt orar.");
        }
        changeRequestRepository.deleteAllOfTimetable(id);
        entryRepository.deleteAllOfTimetable(id);
        timetableRepository.delete(timetable);
        timetableRepository.flush();
    }

    private Timetable get(Long id) {
        return timetableRepository.findById(id).orElseThrow(() -> new NotFoundException("Orarul", id));
    }
}
