package ro.utcn.scd.courseplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.utcn.scd.courseplatform.model.Enrollment;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
}
