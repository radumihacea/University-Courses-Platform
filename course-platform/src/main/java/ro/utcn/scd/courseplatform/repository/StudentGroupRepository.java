package ro.utcn.scd.courseplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.utcn.scd.courseplatform.model.StudentGroup;

public interface StudentGroupRepository extends JpaRepository<StudentGroup, Long> {
}
