package ro.utcn.scd.courseplatform.exception;

import java.time.LocalDateTime;
import java.util.List;

/** Forma unica a raspunsurilor de eroare trimise de API catre frontend si mobil. */
public record ApiError(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        List<String> details
) {
    public static ApiError of(int status, String error, String message, List<String> details) {
        return new ApiError(LocalDateTime.now(), status, error, message, details);
    }
}
