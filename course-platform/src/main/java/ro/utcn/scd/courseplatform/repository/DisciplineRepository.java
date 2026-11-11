package ro.utcn.scd.courseplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.utcn.scd.courseplatform.model.Discipline;

public interface DisciplineRepository extends JpaRepository<Discipline, Long> {
}
