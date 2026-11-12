package ro.utcn.scd.courseplatform.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ro.utcn.scd.courseplatform.model.enums.Role;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

/**
 * Token de autentificare semnat cu HMAC-SHA256 (acelasi principiu ca un JWT, dar minimal).
 *
 * Format:  base64url("userId:ROL:expirare") + "." + base64url(semnatura)
 *
 * Serverul nu tine sesiuni in memorie: orice cerere care vine cu un token valid
 * (semnatura corecta si neexpirat) este acceptata. Tokenul nu poate fi falsificat
 * fara cheia secreta, care exista doar pe server.
 */
@Service
public class TokenService {

    private static final String ALGORITHM = "HmacSHA256";
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    private final byte[] secret;
    private final long validityHours;

    public TokenService(
            @Value("${app.auth.secret:schimba-aceasta-cheie-secreta-in-productie-ucp-2026}") String secret,
            @Value("${app.auth.token-hours:12}") long validityHours) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.validityHours = validityHours;
    }

    public record IssuedToken(String token, Instant expiresAt) {
    }

    public IssuedToken issue(Long userId, Role role) {
        Instant expiresAt = Instant.now().plusSeconds(validityHours * 3600);
        String payload = userId + ":" + role.name() + ":" + expiresAt.getEpochSecond();
        String encodedPayload = ENCODER.encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        return new IssuedToken(encodedPayload + "." + sign(encodedPayload), expiresAt);
    }

    /** Verifica semnatura si expirarea; intoarce utilizatorul daca tokenul e valid. */
    public Optional<AuthUser> verify(String token) {
        if (token == null) {
            return Optional.empty();
        }
        String[] parts = token.split("\\.");
        if (parts.length != 2) {
            return Optional.empty();
        }
        byte[] expected = sign(parts[0]).getBytes(StandardCharsets.UTF_8);
        byte[] actual = parts[1].getBytes(StandardCharsets.UTF_8);
        // comparatie in timp constant, ca sa nu se poata ghici semnatura caracter cu caracter
        if (!MessageDigest.isEqual(expected, actual)) {
            return Optional.empty();
        }
        try {
            String[] fields = new String(DECODER.decode(parts[0]), StandardCharsets.UTF_8).split(":");
            if (fields.length != 3 || Instant.now().getEpochSecond() > Long.parseLong(fields[2])) {
                return Optional.empty();
            }
            return Optional.of(new AuthUser(Long.parseLong(fields[0]), Role.valueOf(fields[1])));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private String sign(String data) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret, ALGORITHM));
            return ENCODER.encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Nu s-a putut semna tokenul", e);
        }
    }
}
