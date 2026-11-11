package ro.utcn.scd.courseplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.utcn.scd.courseplatform.model.Professor;

public interface ProfessorRepository extends JpaRepository<Professor, Long> {
}
