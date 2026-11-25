package ro.utcn.scd.courseplatform.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ro.utcn.scd.courseplatform.ai.AiChatService;
import ro.utcn.scd.courseplatform.dto.ChatRequest;
import ro.utcn.scd.courseplatform.dto.ChatResponse;
import ro.utcn.scd.courseplatform.security.AuthUser;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/** Asistentul AI (chatbot) pentru intrebari despre orar. */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Slf4j
public class AiController {

    private final AiChatService service;
    private final JsonMapper json = JsonMapper.builder().build();

    /**
     * Raspunsul intreg, dintr-o data.
     * Ex (student): POST /api/ai/chat  { "message": "Cand am ore miercuri?" }
     * Ex (admin):   POST /api/ai/chat  { "message": "...", "subgroupId": 1 }
     */
    @PostMapping("/chat")
    public ChatResponse chat(@RequestAttribute(AuthUser.REQUEST_ATTRIBUTE) AuthUser user,
                             @Valid @RequestBody ChatRequest request) {
        return service.chat(user, request);
    }

    /**
     * Raspunsul cu streaming (afisat treptat). Acelasi corp ca /chat, dar raspunsul este NDJSON:
     * cate un obiect JSON pe rand, trimis imediat ce modelul genereaza textul:
     *   {"type":"token","text":"Azi "}
     *   {"type":"token","text":"ai "}
     *   ...
     *   {"type":"done","model":"gemma3:4b","subject":"...","durationMs":20600,"firstTokenMs":9100}
     * Daca apare o eroare dupa ce au plecat primele bucati: {"type":"error","message":"..."}.
     * Erorile de dinainte (ex. Ollama oprit) vin ca raspuns JSON obisnuit, cu status 4xx/5xx.
     */
    @PostMapping("/chat/stream")
    public void chatStream(@RequestAttribute(AuthUser.REQUEST_ATTRIBUTE) AuthUser user,
                           @Valid @RequestBody ChatRequest request,
                           HttpServletResponse response) {
        long start = System.currentTimeMillis();
        NdjsonWriter writer = new NdjsonWriter(response);
        try {
            ChatResponse result = service.chatStream(user, request, token -> {
                if (!writer.started()) {
                    writer.firstTokenMs = System.currentTimeMillis() - start;
                }
                writer.write(Map.of("type", "token", "text", token));
            });
            Map<String, Object> done = new LinkedHashMap<>();
            done.put("type", "done");
            done.put("model", result.model());
            done.put("subject", result.subject());
            done.put("durationMs", result.durationMs());
            done.put("firstTokenMs", writer.firstTokenMs);
            writer.write(done);
        } catch (UncheckedIOException e) {
            // utilizatorul a inchis pagina sau a oprit raspunsul: nu mai avem cui scrie
            log.info("Asistent AI: clientul a inchis conexiunea ({})", e.getMessage());
        } catch (RuntimeException e) {
            if (!writer.started()) {
                throw e; // nimic trimis inca: GlobalExceptionHandler raspunde cu eroarea obisnuita (JSON)
            }
            writer.write(Map.of("type", "error",
                    "message", e.getMessage() == null ? "Eroare neasteptata." : e.getMessage()));
        }
    }

    /** Daca Ollama ruleaza si daca modelul este descarcat. */
    @GetMapping("/status")
    public Map<String, Object> status() {
        return service.status();
    }

    /** Scrie cate un rand JSON si il trimite imediat (flush), fara sa astepte restul raspunsului. */
    private final class NdjsonWriter {
        private final HttpServletResponse response;
        private boolean started;
        private Long firstTokenMs;

        NdjsonWriter(HttpServletResponse response) {
            this.response = response;
        }

        boolean started() {
            return started;
        }

        void write(Map<String, ?> event) {
            try {
                if (!started) {
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.setContentType("application/x-ndjson");
                    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                    response.setHeader("Cache-Control", "no-cache");
                    started = true;
                }
                byte[] line = (json.writeValueAsString(event) + "\n").getBytes(StandardCharsets.UTF_8);
                response.getOutputStream().write(line);
                response.flushBuffer();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
