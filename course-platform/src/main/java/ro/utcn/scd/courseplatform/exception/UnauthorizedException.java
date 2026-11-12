package ro.utcn.scd.courseplatform.exception;

/** Credentiale gresite sau utilizator neautentificat. Se transforma in HTTP 401. */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
