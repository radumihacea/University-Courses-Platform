package ro.utcn.scd.courseplatform.exception;

/** Aruncata cand datele trimise nu respecta o regula de business. Se transforma in HTTP 400. */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
