package ro.utcn.scd.courseplatform.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ro.utcn.scd.courseplatform.dto.NotificationDto;
import ro.utcn.scd.courseplatform.service.NotificationService;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService service;

    /** Ex: GET /api/notifications?userId=30&unreadOnly=true */
    @GetMapping
    public List<NotificationDto> getAll(@RequestParam(required = false) Long userId,
                                        @RequestParam(defaultValue = "false") boolean unreadOnly) {
        return service.findAll(userId, unreadOnly);
    }

    @GetMapping("/{id}")
    public NotificationDto getById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NotificationDto create(@Valid @RequestBody NotificationDto dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    public NotificationDto update(@PathVariable Long id, @Valid @RequestBody NotificationDto dto) {
        return service.update(id, dto);
    }

    /** Marcheaza notificarea ca citita (folosit de aplicatia mobila). */
    @PatchMapping("/{id}/read")
    public NotificationDto markAsRead(@PathVariable Long id) {
        return service.markAsRead(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
