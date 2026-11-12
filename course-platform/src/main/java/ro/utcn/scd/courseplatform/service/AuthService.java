package ro.utcn.scd.courseplatform.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.utcn.scd.courseplatform.dto.AuthResponse;
import ro.utcn.scd.courseplatform.dto.LoginRequest;
import ro.utcn.scd.courseplatform.exception.UnauthorizedException;
import ro.utcn.scd.courseplatform.model.Professor;
import ro.utcn.scd.courseplatform.model.Student;
import ro.utcn.scd.courseplatform.model.User;
import ro.utcn.scd.courseplatform.repository.ProfessorRepository;
import ro.utcn.scd.courseplatform.repository.StudentRepository;
import ro.utcn.scd.courseplatform.repository.UserRepository;
import ro.utcn.scd.courseplatform.security.AuthUser;
import ro.utcn.scd.courseplatform.security.TokenService;

/** Autentificarea: verifica emailul si parola (BCrypt) si emite un token semnat. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private static final String INVALID = "Email sau parolă greșită.";

    private final UserRepository userRepository;
    private final ProfessorRepository professorRepository;
    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final EntityLookup lookup;

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email().trim().toLowerCase())
                .orElseThrow(() -> new UnauthorizedException(INVALID));
        // Acelasi mesaj si pentru email inexistent, ca sa nu se poata afla ce conturi exista
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException(INVALID);
        }
        TokenService.IssuedToken issued = tokenService.issue(user.getId(), user.getRole());
        return toResponse(user, issued.token(), issued.expiresAt());
    }

    /** Datele utilizatorului autentificat (verificarea tokenului salvat la redeschiderea aplicatiei). */
    public AuthResponse me(AuthUser authUser) {
        return toResponse(lookup.user(authUser.userId()), null, null);
    }

    private AuthResponse toResponse(User user, String token, java.time.Instant expiresAt) {
        Long professorId = professorRepository.findByUserId(user.getId()).map(Professor::getId).orElse(null);
        Long studentId = studentRepository.findByUserId(user.getId()).map(Student::getId).orElse(null);
        return new AuthResponse(token, expiresAt, user.getId(), user.getFullName(), user.getEmail(),
                user.getRole(), professorId, studentId);
    }
}
