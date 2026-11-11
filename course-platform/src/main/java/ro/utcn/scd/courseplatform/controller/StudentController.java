package ro.utcn.scd.courseplatform.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ro.utcn.scd.courseplatform.dto.StudentDto;
import ro.utcn.scd.courseplatform.service.StudentService;

import java.util.List;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService service;

    /** Ex: GET /api/students?subgroupId=3 */
    @GetMapping
    public List<StudentDto> getAll(@RequestParam(required = false) Long subgroupId) {
        return service.findAll(subgroupId);
    }

    @GetMapping("/{id}")
    public StudentDto getById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StudentDto create(@Valid @RequestBody StudentDto dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    public StudentDto update(@PathVariable Long id, @Valid @RequestBody StudentDto dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
