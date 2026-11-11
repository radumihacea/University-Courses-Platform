package ro.utcn.scd.courseplatform.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.utcn.scd.courseplatform.dto.DisciplineDto;
import ro.utcn.scd.courseplatform.model.Discipline;
import ro.utcn.scd.courseplatform.repository.DisciplineRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class DisciplineService {

    private final DisciplineRepository repository;
    private final EntityLookup lookup;

    @Transactional(readOnly = true)
    public List<DisciplineDto> findAll(Integer studyYear, Boolean optional) {
        return repository.search(studyYear, optional).stream().map(DisciplineDto::from).toList();
    }

    @Transactional(readOnly = true)
    public DisciplineDto findById(Long id) {
        return DisciplineDto.from(lookup.discipline(id));
    }

    public DisciplineDto create(DisciplineDto dto) {
        Discipline d = new Discipline();
        apply(d, dto);
        return DisciplineDto.from(repository.saveAndFlush(d));
    }

    public DisciplineDto update(Long id, DisciplineDto dto) {
        Discipline d = lookup.discipline(id);
        apply(d, dto);
        return DisciplineDto.from(repository.saveAndFlush(d));
    }

    public void delete(Long id) {
        repository.delete(lookup.discipline(id));
        repository.flush();
    }

    private void apply(Discipline d, DisciplineDto dto) {
        d.setCode(dto.code().trim().toUpperCase());
        d.setName(dto.name());
        d.setCredits(dto.credits());
        d.setStudyYear(dto.studyYear());
        d.setOptional(dto.optional());
    }
}
