package ro.utcn.scd.courseplatform.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.AssertTrue;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ro.utcn.scd.courseplatform.model.enums.ActivityType;
import ro.utcn.scd.courseplatform.model.enums.Frequency;

/**
 * CE trebuie programat in orar: o disciplina, de un anumit tip,
 * predata de un profesor, unui anumit public (serie / grupa / semigrupa).
 *
 * Ex: "SCD, laborator, 2 ore, semigrupa 30141/1, Asistent_Lab_3, saptamanal, sala G6".
 *
 * Generatorul de orar transforma fiecare asignare intr-un ScheduleEntry (CAND si UNDE).
 */
@Entity
@Table(name = "teaching_assignments")
@Getter
@Setter
@NoArgsConstructor
public class TeachingAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "discipline_id", nullable = false)
    private Discipline discipline;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "professor_id", nullable = false)
    private Professor professor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ActivityType type;

    /** Durata unei sedinte, in ore (in orarul nostru, mereu 2 = un slot). */
    @Column(nullable = false)
    private Integer hoursPerSession = 2;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Frequency frequency = Frequency.WEEKLY;

    /**
     * Sala in care trebuie sa aiba loc (ex: laboratorul de SCD doar in G6).
     * Daca e null, generatorul alege orice sala potrivita ca tip si capacitate.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preferred_room_id")
    private Room preferredRoom;

    /** Completat doar pentru cursuri. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "series_id")
    private Series series;

    /** Completat doar pentru seminarii. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private StudentGroup group;

    /** Completat doar pentru laboratoare si proiecte. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subgroup_id")
    private Subgroup subgroup;

    /** Verifica faptul ca este completat exact publicul potrivit tipului de activitate. */
    @AssertTrue(message = "Cursul se asigneaza unei serii, seminarul unei grupe, laboratorul/proiectul unei semigrupe")
    public boolean isTargetConsistent() {
        if (type == null) {
            return false;
        }
        return switch (type) {
            case COURSE -> series != null && group == null && subgroup == null;
            case SEMINAR -> group != null && series == null && subgroup == null;
            case LAB, PROJECT -> subgroup != null && series == null && group == null;
        };
    }
}
