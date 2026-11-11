package ro.utcn.scd.courseplatform.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** O serie de studenti, ex: "I-A" (grupele 30111-30113). */
@Entity
@Table(name = "series")
@Getter
@Setter
@NoArgsConstructor
public class Series {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String name;

    @Column(nullable = false)
    private Integer studyYear;

    /** Ex: "Automatica si Informatica Aplicata" */
    @Column(nullable = false, length = 100)
    private String specialization;
}
