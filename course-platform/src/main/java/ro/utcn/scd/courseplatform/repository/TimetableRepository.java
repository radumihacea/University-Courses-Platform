package ro.utcn.scd.courseplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.utcn.scd.courseplatform.model.Timetable;

public interface TimetableRepository extends JpaRepository<Timetable, Long> {
}
