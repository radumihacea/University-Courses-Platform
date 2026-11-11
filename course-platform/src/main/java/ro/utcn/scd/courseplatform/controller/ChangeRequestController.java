package ro.utcn.scd.courseplatform.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ro.utcn.scd.courseplatform.dto.ChangeRequestDto;
import ro.utcn.scd.courseplatform.dto.DecisionDto;
import ro.utcn.scd.courseplatform.model.enums.RequestStatus;
import ro.utcn.scd.courseplatform.service.ChangeRequestService;

import java.util.List;

@RestController
@RequestMapping("/api/change-requests")
@RequiredArgsConstructor
public class ChangeRequestController {

    private final ChangeRequestService service;

    /** Ex: GET /api/change-requests?status=PENDING&professorId=4 */
    @GetMapping
    public List<ChangeRequestDto> getAll(@RequestParam(required = false) RequestStatus status,
                                         @RequestParam(required = false) Long professorId) {
        return service.findAll(status, professorId);
    }

    @GetMapping("/{id}")
    public ChangeRequestDto getById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ChangeRequestDto create(@Valid @RequestBody ChangeRequestDto dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    public ChangeRequestDto update(@PathVariable Long id, @Valid @RequestBody ChangeRequestDto dto) {
        return service.update(id, dto);
    }

    /** Aproba cererea: verifica conflictele, muta ora si notifica profesorul si studentii. */
    @PostMapping("/{id}/approve")
    public ChangeRequestDto approve(@PathVariable Long id, @Valid @RequestBody(required = false) DecisionDto decision) {
        return service.approve(id, decision);
    }

    @PostMapping("/{id}/reject")
    public ChangeRequestDto reject(@PathVariable Long id, @Valid @RequestBody(required = false) DecisionDto decision) {
        return service.reject(id, decision);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
