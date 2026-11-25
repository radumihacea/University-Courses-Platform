package ro.utcn.scd.courseplatform.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * La pornirea serverului incarca modelul AI in memorie, pe un fir separat
 * (pornirea aplicatiei nu asteapta dupa el). Astfel prima intrebare primeste raspuns mai repede.
 */
@Component
@RequiredArgsConstructor
public class AiWarmUp {

    private final OllamaClient ollama;

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        Thread.ofVirtual().name("ai-warmup").start(ollama::warmUp);
    }
}
