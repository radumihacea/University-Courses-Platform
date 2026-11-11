package ro.utcn.scd.courseplatform.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.utcn.scd.courseplatform.dto.DayScheduleDto;
import ro.utcn.scd.courseplatform.dto.ScheduleEntryDto;
import ro.utcn.scd.courseplatform.model.Enrollment;
import ro.utcn.scd.courseplatform.model.ScheduleEntry;
import ro.utcn.scd.courseplatform.model.SemesterWeek;
import ro.utcn.scd.courseplatform.model.Student;
import ro.utcn.scd.courseplatform.model.Timetable;
import ro.utcn.scd.courseplatform.model.enums.TimetableStatus;
import ro.utcn.scd.courseplatform.model.enums.WeekParity;
import ro.utcn.scd.courseplatform.repository.EnrollmentRepository;
import ro.utcn.scd.courseplatform.repository.ScheduleEntryRepository;
import ro.utcn.scd.courseplatform.repository.SemesterWeekRepository;
import ro.utcn.scd.courseplatform.repository.TimetableRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * OPERATIE COMPLEXA: orarul personal al unui student / profesor, din orarul PUBLICAT.
 * Pentru o zi anume se tine cont de calendarul semestrului (saptamana para / impara,
 * vacanta, weekend). Disciplinele optionale apar doar daca studentul este inrolat.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PersonalScheduleService {

    private final TimetableRepository timetableRepository;
    private final ScheduleEntryRepository entryRepository;
    private final SemesterWeekRepository semesterWeekRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final EntityLookup lookup;

    public List<ScheduleEntryDto> studentWeek(Long studentId) {
        Student student = lookup.student(studentId);
        return publishedTimetable()
                .map(t -> studentEntries(student, t).stream()
                        .map(ScheduleEntryDto::from).sorted(ScheduleEntryDto.CHRONOLOGICAL).toList())
                .orElse(List.of());
    }

    public List<ScheduleEntryDto> professorWeek(Long professorId) {
        lookup.professor(professorId);
        return publishedTimetable()
                .map(t -> professorEntries(professorId, t).stream()
                        .map(ScheduleEntryDto::from).sorted(ScheduleEntryDto.CHRONOLOGICAL).toList())
                .orElse(List.of());
    }

    public DayScheduleDto studentDay(Long studentId, LocalDate date) {
        Student student = lookup.student(studentId);
        return day(date, t -> studentEntries(student, t));
    }

    public DayScheduleDto professorDay(Long professorId, LocalDate date) {
        lookup.professor(professorId);
        return day(date, t -> professorEntries(professorId, t));
    }

    // ------------------------------------------------------------------

    private DayScheduleDto day(LocalDate date, java.util.function.Function<Timetable, List<ScheduleEntry>> source) {
        if (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return new DayScheduleDto(date, null, null, false, "Este weekend - nu ai ore.", List.of());
        }
        Optional<SemesterWeek> week = semesterWeekRepository
                .findFirstByStartDateLessThanEqualAndEndDateGreaterThanEqual(date, date);
        if (week.isEmpty()) {
            return new DayScheduleDto(date, null, null, false, "Data nu face parte din semestru.", List.of());
        }
        SemesterWeek w = week.get();
        if (w.isHoliday()) {
            return new DayScheduleDto(date, null, WeekParity.ALL, true, "Este vacanta - nu ai ore.", List.of());
        }
        Optional<Timetable> timetable = publishedTimetable();
        if (timetable.isEmpty()) {
            return new DayScheduleDto(date, w.getWeekNumber(), w.getParity(), false,
                    "Nu exista inca un orar publicat.", List.of());
        }
        List<ScheduleEntryDto> entries = source.apply(timetable.get()).stream()
                .filter(e -> e.getTimeSlot().getDayOfWeek() == date.getDayOfWeek())
                .filter(e -> e.getParity() == WeekParity.ALL || e.getParity() == w.getParity())
                .map(ScheduleEntryDto::from)
                .sorted(ScheduleEntryDto.CHRONOLOGICAL)
                .toList();
        String parityText = w.getParity() == WeekParity.ODD ? "impara" : "para";
        String message = "Saptamana " + w.getWeekNumber() + " (" + parityText + "): "
                + (entries.isEmpty() ? "nu ai ore in aceasta zi." : entries.size() + " activitati.");
        return new DayScheduleDto(date, w.getWeekNumber(), w.getParity(), false, message, entries);
    }

    private Optional<Timetable> publishedTimetable() {
        return timetableRepository.findFirstByStatusOrderByPublishedAtDesc(TimetableStatus.PUBLISHED);
    }

    /** Orele semigrupei studentului; optionalele doar daca este inrolat la ele. */
    private List<ScheduleEntry> studentEntries(Student student, Timetable timetable) {
        Set<Long> enrolledAssignments = enrollmentRepository.findByStudentId(student.getId()).stream()
                .map(Enrollment::getAssignment)
                .map(a -> a.getId())
                .collect(Collectors.toSet());
        return entryRepository.findForSubgroup(timetable.getId(), student.getSubgroup().getId()).stream()
                .filter(e -> !e.getAssignment().getDiscipline().isOptional()
                        || enrolledAssignments.contains(e.getAssignment().getId()))
                .toList();
    }

    private List<ScheduleEntry> professorEntries(Long professorId, Timetable timetable) {
        return entryRepository.findAllOfTimetable(timetable.getId()).stream()
                .filter(e -> e.getAssignment().getProfessor().getId().equals(professorId))
                .toList();
    }
}
