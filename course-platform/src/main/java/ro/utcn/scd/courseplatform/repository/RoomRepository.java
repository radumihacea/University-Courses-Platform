package ro.utcn.scd.courseplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ro.utcn.scd.courseplatform.model.Room;
import ro.utcn.scd.courseplatform.model.enums.RoomType;
import ro.utcn.scd.courseplatform.model.enums.WeekParity;

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

    /**
     * QUERY CUSTOM: salile libere intr-un slot al unui orar (NOT EXISTS),
     * cu capacitate minima si tip optional. O ora saptamanala (ALL) blocheaza
     * ambele paritati; o ora para/impara blocheaza doar paritatea ei.
     */
    @Query("""
            select r from Room r
            where r.capacity >= :minCapacity
              and (:type is null or r.type = :type)
              and not exists (
                  select e from ScheduleEntry e
                  where e.timetable.id = :timetableId
                    and e.room = r
                    and e.timeSlot.id = :timeSlotId
                    and (:parity = ro.utcn.scd.courseplatform.model.enums.WeekParity.ALL
                         or e.parity = ro.utcn.scd.courseplatform.model.enums.WeekParity.ALL
                         or e.parity = :parity))
            order by r.capacity, r.code
            """)
    List<Room> findAvailable(@Param("timetableId") Long timetableId,
                             @Param("timeSlotId") Long timeSlotId,
                             @Param("parity") WeekParity parity,
                             @Param("minCapacity") int minCapacity,
                             @Param("type") RoomType type);
}
