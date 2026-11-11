package ro.utcn.scd.courseplatform.dto;

import jakarta.validation.constraints.Size;

/** Comentariul adminului la aprobarea / respingerea unei cereri de modificare. */
public record DecisionDto(
        @Size(max = 500) String adminComment
) {
}
