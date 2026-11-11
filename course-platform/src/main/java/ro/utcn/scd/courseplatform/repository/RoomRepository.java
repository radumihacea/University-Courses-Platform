package ro.utcn.scd.courseplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.utcn.scd.courseplatform.model.Room;

public interface RoomRepository extends JpaRepository<Room, Long> {
}
