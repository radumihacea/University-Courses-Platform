package ro.utcn.scd.courseplatform.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import ro.utcn.scd.courseplatform.exception.ServiceUnavailableException;
import tools.jackson.databind.json.JsonMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Clientul pentru Ollama - serverul local care ruleaza modelul de limbaj (LLM).
 * Ollama asculta implicit pe http://localhost:11434 si expune endpoint-ul POST /api/chat
 * (cu raspuns intreg sau cu streaming - bucata cu bucata).
 *
 * Modelul si adresa se pot schimba din application.properties:
 *   app.ai.ollama-url=http://localhost:11434
 *   app.ai.model=gemma3:4b
 */
@Component
@Slf4j
public class OllamaClient {

    /** Modelul ramane in memorie 30 de minute dupa ultima intrebare (implicit Ollama il scoate dupa 5). */
    private static final String KEEP_ALIVE = "30m";

    /** temperatura mica = raspunsuri mai precise; num_ctx 4096 ajunge pentru un orar si e mai rapid. */
    private static final Map<String, Object> OPTIONS = Map.of("temperature", 0.2, "num_ctx", 4096);

    private final RestClient restClient;
    private final HttpClient httpClient;
    private final JsonMapper json = JsonMapper.builder().build();
    private final String model;
    private final String baseUrl;

    public OllamaClient(@Value("${app.ai.ollama-url:http://localhost:11434}") String baseUrl,
                        @Value("${app.ai.model:gemma3:4b}") String model) {
        this.baseUrl = baseUrl;
        this.model = model;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        // Pe procesor (fara placa video) un raspuns poate dura zeci de secunde
        requestFactory.setReadTimeout(Duration.ofSeconds(180));
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
    }

    /** Un mesaj din conversatie: role = "system", "user" sau "assistant". */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Message(String role, String content) {
    }

    /** keep_alive = cat timp ramane modelul incarcat in memorie dupa ultima cerere. */
    record ChatRequest(String model, List<Message> messages, boolean stream, String keep_alive,
                       Map<String, Object> options) {
    }

    record GenerateRequest(String model, String prompt, boolean stream, String keep_alive) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ChatResponse(Message message, Boolean done) {
    }

    /** Un rand din raspunsul cu streaming (ultimul are done = true). */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record StreamChunk(Message message, Boolean done, String error) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TagsResponse(List<ModelInfo> models) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ModelInfo(String name) {
    }

    public String model() {
        return model;
    }

    /** Trimite conversatia la model si intoarce raspunsul lui (fara streaming). */
    public String chat(List<Message> messages) {
        try {
            ChatResponse response = restClient.post()
                    .uri("/api/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new ChatRequest(model, messages, false, KEEP_ALIVE, OPTIONS))
                    .retrieve()
                    .body(ChatResponse.class);
            if (response == null || response.message() == null) {
                throw new ServiceUnavailableException("Modelul AI nu a trimis niciun raspuns.");
            }
            return response.message().content().trim();
        } catch (ResourceAccessException e) {
            log.warn("Ollama nu raspunde la {}: {}", baseUrl, e.getMessage());
            throw notRunning();
        } catch (RestClientResponseException e) {
            throw ollamaError(e.getStatusCode().value(), e.getResponseBodyAsString());
        }
    }

    /**
     * Varianta cu streaming: Ollama trimite raspunsul bucata cu bucata (cate un rand JSON pe bucata),
     * iar fiecare bucata este data imediat mai departe prin onToken. Intoarce raspunsul complet.
     *
     * Daca cel care primeste bucatile renunta (ex. utilizatorul a inchis pagina), onToken arunca o
     * exceptie, conexiunea cu Ollama se inchide si modelul se opreste din generat.
     */
    public String chatStream(List<Message> messages, Consumer<String> onToken) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/api/chat"))
                .timeout(Duration.ofSeconds(180))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        json.writeValueAsString(new ChatRequest(model, messages, true, KEEP_ALIVE, OPTIONS)),
                        StandardCharsets.UTF_8))
                .build();

        HttpResponse<InputStream> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (IOException e) {
            log.warn("Ollama nu raspunde la {}: {}", baseUrl, e.getMessage());
            throw notRunning();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceUnavailableException("Cererea catre modelul AI a fost intrerupta.");
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
            if (response.statusCode() >= 400) {
                StringBuilder body = new StringBuilder();
                reader.lines().forEach(body::append);
                throw ollamaError(response.statusCode(), body.toString());
            }
            StringBuilder answer = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                StreamChunk chunk = json.readValue(line, StreamChunk.class);
                if (chunk.error() != null) {
                    throw new ServiceUnavailableException("Eroare de la Ollama: " + chunk.error());
                }
                if (chunk.message() != null && chunk.message().content() != null && !chunk.message().content().isEmpty()) {
                    answer.append(chunk.message().content());
                    onToken.accept(chunk.message().content());
                }
                if (Boolean.TRUE.equals(chunk.done())) {
                    break;
                }
            }
            return answer.toString().trim();
        } catch (IOException e) {
            throw new ServiceUnavailableException("Conexiunea cu modelul AI s-a intrerupt: " + e.getMessage());
        }
    }

    private ServiceUnavailableException notRunning() {
        return new ServiceUnavailableException("Asistentul AI nu este disponibil: Ollama nu ruleaza pe " + baseUrl
                + ". Porneste aplicatia Ollama si incearca din nou.");
    }

    private ServiceUnavailableException ollamaError(int status, String body) {
        log.warn("Ollama a raspuns cu eroare {}: {}", status, body);
        if (status == 404) {
            return new ServiceUnavailableException("Modelul \"" + model + "\" nu este descarcat. Ruleaza: ollama pull " + model);
        }
        return new ServiceUnavailableException("Eroare de la Ollama: " + body);
    }

    /**
     * Incarca modelul in memorie fara sa genereze nimic (prompt gol), ca prima intrebare
     * reala sa nu mai astepte incarcarea. Erorile sunt ignorate (Ollama poate fi oprit).
     */
    public void warmUp() {
        try {
            restClient.post()
                    .uri("/api/generate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new GenerateRequest(model, "", false, KEEP_ALIVE))
                    .retrieve()
                    .toBodilessEntity();
            log.info("Modelul AI {} a fost incarcat in memorie.", model);
        } catch (RuntimeException e) {
            log.info("Modelul AI nu a putut fi preincarcat (Ollama nu ruleaza?): {}", e.getMessage());
        }
    }

    /** Verifica daca Ollama ruleaza si daca modelul configurat este descarcat. */
    public Map<String, Object> status() {
        try {
            TagsResponse tags = restClient.get().uri("/api/tags").retrieve().body(TagsResponse.class);
            List<String> installed = tags == null || tags.models() == null ? List.of()
                    : tags.models().stream().map(ModelInfo::name).toList();
            boolean modelReady = installed.stream().anyMatch(n -> n.equals(model) || n.equals(model + ":latest"));
            return Map.of("ollamaRunning", true, "model", model, "modelReady", modelReady, "installedModels", installed);
        } catch (RuntimeException e) {
            return Map.of("ollamaRunning", false, "model", model, "modelReady", false, "installedModels", List.of());
        }
    }
}
