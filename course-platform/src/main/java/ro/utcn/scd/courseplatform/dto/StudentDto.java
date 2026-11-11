package ro.utcn.scd.courseplatform.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ro.utcn.scd.courseplatform.model.Student;

/** Un student impreuna cu contul lui de utilizator. Parola: la fel ca la ProfessorDto. */
public record StudentDto(
        Long id,
        Long userId,
        @NotBlank @Size(max = 100) String fullName,
        @NotBlank @Email @Size(max = 120) String email,
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) @Size(min = 6, max = 100) String password,
        @NotBlank @Size(max = 20) String registrationNumber,
        @NotNull Long subgroupId,
        String subgroupName
) {
    public static StudentDto from(Student s) {
        return new StudentDto(s.getId(), s.getUser().getId(), s.getUser().getFullName(),
                s.getUser().getEmail(), null, s.getRegistrationNumber(),
                s.getSubgroup().getId(), s.getSubgroup().getName());
    }
}
