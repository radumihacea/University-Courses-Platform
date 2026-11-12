package ro.utcn.scd.courseplatform.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ro.utcn.scd.courseplatform.dto.AuthResponse;
import ro.utcn.scd.courseplatform.dto.LoginRequest;
import ro.utcn.scd.courseplatform.security.AuthUser;
import ro.utcn.scd.courseplatform.service.AuthService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService service;

    /** Public: primeste email + parola, intoarce tokenul. Folosit de panoul web si de aplicatia mobila. */
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return service.login(request);
    }

    /** Protejat: intoarce datele utilizatorului caruia ii apartine tokenul. */
    @GetMapping("/me")
    public AuthResponse me(@RequestAttribute(AuthUser.REQUEST_ATTRIBUTE) AuthUser user) {
        return service.me(user);
    }
}
