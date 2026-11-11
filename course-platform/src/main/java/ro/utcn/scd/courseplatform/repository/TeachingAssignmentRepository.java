package ro.utcn.scd.courseplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.utcn.scd.courseplatform.model.TeachingAssignment;

public interface TeachingAssignmentRepository extends JpaRepository<TeachingAssignment, Long> {
}
