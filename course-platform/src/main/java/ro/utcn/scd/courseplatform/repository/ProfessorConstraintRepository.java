package ro.utcn.scd.courseplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.utcn.scd.courseplatform.model.ProfessorConstraint;

import java.util.List;

public interface ProfessorConstraintRepository extends JpaRepository<ProfessorConstraint, Long> {

    List<ProfessorConstraint> findByProfessorId(Long professorId);
}
