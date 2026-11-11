package ro.utcn.scd.courseplatform.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ro.utcn.scd.courseplatform.dto.BuildingDto;
import ro.utcn.scd.courseplatform.service.BuildingService;

import java.util.List;

@RestController
@RequestMapping("/api/buildings")
@RequiredArgsConstructor
public class BuildingController {

    private final BuildingService service;

    @GetMapping
    public List<BuildingDto> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public BuildingDto getById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BuildingDto create(@Valid @RequestBody BuildingDto dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    public BuildingDto update(@PathVariable Long id, @Valid @RequestBody BuildingDto dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
