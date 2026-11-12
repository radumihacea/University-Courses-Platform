package ro.utcn.scd.courseplatform.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.utcn.scd.courseplatform.dto.ChangeRequestDto;
import ro.utcn.scd.courseplatform.dto.DecisionDto;
import ro.utcn.scd.courseplatform.dto.TimeSlotDto;
import ro.utcn.scd.courseplatform.exception.BadRequestException;
import ro.utcn.scd.courseplatform.model.*;
import ro.utcn.scd.courseplatform.model.enums.RequestStatus;
import ro.utcn.scd.courseplatform.model.enums.Role;
import ro.utcn.scd.courseplatform.model.enums.TimetableStatus;
import ro.utcn.scd.courseplatform.repository.ChangeRequestRepository;
import ro.utcn.scd.courseplatform.repository.StudentRepository;
import ro.utcn.scd.courseplatform.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * CRUD pe cererile de modificare a orarului + aprobarea / respingerea lor.
 * O cerere poate fi modificata sau stearsa doar cat timp este PENDING.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ChangeRequestService {

    private final ChangeRequestRepository repository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final ScheduleConflictService conflictService;
    private final AudienceResolver audienceResolver;
    private final NotificationService notificationService;
    private final EntityLookup lookup;

    @Transactional(readOnly = true)
    public List<ChangeRequestDto> findAll(RequestStatus status, Long professorId) {
        return repository.search(status, professorId).stream().map(ChangeRequestDto::from).toList();
    }

    @Transactional(readOnly = true)
    public ChangeRequestDto findById(Long id) {
        return ChangeRequestDto.from(lookup.changeRequest(id));
    }

    /** Creeaza cererea si anunta live toti adminii ca au o cerere noua de analizat. */
    public ChangeRequestDto create(ChangeRequestDto dto) {
        ChangeRequest c = new ChangeRequest();
        c.setStatus(RequestStatus.PENDING);
        apply(c, dto);
        ChangeRequestDto saved = ChangeRequestDto.from(repository.saveAndFlush(c));

        notificationService.notifyUsers(userRepository.findByRoleIn(List.of(Role.ADMIN)),
                "Cerere noua de la " + saved.professorName() + ": " + saved.disciplineCode() + " "
                        + saved.activityType() + " (" + saved.targetName() + ") din "
                        + saved.currentTimeSlotLabel() + " in " + saved.proposedTimeSlotLabel() + ".");
        return saved;
    }

    public ChangeRequestDto update(Long id, ChangeRequestDto dto) {
        ChangeRequest c = lookup.changeRequest(id);
        ensurePending(c);
        apply(c, dto);
        return ChangeRequestDto.from(repository.saveAndFlush(c));
    }

    public void delete(Long id) {
        ChangeRequest c = lookup.changeRequest(id);
        ensurePending(c);
        repository.delete(c);
        repository.flush();
    }

    /**
     * OPERATIE COMPLEXA: aprobarea unei cereri.
     * Se verifica daca noul slot / noua sala nu creeaza conflicte (profesor, sala, studenti,
     * capacitate, indisponibilitate); daca nu, ora este mutata si cei afectati sunt notificati.
     */
    public ChangeRequestDto approve(Long id, DecisionDto decision) {
        ChangeRequest c = lookup.changeRequest(id);
        ensurePending(c);
        ScheduleEntry entry = c.getScheduleEntry();
        Room newRoom = c.getProposedRoom() != null ? c.getProposedRoom() : entry.getRoom();
        TimeSlot newSlot = c.getProposedTimeSlot();

        List<String> problems = conflictService.checkMove(entry, newSlot, newRoom);
        if (!problems.isEmpty()) {
            throw new BadRequestException("Mutarea nu este posibila: " + String.join(" ", problems));
        }

        String oldPlace = TimeSlotDto.label(entry.getTimeSlot()) + ", sala " + entry.getRoom().getCode();
        entry.setTimeSlot(newSlot);
        entry.setRoom(newRoom);

        c.setStatus(RequestStatus.APPROVED);
        c.setAdminComment(decision != null ? decision.adminComment() : null);
        c.setResolvedAt(LocalDateTime.now());
        repository.saveAndFlush(c);

        notifyAffected(c, oldPlace);
        return ChangeRequestDto.from(c);
    }

    /** Respingerea unei cereri: se salveaza motivul si profesorul este notificat. */
    public ChangeRequestDto reject(Long id, DecisionDto decision) {
        ChangeRequest c = lookup.changeRequest(id);
        ensurePending(c);
        c.setStatus(RequestStatus.REJECTED);
        c.setAdminComment(decision != null ? decision.adminComment() : null);
        c.setResolvedAt(LocalDateTime.now());
        repository.saveAndFlush(c);

        notificationService.notifyUsers(List.of(c.getProfessor().getUser()),
                "Cererea ta de mutare pentru " + c.getScheduleEntry().getAssignment().getDiscipline().getCode()
                        + " a fost respinsa." + (c.getAdminComment() != null ? " Motiv: " + c.getAdminComment() : ""));
        return ChangeRequestDto.from(c);
    }

    // ------------------------------------------------------------------

    /** Notifica profesorul si - daca orarul este publicat - studentii care au acea ora. */
    private void notifyAffected(ChangeRequest c, String oldPlace) {
        ScheduleEntry entry = c.getScheduleEntry();
        String newPlace = TimeSlotDto.label(entry.getTimeSlot()) + ", sala " + entry.getRoom().getCode();
        String message = entry.getAssignment().getDiscipline().getCode() + " ("
                + entry.getAssignment().getType() + ") s-a mutat: " + oldPlace + " -> " + newPlace + ".";

        List<User> users = new ArrayList<>();
        users.add(c.getProfessor().getUser());
        if (entry.getTimetable().getStatus() == TimetableStatus.PUBLISHED) {
            var audience = audienceResolver.create().of(entry.getAssignment());
            studentRepository.findBySubgroupIdIn(audience.subgroupIds())
                    .forEach(s -> users.add(s.getUser()));
        }
        notificationService.notifyUsers(users, message);
    }

    private void ensurePending(ChangeRequest c) {
        if (c.getStatus() != RequestStatus.PENDING) {
            throw new BadRequestException("Cererea a fost deja " +
                    (c.getStatus() == RequestStatus.APPROVED ? "aprobata" : "respinsa") +
                    " si nu mai poate fi modificata.");
        }
    }

    private void apply(ChangeRequest c, ChangeRequestDto dto) {
        var entry = lookup.scheduleEntry(dto.scheduleEntryId());
        var professor = lookup.professor(dto.professorId());
        if (!entry.getAssignment().getProfessor().getId().equals(professor.getId())) {
            throw new BadRequestException("Profesorul poate cere mutarea doar a orelor pe care le preda.");
        }
        c.setScheduleEntry(entry);
        c.setProfessor(professor);
        c.setProposedTimeSlot(lookup.timeSlot(dto.proposedTimeSlotId()));
        c.setProposedRoom(dto.proposedRoomId() != null ? lookup.room(dto.proposedRoomId()) : null);
        c.setReason(dto.reason());
    }
}
