package ro.utcn.scd.courseplatform.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "professors")
@Getter
@Setter
@NoArgsConstructor
public class Professor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    /** Ex: "Prof. dr. ing.", "Conf. dr.", "Asist. drd." */
    @Column(length = 50)
    private String title;

    @Column(length = 100)
    private String department;
}
