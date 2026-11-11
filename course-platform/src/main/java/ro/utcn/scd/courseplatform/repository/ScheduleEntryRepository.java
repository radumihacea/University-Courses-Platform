package ro.utcn.scd.courseplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ro.utcn.scd.courseplatform.model.ScheduleEntry;

import java.util.List;

public interface ScheduleEntryRepository extends JpaRepository<ScheduleEntry, Long> {

    /** Toate orele unui orar, cu relatiile incarcate dintr-o singura interogare (fara N+1). */
    @Query("""
            select e from ScheduleEntry e
            join fetch e.assignment a
            join fetch a.discipline
            join fetch a.professor p
            join fetch p.user
            join fetch e.room
            join fetch e.timeSlot
            left join fetch a.series
            left join fetch a.group
            left join fetch a.subgroup
            where e.timetable.id = :timetableId
            """)
    List<ScheduleEntry> findAllOfTimetable(@Param("timetableId") Long timetableId);

    /**
     * QUERY CUSTOM: orarul unei semigrupe = laboratoarele/proiectele ei
     * + seminariile grupei din care face parte + cursurile seriei din care face parte.
     */
    @Query("""
            select e from ScheduleEntry e
            join fetch e.assignment a
            join fetch a.discipline
            join fetch a.professor p
            join fetch p.user
            join fetch e.room
            join fetch e.timeSlot
            left join fetch a.series se
            left join fetch a.group g
            left join fetch a.subgroup sg
            where e.timetable.id = :timetableId
              and (sg.id = :subgroupId
                   or g.id = (select s.group.id from Subgroup s where s.id = :subgroupId)
                   or se.id = (select s.group.series.id from Subgroup s where s.id = :subgroupId))
            """)
    List<ScheduleEntry> findForSubgroup(@Param("timetableId") Long timetableId,
                                        @Param("subgroupId") Long subgroupId);

    /**
     * QUERY CUSTOM: perechi de ore care se suprapun (acelasi slot si paritati compatibile)
     * si folosesc aceeasi sala sau acelasi profesor. Folosita la detectarea conflictelor.
     */
    @Query("""
            select e1, e2 from ScheduleEntry e1, ScheduleEntry e2
            where e1.timetable.id = :timetableId and e2.timetable.id = :timetableId
              and e1.id < e2.id
              and e1.timeSlot.id = e2.timeSlot.id
              and (e1.parity = e2.parity
                   or e1.parity = ro.utcn.scd.courseplatform.model.enums.WeekParity.ALL
                   or e2.parity = ro.utcn.scd.courseplatform.model.enums.WeekParity.ALL)
              and (e1.room.id = e2.room.id
                   or e1.assignment.professor.id = e2.assignment.professor.id)
            """)
    List<Object[]> findRoomOrProfessorOverlaps(@Param("timetableId") Long timetableId);

    /**
     * QUERY CUSTOM (agregare): incarcarea fiecarui profesor intr-un orar.
     * O ora saptamanala conteaza integral, una din doua in doua saptamani conteaza pe jumatate.
     * Rezultat: [professorId, nume, numar de activitati, ore pe saptamana (medie)]
     */
    @Query("""
            select p.id, u.fullName, count(e),
                   sum(case when e.parity = ro.utcn.scd.courseplatform.model.enums.WeekParity.ALL
                            then a.hoursPerSession * 1.0 else a.hoursPerSession * 0.5 end)
            from ScheduleEntry e
            join e.assignment a
            join a.professor p
            join p.user u
            where e.timetable.id = :timetableId
            group by p.id, u.fullName
            """)
    List<Object[]> professorLoad(@Param("timetableId") Long timetableId);

    /**
     * QUERY CUSTOM (agregare): gradul de ocupare al salilor intr-un orar.
     * Rezultat: [roomId, cod, capacitate, numar de activitati, sloturi ocupate (medie pe saptamana)]
     */
    @Query("""
            select r.id, r.code, r.capacity, count(e),
                   sum(case when e.parity = ro.utcn.scd.courseplatform.model.enums.WeekParity.ALL
                            then 1.0 else 0.5 end)
            from ScheduleEntry e
            join e.room r
            where e.timetable.id = :timetableId
            group by r.id, r.code, r.capacity
            """)
    List<Object[]> roomOccupancy(@Param("timetableId") Long timetableId);

    long countByTimetableId(Long timetableId);

    @Modifying
    @Query("delete from ScheduleEntry e where e.timetable.id = :timetableId")
    void deleteAllOfTimetable(@Param("timetableId") Long timetableId);
}
