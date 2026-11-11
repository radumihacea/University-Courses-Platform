package ro.utcn.scd.courseplatform.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** O disciplina, ex: "PCLP - Programarea calculatoarelor si limbaje de programare". */
@Entity
@Table(name = "disciplines")
@Getter
@Setter
@NoArgsConstructor
public class Discipline {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 15)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false)
    private Integer credits;

    @Column(nullable = false)
    private Integer studyYear;

    /** true pentru discipline la care studentul se inroleaza (limbi straine, facultative). */
    @Column(name = "is_optional", nullable = false)
    private boolean optional;
}
