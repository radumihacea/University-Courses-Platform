package ro.utcn.scd.courseplatform.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.utcn.scd.courseplatform.dto.StudentGroupDto;
import ro.utcn.scd.courseplatform.model.StudentGroup;
import ro.utcn.scd.courseplatform.repository.StudentGroupRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class StudentGroupService {

    private final StudentGroupRepository repository;
    private final EntityLookup lookup;

    @Transactional(readOnly = true)
    public List<StudentGroupDto> findAll(Long seriesId) {
        List<StudentGroup> groups = seriesId != null
                ? repository.findBySeriesIdOrderByCode(seriesId)
                : repository.findAll(Sort.by("code"));
        return groups.stream().map(StudentGroupDto::from).toList();
    }

    @Transactional(readOnly = true)
    public StudentGroupDto findById(Long id) {
        return StudentGroupDto.from(lookup.group(id));
    }

    public StudentGroupDto create(StudentGroupDto dto) {
        StudentGroup g = new StudentGroup();
        apply(g, dto);
        return StudentGroupDto.from(repository.saveAndFlush(g));
    }

    public StudentGroupDto update(Long id, StudentGroupDto dto) {
        StudentGroup g = lookup.group(id);
        apply(g, dto);
        return StudentGroupDto.from(repository.saveAndFlush(g));
    }

    public void delete(Long id) {
        repository.delete(lookup.group(id));
        repository.flush();
    }

    private void apply(StudentGroup g, StudentGroupDto dto) {
        g.setCode(dto.code().trim());
        g.setSeries(lookup.series(dto.seriesId()));
    }
}
