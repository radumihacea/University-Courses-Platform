package ro.utcn.scd.courseplatform.dto;

import ro.utcn.scd.courseplatform.model.enums.Role;

import java.time.Instant;

/**
 * Raspunsul la login: tokenul si datele utilizatorului.
 * professorId / studentId sunt completate doar pentru rolul corespunzator
 * (le foloseste aplicatia mobila ca sa ceara orarul personal).
 */
public record AuthResponse(
        String token,
        Instant expiresAt,
        Long userId,
        String fullName,
        String email,
        Role role,
        Long professorId,
        Long studentId
) {
}
