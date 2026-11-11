package ro.utcn.scd.courseplatform.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ro.utcn.scd.courseplatform.model.enums.ConstraintType;

/**
 * O restrictie a unui profesor pentru un anumit slot orar.
 * Ex: "indisponibil luni 08-10" (obligatorie) sau "prefer miercuri 10-12" (optionala).
 *
 * originalText pastreaza ce a scris profesorul in limbaj natural,
 * iar AI-ul il transforma in una sau mai multe astfel de restrictii.
 */
@Entity
@Table(name = "professor_constraints")
@Getter
@Setter
@NoArgsConstructor
public class ProfessorConstraint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "professor_id", nullable = false)
    private Professor professor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "time_slot_id", nullable = false)
    private TimeSlot timeSlot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConstraintType type;

    /** true = trebuie respectata; false = doar preferinta. */
    @Column(nullable = false)
    private boolean mandatory;

    @Column(length = 1000)
    private String originalText;
}
