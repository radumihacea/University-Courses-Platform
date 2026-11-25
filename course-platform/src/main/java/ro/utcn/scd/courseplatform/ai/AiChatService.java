package ro.utcn.scd.courseplatform.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import ro.utcn.scd.courseplatform.dto.ChatRequest;
import ro.utcn.scd.courseplatform.dto.ChatResponse;
import ro.utcn.scd.courseplatform.dto.ScheduleEntryDto;
import ro.utcn.scd.courseplatform.exception.BadRequestException;
import ro.utcn.scd.courseplatform.exception.NotFoundException;
import ro.utcn.scd.courseplatform.model.Professor;
import ro.utcn.scd.courseplatform.model.Student;
import ro.utcn.scd.courseplatform.model.Subgroup;
import ro.utcn.scd.courseplatform.model.enums.TimetableStatus;
import ro.utcn.scd.courseplatform.repository.ProfessorRepository;
import ro.utcn.scd.courseplatform.repository.StudentRepository;
import ro.utcn.scd.courseplatform.repository.TimetableRepository;
import ro.utcn.scd.courseplatform.security.AuthUser;
import ro.utcn.scd.courseplatform.service.EntityLookup;
import ro.utcn.scd.courseplatform.service.PersonalScheduleService;
import ro.utcn.scd.courseplatform.service.TimetableService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Asistentul AI pentru orar (chatbot).
 *
 * Pasii:
 *   1. afla al cui orar trebuie folosit (studentul / profesorul autentificat, sau ce a ales adminul);
 *   2. construieste un "context" text cu orarul publicat si calendarul (ScheduleContextBuilder);
 *   3. trimite la model: instructiunile + contextul + conversatia + intrebarea;
 *   4. intoarce raspunsul modelului - intreg (chat) sau bucata cu bucata (chatStream).
 *
 * Modelul primeste DOAR datele persoanei respective si instructiunea de a nu inventa nimic,
 * deci raspunsurile se bazeaza pe orarul real din baza de date.
 */
@Service
@Slf4j
public class AiChatService {

    private static final int MAX_HISTORY = 6;

    private static final String INSTRUCTIONS = """
            Esti asistentul pentru orar al platformei University Courses Platform (UTCN).
            Reguli:
            - Raspunde in romana, scurt (1-3 propozitii sau o lista scurta).
            - Foloseste DOAR datele de mai jos. Nu inventa ore, sali sau profesori. Daca nu gasesti informatia, spune asta.
            - Pentru azi/maine foloseste sectiunile AZI si MAINE (sunt deja corecte).
            - [S.I.] = doar in saptamanile impare, [S.P.] = doar in saptamanile pare; mentioneaza asta cand e cazul.
            - Daca intrebarea nu e despre orar, spune ca poti ajuta doar cu orarul.

            DATE:
            """;

    private final OllamaClient ollama;
    private final ScheduleContextBuilder contextBuilder;
    private final PersonalScheduleService personalScheduleService;
    private final TimetableService timetableService;
    private final TimetableRepository timetableRepository;
    private final StudentRepository studentRepository;
    private final ProfessorRepository professorRepository;
    private final EntityLookup lookup;
    private final TransactionTemplate readOnlyTx;

    public AiChatService(OllamaClient ollama, ScheduleContextBuilder contextBuilder,
                         PersonalScheduleService personalScheduleService, TimetableService timetableService,
                         TimetableRepository timetableRepository, StudentRepository studentRepository,
                         ProfessorRepository professorRepository, EntityLookup lookup,
                         PlatformTransactionManager transactionManager) {
        this.ollama = ollama;
        this.contextBuilder = contextBuilder;
        this.personalScheduleService = personalScheduleService;
        this.timetableService = timetableService;
        this.timetableRepository = timetableRepository;
        this.studentRepository = studentRepository;
        this.professorRepository = professorRepository;
        this.lookup = lookup;
        this.readOnlyTx = new TransactionTemplate(transactionManager);
        this.readOnlyTx.setReadOnly(true);
    }

    /** showAudience = true pentru profesori (contextul include cui predau). */
    private record Subject(String description, List<ScheduleEntryDto> week, boolean showAudience) {
    }

    private record Prepared(String subject, String context) {
    }

