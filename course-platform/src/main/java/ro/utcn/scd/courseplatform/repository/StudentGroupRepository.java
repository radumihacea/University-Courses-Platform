package ro.utcn.scd.courseplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.utcn.scd.courseplatform.model.StudentGroup;

import java.util.List;

public interface StudentGroupRepository extends JpaRepository<StudentGroup, Long> {

    List<StudentGroup> findBySeriesIdOrderByCode(Long seriesId);
}
