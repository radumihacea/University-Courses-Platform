package ro.utcn.scd.courseplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.utcn.scd.courseplatform.model.SemesterWeek;

public interface SemesterWeekRepository extends JpaRepository<SemesterWeek, Long> {
}
