package ro.utcn.scd.courseplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.utcn.scd.courseplatform.model.Student;

import java.util.List;

public interface StudentRepository extends JpaRepository<Student, Long> {

    List<Student> findBySubgroupIdOrderByRegistrationNumber(Long subgroupId);
}
