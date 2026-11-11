package ro.utcn.scd.courseplatform.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ro.utcn.scd.courseplatform.model.enums.WeekParity;

import java.time.LocalDate;

/**
 * Calendarul semestrului (ex: saptamana 1 = 28 sept - 2 oct, impara).
 * Folosit pentru a afla daca saptamana curenta e para sau impara
 * si ce ore are un student "azi".
 */
@Entity
@Table(name = "semester_weeks")
@Getter
@Setter
@NoArgsConstructor
public class SemesterWeek {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Numarul saptamanii didactice (1-14); null pentru vacanta. */
    private Integer weekNumber;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    /** ODD sau EVEN pentru saptamanile didactice; ALL pentru vacanta. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private WeekParity parity;

    @Column(nullable = false)
    private boolean holiday;
}
