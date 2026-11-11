package ro.utcn.scd.courseplatform.exception;

/** Aruncata cand o resursa ceruta nu exista. Se transforma in raspuns HTTP 404. */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String resource, Long id) {
        super(resource + " cu id " + id + " nu exista.");
    }
}
