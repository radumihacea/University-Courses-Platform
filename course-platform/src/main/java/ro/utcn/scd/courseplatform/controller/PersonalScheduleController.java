package ro.utcn.scd.courseplatform.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import ro.utcn.scd.courseplatform.dto.DayScheduleDto;
import ro.utcn.scd.courseplatform.dto.ScheduleEntryDto;
import ro.utcn.scd.courseplatform.service.PersonalScheduleService;

import java.time.LocalDate;
import java.util.List;

/** Orarul personal din orarul publicat - folosit in principal de aplicatia mobila. */
@RestController
@RequestMapping("/api/schedule")
@RequiredArgsConstructor
public class PersonalScheduleController {

    private final PersonalScheduleService service;

    @GetMapping("/students/{studentId}/week")
    public List<ScheduleEntryDto> studentWeek(@PathVariable Long studentId) {
        return service.studentWeek(studentId);
    }

    /** Ex: GET /api/schedule/students/1/day?date=2026-11-12 (fara date = azi) */
    @GetMapping("/students/{studentId}/day")
    public DayScheduleDto studentDay(@PathVariable Long studentId,
                                     @RequestParam(required = false)
                                     @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.studentDay(studentId, date != null ? date : LocalDate.now());
    }

    @GetMapping("/professors/{professorId}/week")
    public List<ScheduleEntryDto> professorWeek(@PathVariable Long professorId) {
        return service.professorWeek(professorId);
    }

    @GetMapping("/professors/{professorId}/day")
    public DayScheduleDto professorDay(@PathVariable Long professorId,
                                       @RequestParam(required = false)
                                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.professorDay(professorId, date != null ? date : LocalDate.now());
    }
}
