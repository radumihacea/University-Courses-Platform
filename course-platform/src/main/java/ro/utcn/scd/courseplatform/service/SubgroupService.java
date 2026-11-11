package ro.utcn.scd.courseplatform.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.utcn.scd.courseplatform.dto.SubgroupDto;
import ro.utcn.scd.courseplatform.model.Subgroup;
import ro.utcn.scd.courseplatform.repository.SubgroupRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class SubgroupService {

    private final SubgroupRepository repository;
    private final EntityLookup lookup;

    @Transactional(readOnly = true)
    public List<SubgroupDto> findAll(Long groupId) {
        List<Subgroup> subgroups = groupId != null
                ? repository.findByGroupIdOrderByName(groupId)
                : repository.findAll(Sort.by("name"));
        return subgroups.stream().map(SubgroupDto::from).toList();
    }

    @Transactional(readOnly = true)
    public SubgroupDto findById(Long id) {
        return SubgroupDto.from(lookup.subgroup(id));
    }

    public SubgroupDto create(SubgroupDto dto) {
        Subgroup s = new Subgroup();
        apply(s, dto);
        return SubgroupDto.from(repository.saveAndFlush(s));
    }

    public SubgroupDto update(Long id, SubgroupDto dto) {
        Subgroup s = lookup.subgroup(id);
        apply(s, dto);
        return SubgroupDto.from(repository.saveAndFlush(s));
    }

    public void delete(Long id) {
        repository.delete(lookup.subgroup(id));
        repository.flush();
    }

    private void apply(Subgroup s, SubgroupDto dto) {
        s.setName(dto.name().trim());
        s.setGroup(lookup.group(dto.groupId()));
        s.setStudentCount(dto.studentCount());
    }
}
