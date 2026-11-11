package ro.utcn.scd.courseplatform.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ro.utcn.scd.courseplatform.model.enums.TimetableStatus;

import java.time.LocalDateTime;

/**
 * O versiune de orar. Generatorul creeaza un orar DRAFT,
 * adminul il revizuieste si il publica (PUBLISHED).
 */
@Entity
@Table(name = "timetables")
@Getter
@Setter
@NoArgsConstructor
public class Timetable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    /** Ex: "2026-2027" */
    @Column(nullable = false, length = 9)
    private String academicYear;

    @Column(nullable = false)
    private Integer semester;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TimetableStatus status = TimetableStatus.DRAFT;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime publishedAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
