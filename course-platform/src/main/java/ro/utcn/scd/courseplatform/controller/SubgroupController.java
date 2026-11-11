package ro.utcn.scd.courseplatform.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ro.utcn.scd.courseplatform.dto.SubgroupDto;
import ro.utcn.scd.courseplatform.service.SubgroupService;

import java.util.List;

@RestController
@RequestMapping("/api/subgroups")
@RequiredArgsConstructor
public class SubgroupController {

    private final SubgroupService service;

    /** Ex: GET /api/subgroups?groupId=1 */
    @GetMapping
    public List<SubgroupDto> getAll(@RequestParam(required = false) Long groupId) {
        return service.findAll(groupId);
    }

    @GetMapping("/{id}")
    public SubgroupDto getById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SubgroupDto create(@Valid @RequestBody SubgroupDto dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    public SubgroupDto update(@PathVariable Long id, @Valid @RequestBody SubgroupDto dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
