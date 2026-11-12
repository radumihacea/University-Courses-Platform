package ro.utcn.scd.courseplatform.security;

import ro.utcn.scd.courseplatform.model.enums.Role;

/** Utilizatorul autentificat, extras din token. */
public record AuthUser(Long userId, Role role) {

    /** Numele atributului din cerere in care interceptorul pune utilizatorul autentificat. */
    public static final String REQUEST_ATTRIBUTE = "authUser";
}
