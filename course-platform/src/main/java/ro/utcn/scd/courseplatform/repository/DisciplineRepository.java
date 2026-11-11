package ro.utcn.scd.courseplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ro.utcn.scd.courseplatform.model.Discipline;

import java.util.List;

public interface DisciplineRepository extends JpaRepository<Discipline, Long> {

    /** Filtrare optionala dupa an de studiu si dupa tip (optionala / obligatorie). */
    @Query("""
            select d from Discipline d
            where (:studyYear is null or d.studyYear = :studyYear)
              and (:optional is null or d.optional = :optional)
            order by d.code
            """)
    List<Discipline> search(@Param("studyYear") Integer studyYear, @Param("optional") Boolean optional);
}
