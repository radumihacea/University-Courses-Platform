package ro.utcn.scd.courseplatform.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.utcn.scd.courseplatform.dto.RoomDto;
import ro.utcn.scd.courseplatform.model.Room;
import ro.utcn.scd.courseplatform.model.enums.RoomType;
import ro.utcn.scd.courseplatform.repository.RoomRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class RoomService {

    private final RoomRepository repository;
    private final EntityLookup lookup;

    @Transactional(readOnly = true)
    public List<RoomDto> findAll(RoomType type, Long buildingId) {
        return repository.search(type, buildingId).stream().map(RoomDto::from).toList();
    }

    @Transactional(readOnly = true)
    public RoomDto findById(Long id) {
        return RoomDto.from(lookup.room(id));
    }

    public RoomDto create(RoomDto dto) {
        Room r = new Room();
        apply(r, dto);
        return RoomDto.from(repository.saveAndFlush(r));
    }

    public RoomDto update(Long id, RoomDto dto) {
        Room r = lookup.room(id);
        apply(r, dto);
        return RoomDto.from(repository.saveAndFlush(r));
    }

    public void delete(Long id) {
        repository.delete(lookup.room(id));
        repository.flush();
    }

    private void apply(Room r, RoomDto dto) {
        r.setCode(dto.code().trim());
        r.setBuilding(lookup.building(dto.buildingId()));
        r.setCapacity(dto.capacity());
        r.setType(dto.type());
    }
}