    /** Raspunsul intreg, dintr-o data (folosit de aplicatia mobila si de Postman). */
    public ChatResponse chat(AuthUser user, ChatRequest request) {
        return answer(user, request, null);
    }

    /**
     * Raspunsul cu streaming: fiecare bucata de text generata de model este trimisa imediat prin onToken,
     * ca utilizatorul sa vada raspunsul aparand treptat. La final intoarce raspunsul complet.
     */
    public ChatResponse chatStream(AuthUser user, ChatRequest request, Consumer<String> onToken) {
        return answer(user, request, onToken);
    }

    private ChatResponse answer(AuthUser user, ChatRequest request, Consumer<String> onToken) {
        long start = System.currentTimeMillis();
        // Datele se citesc intr-o tranzactie scurta; apelul catre model (care poate dura)
        // se face dupa ce tranzactia s-a inchis, ca sa nu tina ocupata o conexiune la baza de date.
        Prepared prepared = readOnlyTx.execute(status -> {
            Subject subject = resolveSubject(user, request);
            return new Prepared(subject.description(),
                    contextBuilder.build(subject.description(), subject.week(), LocalDate.now(), subject.showAudience()));
        });

        // Mesajul "system" este identic intre intrebarile aceleiasi persoane din aceeasi zi, asa ca
        // Ollama il refoloseste din memorie (cache) si intrebarile urmatoare incep sa raspunda mai repede.
        List<OllamaClient.Message> messages = new ArrayList<>();
        messages.add(new OllamaClient.Message("system", INSTRUCTIONS + prepared.context()));
        if (request.history() != null) {
            request.history().stream()
                    .skip(Math.max(0, request.history().size() - MAX_HISTORY))
                    .filter(m -> m.role().equals("user") || m.role().equals("assistant"))
                    .forEach(m -> messages.add(new OllamaClient.Message(m.role(), m.content())));
        }
        messages.add(new OllamaClient.Message("user", request.message()));

        String answer = onToken == null ? ollama.chat(messages) : ollama.chatStream(messages, onToken);
        long duration = System.currentTimeMillis() - start;
        log.info("Asistent AI ({}): \"{}\" -> raspuns in {} ms{}", prepared.subject(), request.message(), duration,
                onToken == null ? "" : " (streaming)");
        return new ChatResponse(answer, ollama.model(), prepared.subject(), duration);
    }

    /** Al cui orar se foloseste: studentul / profesorul autentificat sau ce a ales adminul. */
    private Subject resolveSubject(AuthUser user, ChatRequest request) {
        return switch (user.role()) {
            case STUDENT -> {
                Student s = studentRepository.findByUserId(user.userId())
                        .orElseThrow(() -> new NotFoundException("Studentul pentru utilizatorul", user.userId()));
                yield new Subject(s.getUser().getFullName() + ", student in semigrupa " + s.getSubgroup().getName(),
                        personalScheduleService.studentWeek(s.getId()), false);
            }
            case PROFESSOR -> {
                Professor p = professorRepository.findByUserId(user.userId())
                        .orElseThrow(() -> new NotFoundException("Profesorul pentru utilizatorul", user.userId()));
                yield new Subject(p.getUser().getFullName() + ", cadru didactic",
                        personalScheduleService.professorWeek(p.getId()), true);
            }
            case ADMIN -> {
                if (request.professorId() != null) {
                    Professor p = lookup.professor(request.professorId());
                    yield new Subject(p.getUser().getFullName() + ", cadru didactic",
                            personalScheduleService.professorWeek(p.getId()), true);
                }
                if (request.subgroupId() != null) {
                    Subgroup sg = lookup.subgroup(request.subgroupId());
                    List<ScheduleEntryDto> week = timetableRepository
                            .findFirstByStatusOrderByPublishedAtDesc(TimetableStatus.PUBLISHED)
                            .map(t -> timetableService.entries(t.getId(), sg.getId(), null, null))
                            .orElse(List.of());
                    yield new Subject("un student din semigrupa " + sg.getName(), week, false);
                }
                throw new BadRequestException("Ca admin, alege o semigrupa (subgroupId) sau un profesor (professorId).");
            }
        };
    }

    public java.util.Map<String, Object> status() {
        return ollama.status();
    }
}
