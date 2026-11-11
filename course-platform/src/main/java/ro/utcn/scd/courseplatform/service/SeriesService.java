package ro.utcn.scd.courseplatform.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.utcn.scd.courseplatform.dto.SeriesDto;
import ro.utcn.scd.courseplatform.model.Series;
import ro.utcn.scd.courseplatform.repository.SeriesRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class SeriesService {

    private final SeriesRepository repository;
    private final EntityLookup lookup;

    @Transactional(readOnly = true)
    public List<SeriesDto> findAll() {
        return repository.findAll(Sort.by("studyYear", "name")).stream().map(SeriesDto::from).toList();
    }

    @Transactional(readOnly = true)
    public SeriesDto findById(Long id) {
        return SeriesDto.from(lookup.series(id));
    }

    public SeriesDto create(SeriesDto dto) {
        Series s = new Series();
        apply(s, dto);
        return SeriesDto.from(repository.saveAndFlush(s));
    }

    public SeriesDto update(Long id, SeriesDto dto) {
        Series s = lookup.series(id);
        apply(s, dto);
        return SeriesDto.from(repository.saveAndFlush(s));
    }

    public void delete(Long id) {
        repository.delete(lookup.series(id));
        repository.flush();
    }

    private void apply(Series s, SeriesDto dto) {
        s.setName(dto.name().trim());
        s.setStudyYear(dto.studyYear());
        s.setSpecialization(dto.specialization());
    }
}
