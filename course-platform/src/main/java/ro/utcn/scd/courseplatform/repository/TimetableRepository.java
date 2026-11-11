package ro.utcn.scd.courseplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.utcn.scd.courseplatform.model.Timetable;
import ro.utcn.scd.courseplatform.model.enums.TimetableStatus;

import java.util.List;
import java.util.Optional;

public interface TimetableRepository extends JpaRepository<Timetable, Long> {

    List<Timetable> findAllByOrderByCreatedAtDesc();

    List<Timetable> findByStatus(TimetableStatus status);

    /** Orarul publicat cel mai recent (cel vazut de studenti si profesori). */
    Optional<Timetable> findFirstByStatusOrderByPublishedAtDesc(TimetableStatus status);
}
