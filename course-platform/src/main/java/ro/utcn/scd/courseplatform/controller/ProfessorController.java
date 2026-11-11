package ro.utcn.scd.courseplatform.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ro.utcn.scd.courseplatform.dto.ProfessorDto;
import ro.utcn.scd.courseplatform.service.ProfessorService;

import java.util.List;

@RestController
@RequestMapping("/api/professors")
@RequiredArgsConstructor
public class ProfessorController {

    private final ProfessorService service;

    @GetMapping
    public List<ProfessorDto> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public ProfessorDto getById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProfessorDto create(@Valid @RequestBody ProfessorDto dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    public ProfessorDto update(@PathVariable Long id, @Valid @RequestBody ProfessorDto dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
