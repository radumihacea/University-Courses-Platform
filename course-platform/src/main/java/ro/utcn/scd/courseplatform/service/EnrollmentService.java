package ro.utcn.scd.courseplatform.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.utcn.scd.courseplatform.dto.OptionalCourseDto;
import ro.utcn.scd.courseplatform.dto.TimeSlotDto;
import ro.utcn.scd.courseplatform.exception.BadRequestException;
import ro.utcn.scd.courseplatform.model.Enrollment;
import ro.utcn.scd.courseplatform.model.ScheduleEntry;
import ro.utcn.scd.courseplatform.model.Student;
import ro.utcn.scd.courseplatform.model.Subgroup;
import ro.utcn.scd.courseplatform.model.TeachingAssignment;
import ro.utcn.scd.courseplatform.model.enums.Role;
import ro.utcn.scd.courseplatform.model.enums.TimetableStatus;
import ro.utcn.scd.courseplatform.model.enums.WeekParity;
import ro.utcn.scd.courseplatform.repository.EnrollmentRepository;
import ro.utcn.scd.courseplatform.repository.ScheduleEntryRepository;
import ro.utcn.scd.courseplatform.repository.TeachingAssignmentRepository;
import ro.utcn.scd.courseplatform.repository.TimetableRepository;
import ro.utcn.scd.courseplatform.security.AuthUser;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Inscrierea studentilor la disciplinele optionale (ex. Marketing, Dezvoltare personala).
 * Optionalele apar in orarul personal al studentului doar dupa inscriere.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final TeachingAssignmentRepository assignmentRepository;
    private final TimetableRepository timetableRepository;
    private final ScheduleEntryRepository entryRepository;
    private final EntityLookup lookup;

    /** Optionalele disponibile pentru student (ale seriei / grupei / semigrupei lui), cu starea inscrierii. */
    @Transactional(readOnly = true)
    public List<OptionalCourseDto> optionals(Long studentId) {
        Student student = lookup.student(studentId);
        Set<Long> enrolled = enrollmentRepository.findByStudentId(studentId).stream()
                .map(e -> e.getAssignment().getId())
                .collect(Collectors.toSet());
        List<TeachingAssignment> available = assignmentRepository.findAll().stream()
                .filter(a -> a.getDiscipline().isOptional() && isFor(a, student.getSubgroup()))
                .sorted(Comparator.comparing((TeachingAssignment a) -> a.getDiscipline().getCode())
                        .thenComparing(TeachingAssignment::getType))
                .toList();
        Map<Long, List<String>> schedule = scheduleOf(available);
        return available.stream()
                .map(a -> new OptionalCourseDto(a.getId(), a.getDiscipline().getCode(), a.getDiscipline().getName(),
                        a.getDiscipline().getCredits(), a.getType(), a.getProfessor().getUser().getFullName(),
                        enrolled.contains(a.getId()), schedule.getOrDefault(a.getId(), List.of())))
                .toList();
    }

    public List<OptionalCourseDto> enroll(AuthUser user, Long studentId, Long assignmentId) {
        Student student = ownStudent(user, studentId);
        TeachingAssignment assignment = lookup.assignment(assignmentId);
        if (!assignment.getDiscipline().isOptional()) {
            throw new BadRequestException(assignment.getDiscipline().getCode() + " nu este o disciplina optionala.");
        }
        if (!isFor(assignment, student.getSubgroup())) {
            throw new BadRequestException("Aceasta optionala nu este disponibila pentru semigrupa ta.");
        }
        if (enrollmentRepository.findByStudentIdAndAssignmentId(studentId, assignmentId).isEmpty()) {
            Enrollment e = new Enrollment();
            e.setStudent(student);
            e.setAssignment(assignment);
            enrollmentRepository.saveAndFlush(e);
        }
        return optionals(studentId);
    }

    public List<OptionalCourseDto> unenroll(AuthUser user, Long studentId, Long assignmentId) {
        ownStudent(user, studentId);
        enrollmentRepository.findByStudentIdAndAssignmentId(studentId, assignmentId)
                .ifPresent(e -> {
                    enrollmentRepository.delete(e);
                    enrollmentRepository.flush();
                });
        return optionals(studentId);
    }

    // ------------------------------------------------------------------

    /** Un student isi poate modifica doar propriile optionale; adminul le poate modifica pe ale oricui. */
    private Student ownStudent(AuthUser user, Long studentId) {
        Student student = lookup.student(studentId);
        if (user.role() == Role.STUDENT && !student.getUser().getId().equals(user.userId())) {
            throw new BadRequestException("Poti modifica doar propriile optionale.");
        }
        if (user.role() == Role.PROFESSOR) {
            throw new BadRequestException("Doar studentii (sau adminul) pot face inscrieri.");
        }
        return student;
    }

    /** Activitatea este pentru semigrupa data (direct, prin grupa sau prin serie)? */
    private static boolean isFor(TeachingAssignment a, Subgroup subgroup) {
        if (a.getSubgroup() != null) {
            return a.getSubgroup().getId().equals(subgroup.getId());
        }
        if (a.getGroup() != null) {
            return a.getGroup().getId().equals(subgroup.getGroup().getId());
        }
        return a.getSeries() != null && a.getSeries().getId().equals(subgroup.getGroup().getSeries().getId());
    }

    /** Cand si unde are loc fiecare activitate in orarul publicat, ex: "Miercuri 08:00-10:00, sala 356". */
    private Map<Long, List<String>> scheduleOf(List<TeachingAssignment> assignments) {
        Map<Long, List<String>> result = new HashMap<>();
        Set<Long> ids = assignments.stream().map(TeachingAssignment::getId).collect(Collectors.toSet());
        timetableRepository.findFirstByStatusOrderByPublishedAtDesc(TimetableStatus.PUBLISHED).ifPresent(t -> {
            for (ScheduleEntry e : entryRepository.findAllOfTimetable(t.getId())) {
                if (ids.contains(e.getAssignment().getId())) {
                    String parity = e.getParity() == WeekParity.ALL ? ""
                            : e.getParity() == WeekParity.ODD ? " (sapt. impare)" : " (sapt. pare)";
                    result.computeIfAbsent(e.getAssignment().getId(), k -> new ArrayList<>())
                            .add(TimeSlotDto.label(e.getTimeSlot()) + ", sala " + e.getRoom().getCode() + parity);
                }
            }
        });
        return result;
    }
}
