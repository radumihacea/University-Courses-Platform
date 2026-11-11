package ro.utcn.scd.courseplatform.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.utcn.scd.courseplatform.dto.SemesterWeekDto;
import ro.utcn.scd.courseplatform.dto.TimeSlotDto;
import ro.utcn.scd.courseplatform.repository.SemesterWeekRepository;
import ro.utcn.scd.courseplatform.repository.TimeSlotRepository;

import java.util.List;

/** Date fixe, doar pentru citire: sloturile orare si calendarul semestrului. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CalendarService {

    private final TimeSlotRepository timeSlotRepository;
    private final SemesterWeekRepository semesterWeekRepository;

    public List<TimeSlotDto> timeSlots() {
        // Sortam dupa id: sloturile au fost create in ordinea luni 8-10 ... vineri 20-22.
        // (Sortarea dupa dayOfWeek ar fi alfabetica: FRIDAY, MONDAY, ...)
        return timeSlotRepository.findAll(Sort.by("id")).stream()
                .map(TimeSlotDto::from).toList();
    }

    public List<SemesterWeekDto> semesterWeeks() {
        return semesterWeekRepository.findAll(Sort.by("startDate")).stream()
                .map(SemesterWeekDto::from).toList();
    }
}
