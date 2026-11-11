package ro.utcn.scd.courseplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ro.utcn.scd.courseplatform.model.ChangeRequest;
import ro.utcn.scd.courseplatform.model.enums.RequestStatus;

import java.util.List;

public interface ChangeRequestRepository extends JpaRepository<ChangeRequest, Long> {

    @Query("""
            select c from ChangeRequest c
            where (:status is null or c.status = :status)
              and (:professorId is null or c.professor.id = :professorId)
            order by c.createdAt desc
            """)
    List<ChangeRequest> search(@Param("status") RequestStatus status, @Param("professorId") Long professorId);

    @Modifying
    @Query("""
            delete from ChangeRequest c
            where c.scheduleEntry.id in (select e.id from ScheduleEntry e where e.timetable.id = :timetableId)
            """)
    void deleteAllOfTimetable(@Param("timetableId") Long timetableId);
}
