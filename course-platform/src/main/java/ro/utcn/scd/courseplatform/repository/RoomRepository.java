package ro.utcn.scd.courseplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ro.utcn.scd.courseplatform.model.Room;
import ro.utcn.scd.courseplatform.model.enums.RoomType;

import java.util.List;

public interface RoomRepository extends JpaRepository<Room, Long> {

    /** Filtrare optionala: parametrii null sunt ignorati. */
    @Query("""
            select r from Room r
            where (:type is null or r.type = :type)
              and (:buildingId is null or r.building.id = :buildingId)
            order by r.code
            """)
    List<Room> search(@Param("type") RoomType type, @Param("buildingId") Long buildingId);
}
