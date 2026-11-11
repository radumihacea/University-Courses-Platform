package ro.utcn.scd.courseplatform.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ro.utcn.scd.courseplatform.dto.*;
import ro.utcn.scd.courseplatform.service.TimetableGenerationService;
import ro.utcn.scd.courseplatform.service.TimetableService;

import java.util.List;

@RestController
@RequestMapping("/api/timetables")
@RequiredArgsConstructor
public class TimetableController {

    private final TimetableService timetableService;
    private final TimetableGenerationService generationService;

    @GetMapping
    public List<TimetableDto> getAll() {
        return timetableService.findAll();
    }

    @GetMapping("/{id}")
    public TimetableDto getById(@PathVariable Long id) {
        return timetableService.findById(id);
    }

    /** Genereaza automat un orar nou (DRAFT). */
    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.CREATED)
    public GenerationResultDto generate(@Valid @RequestBody GenerateTimetableRequest request) {
        return generationService.generate(request);
    }

    /** Publica orarul: devine vizibil pe mobil, iar toti utilizatorii sunt notificati. */
    @PostMapping("/{id}/publish")
    public TimetableDto publish(@PathVariable Long id) {
        return timetableService.publish(id);
    }

    /** Ex: GET /api/timetables/1/entries?subgroupId=1 (sau professorId=4, sau roomId=14) */
    @GetMapping("/{id}/entries")
    public List<ScheduleEntryDto> entries(@PathVariable Long id,
                                          @RequestParam(required = false) Long subgroupId,
                                          @RequestParam(required = false) Long professorId,
                                          @RequestParam(required = false) Long roomId) {
        return timetableService.entries(id, subgroupId, professorId, roomId);
    }

    @GetMapping("/{id}/conflicts")
    public List<ConflictDto> conflicts(@PathVariable Long id) {
        return timetableService.conflicts(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        timetableService.delete(id);
    }
}
