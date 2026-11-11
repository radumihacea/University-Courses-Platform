package ro.utcn.scd.courseplatform.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ro.utcn.scd.courseplatform.dto.ProfessorConstraintDto;
import ro.utcn.scd.courseplatform.service.ProfessorConstraintService;

import java.util.List;

@RestController
@RequestMapping("/api/professor-constraints")
@RequiredArgsConstructor
public class ProfessorConstraintController {

    private final ProfessorConstraintService service;

    /** Ex: GET /api/professor-constraints?professorId=4 */
    @GetMapping
    public List<ProfessorConstraintDto> getAll(@RequestParam(required = false) Long professorId) {
        return service.findAll(professorId);
    }

    @GetMapping("/{id}")
    public ProfessorConstraintDto getById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProfessorConstraintDto create(@Valid @RequestBody ProfessorConstraintDto dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    public ProfessorConstraintDto update(@PathVariable Long id, @Valid @RequestBody ProfessorConstraintDto dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
