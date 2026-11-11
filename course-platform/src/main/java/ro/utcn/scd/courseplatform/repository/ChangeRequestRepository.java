package ro.utcn.scd.courseplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.utcn.scd.courseplatform.model.ChangeRequest;

public interface ChangeRequestRepository extends JpaRepository<ChangeRequest, Long> {
}
