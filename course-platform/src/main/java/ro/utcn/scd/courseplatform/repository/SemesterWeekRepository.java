package ro.utcn.scd.courseplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.utcn.scd.courseplatform.model.SemesterWeek;

import java.time.LocalDate;
import java.util.Optional;

public interface SemesterWeekRepository extends JpaRepository<SemesterWeek, Long> {

    /** Saptamana din calendar care contine data data (startDate <= data <= endDate). */
    Optional<SemesterWeek> findFirstByStartDateLessThanEqualAndEndDateGreaterThanEqual(LocalDate d1, LocalDate d2);
}
