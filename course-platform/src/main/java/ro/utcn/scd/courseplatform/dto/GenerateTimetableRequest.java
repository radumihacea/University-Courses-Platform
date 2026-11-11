package ro.utcn.scd.courseplatform.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Cererea de generare a unui orar nou.
 * attempts: de cate ori ruleaza algoritmul (implicit 30); seed: pentru rezultate reproductibile.
 */
public record GenerateTimetableRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Pattern(regexp = "\\d{4}-\\d{4}", message = "trebuie sa fie de forma 2026-2027") String academicYear,
        @NotNull @Min(1) @Max(2) Integer semester,
        @Min(1) @Max(200) Integer attempts,
        Long seed
) {
}
