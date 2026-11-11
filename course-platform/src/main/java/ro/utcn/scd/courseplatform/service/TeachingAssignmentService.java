package ro.utcn.scd.courseplatform.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.utcn.scd.courseplatform.dto.TeachingAssignmentDto;
import ro.utcn.scd.courseplatform.exception.BadRequestException;
import ro.utcn.scd.courseplatform.model.TeachingAssignment;
import ro.utcn.scd.courseplatform.model.enums.ActivityType;
import ro.utcn.scd.courseplatform.repository.TeachingAssignmentRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TeachingAssignmentService {

    private final TeachingAssignmentRepository repository;
    private final EntityLookup lookup;

    @Transactional(readOnly = true)
    public List<TeachingAssignmentDto> findAll(Long professorId, Long disciplineId, Long subgroupId,
                                               ActivityType type) {
        return repository.search(professorId, disciplineId, subgroupId, type).stream()
                .map(TeachingAssignmentDto::from).toList();
    }

    @Transactional(readOnly = true)
    public TeachingAssignmentDto findById(Long id) {
        return TeachingAssignmentDto.from(lookup.assignment(id));
    }

    public TeachingAssignmentDto create(TeachingAssignmentDto dto) {
        TeachingAssignment a = new TeachingAssignment();
        apply(a, dto);
        return TeachingAssignmentDto.from(repository.saveAndFlush(a));
    }

    public TeachingAssignmentDto update(Long id, TeachingAssignmentDto dto) {
        TeachingAssignment a = lookup.assignment(id);
        apply(a, dto);
        return TeachingAssignmentDto.from(repository.saveAndFlush(a));
    }

    public void delete(Long id) {
        repository.delete(lookup.assignment(id));
        repository.flush();
    }

    private void apply(TeachingAssignment a, TeachingAssignmentDto dto) {
        validateTarget(dto);
        a.setDiscipline(lookup.discipline(dto.disciplineId()));
        a.setProfessor(lookup.professor(dto.professorId()));
        a.setType(dto.type());
        a.setHoursPerSession(dto.hoursPerSession());
        a.setFrequency(dto.frequency());
        a.setPreferredRoom(dto.preferredRoomId() != null ? lookup.room(dto.preferredRoomId()) : null);
        a.setSeries(dto.seriesId() != null ? lookup.series(dto.seriesId()) : null);
        a.setGroup(dto.groupId() != null ? lookup.group(dto.groupId()) : null);
        a.setSubgroup(dto.subgroupId() != null ? lookup.subgroup(dto.subgroupId()) : null);
    }

    /** Cursul -> serie, seminarul -> grupa, laboratorul/proiectul -> semigrupa (exact unul). */
    private void validateTarget(TeachingAssignmentDto dto) {
        boolean ok = switch (dto.type()) {
            case COURSE -> dto.seriesId() != null && dto.groupId() == null && dto.subgroupId() == null;
            case SEMINAR -> dto.groupId() != null && dto.seriesId() == null && dto.subgroupId() == null;
            case LAB, PROJECT -> dto.subgroupId() != null && dto.seriesId() == null && dto.groupId() == null;
        };
        if (!ok) {
            throw new BadRequestException(
                    "Publicul nu corespunde tipului: cursul necesita seriesId, seminarul groupId, "
                            + "iar laboratorul/proiectul subgroupId (si doar pe acela).");
        }
    }
}
