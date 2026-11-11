package ro.utcn.scd.courseplatform.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** O semigrupa, ex: "30111/1". Laboratoarele se tin pe semigrupe. */
@Entity
@Table(name = "subgroups")
@Getter
@Setter
@NoArgsConstructor
public class Subgroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 15)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private StudentGroup group;

    /** Numarul estimat de studenti; folosit la verificarea capacitatii salii. */
    @Column(nullable = false)
    private Integer studentCount;
}
