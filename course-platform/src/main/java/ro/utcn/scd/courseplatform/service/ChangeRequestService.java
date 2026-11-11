package ro.utcn.scd.courseplatform.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.utcn.scd.courseplatform.dto.ChangeRequestDto;
import ro.utcn.scd.courseplatform.exception.BadRequestException;
import ro.utcn.scd.courseplatform.model.ChangeRequest;
import ro.utcn.scd.courseplatform.model.enums.RequestStatus;
import ro.utcn.scd.courseplatform.repository.ChangeRequestRepository;

import java.util.List;

/**
 * CRUD pe cererile de modificare a orarului.
 * O cerere poate fi modificata sau stearsa doar cat timp este PENDING.
 * Aprobarea / respingerea (cu verificarea conflictelor) vine la operatiile complexe.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ChangeRequestService {

    private final ChangeRequestRepository repository;
    private final EntityLookup lookup;

    @Transactional(readOnly = true)
    public List<ChangeRequestDto> findAll(RequestStatus status, Long professorId) {
        return repository.search(status, professorId).stream().map(ChangeRequestDto::from).toList();
    }

    @Transactional(readOnly = true)
    public ChangeRequestDto findById(Long id) {
        return ChangeRequestDto.from(lookup.changeRequest(id));
    }

    public ChangeRequestDto create(ChangeRequestDto dto) {
        ChangeRequest c = new ChangeRequest();
        c.setStatus(RequestStatus.PENDING);
        apply(c, dto);
        return ChangeRequestDto.from(repository.saveAndFlush(c));
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
