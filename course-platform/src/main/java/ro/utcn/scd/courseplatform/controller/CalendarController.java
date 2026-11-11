package ro.utcn.scd.courseplatform.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import ro.utcn.scd.courseplatform.dto.SemesterWeekDto;
import ro.utcn.scd.courseplatform.dto.TimeSlotDto;
import ro.utcn.scd.courseplatform.service.CalendarService;

import java.util.List;

/** Date fixe pentru frontend / mobil: sloturile orare si saptamanile semestrului. */
@RestController
@RequiredArgsConstructor
public class CalendarController {

    private final CalendarService service;

    @GetMapping("/api/time-slots")
    public List<TimeSlotDto> timeSlots() {
        return service.timeSlots();
    }

    @GetMapping("/api/semester-weeks")
    public List<SemesterWeekDto> semesterWeeks() {
        return service.semesterWeeks();
    }
}
