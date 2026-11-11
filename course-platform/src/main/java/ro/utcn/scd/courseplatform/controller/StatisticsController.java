package ro.utcn.scd.courseplatform.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ro.utcn.scd.courseplatform.dto.ProfessorLoadDto;
import ro.utcn.scd.courseplatform.dto.RoomOccupancyDto;
import ro.utcn.scd.courseplatform.service.StatisticsService;

import java.util.List;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService service;

    /** Ex: GET /api/statistics/professor-load?timetableId=1 */
    @GetMapping("/professor-load")
    public List<ProfessorLoadDto> professorLoad(@RequestParam Long timetableId) {
        return service.professorLoad(timetableId);
    }

    /** Ex: GET /api/statistics/room-occupancy?timetableId=1 */
    @GetMapping("/room-occupancy")
    public List<RoomOccupancyDto> roomOccupancy(@RequestParam Long timetableId) {
        return service.roomOccupancy(timetableId);
    }
}
