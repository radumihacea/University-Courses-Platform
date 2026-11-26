package ro.utcn.scd.mobile.data.model;

/** O restrictie a profesorului: un interval in care nu poate preda sau pe care il prefera. */
public class ProfessorConstraint {
    public Long id;
    public Long professorId;
    public Long timeSlotId;
    public String timeSlotLabel;
    /** UNAVAILABLE sau PREFERRED */
    public String type;
    /** true = obligatoriu (generatorul nu pune ore acolo); false = doar de evitat */
    public Boolean mandatory;
    public String originalText;
}
