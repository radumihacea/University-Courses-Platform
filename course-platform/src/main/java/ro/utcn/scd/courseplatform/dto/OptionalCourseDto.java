package ro.utcn.scd.courseplatform.dto;

import ro.utcn.scd.courseplatform.model.enums.ActivityType;

import java.util.List;

/**
 * O disciplina optionala la care se poate inscrie un student.
 * schedule = cand si unde are loc in orarul publicat (ex: "Miercuri 08:00-10:00, sala 356").
 */
public record OptionalCourseDto(
        Long assignmentId,
        String disciplineCode,
        String disciplineName,
        Integer credits,
        ActivityType type,
        String professorName,
        boolean enrolled,
        List<String> schedule
) {
}
