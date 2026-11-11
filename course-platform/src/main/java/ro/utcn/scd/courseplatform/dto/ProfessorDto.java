package ro.utcn.scd.courseplatform.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import ro.utcn.scd.courseplatform.model.Professor;

/**
 * Un profesor impreuna cu contul lui de utilizator.
 * password: obligatorie la creare, optionala la modificare (daca lipseste, ramane cea veche);
 * nu este trimisa niciodata inapoi in raspunsuri (WRITE_ONLY).
 */
public record ProfessorDto(
        Long id,
        Long userId,
        @NotBlank @Size(max = 100) String fullName,
        @NotBlank @Email @Size(max = 120) String email,
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) @Size(min = 6, max = 100) String password,
        @Size(max = 50) String title,
        @Size(max = 100) String department
) {
    public static ProfessorDto from(Professor p) {
        return new ProfessorDto(p.getId(), p.getUser().getId(), p.getUser().getFullName(),
                p.getUser().getEmail(), null, p.getTitle(), p.getDepartment());
    }
}
