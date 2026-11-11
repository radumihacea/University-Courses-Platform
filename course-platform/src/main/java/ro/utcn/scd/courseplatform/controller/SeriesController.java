package ro.utcn.scd.courseplatform.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ro.utcn.scd.courseplatform.dto.SeriesDto;
import ro.utcn.scd.courseplatform.service.SeriesService;

import java.util.List;

@RestController
@RequestMapping("/api/series")
@RequiredArgsConstructor
public class SeriesController {

    private final SeriesService service;

    @GetMapping
    public List<SeriesDto> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public SeriesDto getById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SeriesDto create(@Valid @RequestBody SeriesDto dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    public SeriesDto update(@PathVariable Long id, @Valid @RequestBody SeriesDto dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
