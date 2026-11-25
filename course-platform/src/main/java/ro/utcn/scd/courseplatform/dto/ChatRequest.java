package ro.utcn.scd.courseplatform.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Intrebarea trimisa asistentului AI.
 * history: mesajele anterioare din conversatie (optional), ca asistentul sa inteleaga
 *          intrebari de continuare de tipul "si joi?".
 * subgroupId / professorId: doar pentru admin - al cui orar sa fie folosit.
 *          Studentii si profesorii primesc automat propriul orar.
 */
public record ChatRequest(
        @NotBlank @Size(max = 1000) String message,
        @Valid @Size(max = 20) List<ChatMessageDto> history,
        Long subgroupId,
        Long professorId
) {
    /** role = "user" (intrebarea) sau "assistant" (raspunsul asistentului). */
    public record ChatMessageDto(@NotBlank String role, @NotBlank @Size(max = 4000) String content) {
    }
}
