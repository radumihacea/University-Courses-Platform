package ro.utcn.scd.courseplatform.exception;

/** Un serviciu extern (ex: Ollama) nu este disponibil. Se transforma in HTTP 503. */
public class ServiceUnavailableException extends RuntimeException {

    public ServiceUnavailableException(String message) {
        super(message);
    }
}
