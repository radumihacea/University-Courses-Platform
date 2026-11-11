package ro.utcn.scd.courseplatform.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ro.utcn.scd.courseplatform.dto.StudentGroupDto;
import ro.utcn.scd.courseplatform.service.StudentGroupService;

import java.util.List;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class StudentGroupController {

    private final StudentGroupService service;

    /** Ex: GET /api/groups?seriesId=1 */
    @GetMapping
    public List<StudentGroupDto> getAll(@RequestParam(required = false) Long seriesId) {
        return service.findAll(seriesId);
    }

    @GetMapping("/{id}")
    public StudentGroupDto getById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StudentGroupDto create(@Valid @RequestBody StudentGroupDto dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    public StudentGroupDto update(@PathVariable Long id, @Valid @RequestBody StudentGroupDto dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
