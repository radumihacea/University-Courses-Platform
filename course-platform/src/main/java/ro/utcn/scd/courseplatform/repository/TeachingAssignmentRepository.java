package ro.utcn.scd.courseplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ro.utcn.scd.courseplatform.model.TeachingAssignment;
import ro.utcn.scd.courseplatform.model.enums.ActivityType;

import java.util.List;

public interface TeachingAssignmentRepository extends JpaRepository<TeachingAssignment, Long> {

    /** Filtrare optionala dupa profesor, disciplina, semigrupa si tip de activitate. */
    @Query("""
            select a from TeachingAssignment a
            where (:professorId is null or a.professor.id = :professorId)
              and (:disciplineId is null or a.discipline.id = :disciplineId)
              and (:subgroupId is null or a.subgroup.id = :subgroupId)
              and (:type is null or a.type = :type)
            order by a.id
            """)
    List<TeachingAssignment> search(@Param("professorId") Long professorId,
                                    @Param("disciplineId") Long disciplineId,
                                    @Param("subgroupId") Long subgroupId,
                                    @Param("type") ActivityType type);
}
