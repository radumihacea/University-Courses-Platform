package ro.utcn.scd.courseplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.utcn.scd.courseplatform.model.ProfessorConstraint;

public interface ProfessorConstraintRepository extends JpaRepository<ProfessorConstraint, Long> {
}
