package ro.utcn.scd.courseplatform.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ro.utcn.scd.courseplatform.dto.TeachingAssignmentDto;
import ro.utcn.scd.courseplatform.model.enums.ActivityType;
import ro.utcn.scd.courseplatform.service.TeachingAssignmentService;

import java.util.List;

@RestController
@RequestMapping("/api/teaching-assignments")
@RequiredArgsConstructor
public class TeachingAssignmentController {

    private final TeachingAssignmentService service;

    /** Ex: GET /api/teaching-assignments?professorId=15&type=LAB (toti parametrii optionali) */
    @GetMapping
    public List<TeachingAssignmentDto> getAll(@RequestParam(required = false) Long professorId,
                                              @RequestParam(required = false) Long disciplineId,
                                              @RequestParam(required = false) Long subgroupId,
                                              @RequestParam(required = false) ActivityType type) {
        return service.findAll(professorId, disciplineId, subgroupId, type);
    }

    @GetMapping("/{id}")
    public TeachingAssignmentDto getById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TeachingAssignmentDto create(@Valid @RequestBody TeachingAssignmentDto dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    public TeachingAssignmentDto update(@PathVariable Long id, @Valid @RequestBody TeachingAssignmentDto dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
