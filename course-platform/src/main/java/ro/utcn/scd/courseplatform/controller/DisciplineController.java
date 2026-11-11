package ro.utcn.scd.courseplatform.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ro.utcn.scd.courseplatform.dto.DisciplineDto;
import ro.utcn.scd.courseplatform.service.DisciplineService;

import java.util.List;

@RestController
@RequestMapping("/api/disciplines")
@RequiredArgsConstructor
public class DisciplineController {

    private final DisciplineService service;

    /** Ex: GET /api/disciplines?studyYear=4&optional=true */
    @GetMapping
    public List<DisciplineDto> getAll(@RequestParam(required = false) Integer studyYear,
                                      @RequestParam(required = false) Boolean optional) {
        return service.findAll(studyYear, optional);
    }

    @GetMapping("/{id}")
    public DisciplineDto getById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DisciplineDto create(@Valid @RequestBody DisciplineDto dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    public DisciplineDto update(@PathVariable Long id, @Valid @RequestBody DisciplineDto dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
