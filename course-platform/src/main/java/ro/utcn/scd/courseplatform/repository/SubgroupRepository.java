package ro.utcn.scd.courseplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.utcn.scd.courseplatform.model.Subgroup;

import java.util.List;

public interface SubgroupRepository extends JpaRepository<Subgroup, Long> {

    List<Subgroup> findByGroupIdOrderByName(Long groupId);
}
