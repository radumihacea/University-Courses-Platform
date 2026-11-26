package ro.utcn.scd.mobile.data.model;

import java.util.List;

/** O disciplina optionala si daca studentul este inscris la ea. */
public class OptionalCourse {
    public Long assignmentId;
    public String disciplineCode;
    public String disciplineName;
    public Integer credits;
    public String type;
    public String professorName;
    public boolean enrolled;
    /** cand si unde are loc, ex: "Miercuri 08:00-10:00, sala 356" */
    public List<String> schedule;
}
