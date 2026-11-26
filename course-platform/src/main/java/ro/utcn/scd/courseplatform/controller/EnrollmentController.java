package ro.utcn.scd.courseplatform.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ro.utcn.scd.courseplatform.dto.OptionalCourseDto;
import ro.utcn.scd.courseplatform.security.AuthUser;
import ro.utcn.scd.courseplatform.service.EnrollmentService;

import java.util.List;

/**
 * Disciplinele optionale ale unui student si inscrierea la ele (folosit de aplicatia mobila).
 *   GET    /api/students/1/optionals       - lista, cu "enrolled" true/false
 *   POST   /api/students/1/optionals/7     - inscriere la activitatea 7
 *   DELETE /api/students/1/optionals/7     - renuntare
 */
@RestController
@RequestMapping("/api/students/{studentId}/optionals")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService service;

    @GetMapping
    public List<OptionalCourseDto> optionals(@PathVariable Long studentId) {
        return service.optionals(studentId);
    }

    @PostMapping("/{assignmentId}")
    public List<OptionalCourseDto> enroll(@RequestAttribute(AuthUser.REQUEST_ATTRIBUTE) AuthUser user,
                                          @PathVariable Long studentId, @PathVariable Long assignmentId) {
        return service.enroll(user, studentId, assignmentId);
    }

    @DeleteMapping("/{assignmentId}")
    public List<OptionalCourseDto> unenroll(@RequestAttribute(AuthUser.REQUEST_ATTRIBUTE) AuthUser user,
                                            @PathVariable Long studentId, @PathVariable Long assignmentId) {
        return service.unenroll(user, studentId, assignmentId);
    }
}
