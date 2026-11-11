package ro.utcn.scd.courseplatform.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.utcn.scd.courseplatform.dto.BuildingDto;
import ro.utcn.scd.courseplatform.model.Building;
import ro.utcn.scd.courseplatform.repository.BuildingRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class BuildingService {

    private final BuildingRepository repository;
    private final EntityLookup lookup;

    @Transactional(readOnly = true)
    public List<BuildingDto> findAll() {
        return repository.findAll(Sort.by("name")).stream().map(BuildingDto::from).toList();
    }

    @Transactional(readOnly = true)
    public BuildingDto findById(Long id) {
        return BuildingDto.from(lookup.building(id));
    }

    public BuildingDto create(BuildingDto dto) {
        Building b = new Building();
        apply(b, dto);
        return BuildingDto.from(repository.saveAndFlush(b));
    }

    public BuildingDto update(Long id, BuildingDto dto) {
        Building b = lookup.building(id);
        apply(b, dto);
        return BuildingDto.from(repository.saveAndFlush(b));
    }

    public void delete(Long id) {
        repository.delete(lookup.building(id));
        repository.flush();
    }

    private void apply(Building b, BuildingDto dto) {
        b.setName(dto.name());
        b.setAddress(dto.address());
    }
}
