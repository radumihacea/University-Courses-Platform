package ro.utcn.scd.courseplatform.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.utcn.scd.courseplatform.dto.ProfessorConstraintDto;
import ro.utcn.scd.courseplatform.model.ProfessorConstraint;
import ro.utcn.scd.courseplatform.repository.ProfessorConstraintRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ProfessorConstraintService {

    private final ProfessorConstraintRepository repository;
    private final EntityLookup lookup;

    @Transactional(readOnly = true)
    public List<ProfessorConstraintDto> findAll(Long professorId) {
        List<ProfessorConstraint> list = professorId != null
                ? repository.findByProfessorId(professorId)
                : repository.findAll();
        return list.stream().map(ProfessorConstraintDto::from).toList();
    }

    @Transactional(readOnly = true)
    public ProfessorConstraintDto findById(Long id) {
        return ProfessorConstraintDto.from(lookup.constraint(id));
    }

    public ProfessorConstraintDto create(ProfessorConstraintDto dto) {
        ProfessorConstraint c = new ProfessorConstraint();
        apply(c, dto);
        return ProfessorConstraintDto.from(repository.saveAndFlush(c));
    }

    public ProfessorConstraintDto update(Long id, ProfessorConstraintDto dto) {
        ProfessorConstraint c = lookup.constraint(id);
        apply(c, dto);
        return ProfessorConstraintDto.from(repository.saveAndFlush(c));
    }

    public void delete(Long id) {
        repository.delete(lookup.constraint(id));
        repository.flush();
    }

    private void apply(ProfessorConstraint c, ProfessorConstraintDto dto) {
        c.setProfessor(lookup.professor(dto.professorId()));
        c.setTimeSlot(lookup.timeSlot(dto.timeSlotId()));
        c.setType(dto.type());
        c.setMandatory(Boolean.TRUE.equals(dto.mandatory()));
        c.setOriginalText(dto.originalText());
    }
}
