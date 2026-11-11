package ro.utcn.scd.courseplatform.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ro.utcn.scd.courseplatform.dto.RoomDto;
import ro.utcn.scd.courseplatform.model.enums.RoomType;
import ro.utcn.scd.courseplatform.model.enums.WeekParity;
import ro.utcn.scd.courseplatform.service.RoomService;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService service;

    /** Ex: GET /api/rooms?type=LAB&buildingId=1 (ambii parametri optionali) */
    @GetMapping
    public List<RoomDto> getAll(@RequestParam(required = false) RoomType type,
                                @RequestParam(required = false) Long buildingId) {
        return service.findAll(type, buildingId);
    }

    /**
     * Salile libere intr-un slot al unui orar.
     * Ex: GET /api/rooms/available?timetableId=1&timeSlotId=10&parity=ODD&minCapacity=15&type=LAB
     */
    @GetMapping("/available")
    public List<RoomDto> getAvailable(@RequestParam Long timetableId,
                                      @RequestParam Long timeSlotId,
                                      @RequestParam(defaultValue = "ALL") WeekParity parity,
                                      @RequestParam(defaultValue = "0") int minCapacity,
                                      @RequestParam(required = false) RoomType type) {
        return service.findAvailable(timetableId, timeSlotId, parity, minCapacity, type);
    }

    @GetMapping("/{id}")
    public RoomDto getById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RoomDto create(@Valid @RequestBody RoomDto dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    public RoomDto update(@PathVariable Long id, @Valid @RequestBody RoomDto dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
