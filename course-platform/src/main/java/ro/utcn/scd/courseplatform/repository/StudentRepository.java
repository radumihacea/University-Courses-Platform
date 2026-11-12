package ro.utcn.scd.courseplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.utcn.scd.courseplatform.model.Student;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {

    List<Student> findBySubgroupIdOrderByRegistrationNumber(Long subgroupId);

    List<Student> findBySubgroupIdIn(Collection<Long> subgroupIds);

    Optional<Student> findByUserId(Long userId);
}
