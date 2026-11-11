package ro.utcn.scd.courseplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.utcn.scd.courseplatform.model.TimeSlot;

public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {
}
