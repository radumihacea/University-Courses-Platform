package ro.utcn.scd.courseplatform.dto;

/** Incarcarea unui profesor: numar de activitati si ore pe saptamana (medie pe doua saptamani). */
public record ProfessorLoadDto(
        Long professorId,
        String professorName,
        long activities,
        double weeklyHours
) {
}
