package ro.utcn.scd.courseplatform.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Protejeaza toate endpoint-urile /api/** : cererea trebuie sa aiba antetul
 *     Authorization: Bearer <token>
 * cu un token valid primit la login. Altfel raspunsul este 401 Unauthorized.
 * Exceptii: /api/auth/login si cererile OPTIONS (verificarea CORS a browserului).
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private static final String BEARER = "Bearer ";

    private final TokenService tokenService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String header = request.getHeader("Authorization");
        String token = header != null && header.startsWith(BEARER) ? header.substring(BEARER.length()).trim() : null;

        Optional<AuthUser> user = tokenService.verify(token);
        if (user.isEmpty()) {
            writeUnauthorized(response, token == null
                    ? "Autentificare necesara: trimite antetul Authorization: Bearer <token>."
                    : "Tokenul este invalid sau a expirat. Autentifica-te din nou.");
            return false;
        }
        request.setAttribute(AuthUser.REQUEST_ATTRIBUTE, user.get());
        return true;
    }

    /** Acelasi format ca restul erorilor (ApiError). */
    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"timestamp\":\"" + LocalDateTime.now() + "\",\"status\":401,"
                + "\"error\":\"Unauthorized\",\"message\":\"" + message + "\",\"details\":[]}");
    }
}
