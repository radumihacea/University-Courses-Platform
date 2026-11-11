package ro.utcn.scd.courseplatform.model.enums;

/**
 * Cat de des are loc o activitate (folosit la asignari).
 * WEEKLY   -> in fiecare saptamana (ex: "SCD 2L")
 * BIWEEKLY -> o data la doua saptamani (ex: "FD 4L"); generatorul alege
 *             daca ora cade in saptamanile pare sau impare.
 */
public enum Frequency {
    WEEKLY,
    BIWEEKLY
}
