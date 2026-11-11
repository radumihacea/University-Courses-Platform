package ro.utcn.scd.courseplatform.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ro.utcn.scd.courseplatform.model.*;
import ro.utcn.scd.courseplatform.model.enums.*;
import ro.utcn.scd.courseplatform.repository.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

/**
 * Populeaza baza de date la prima pornire cu structura anului IV AIA,
 * semestrul I 2026-2027 (dupa orarul oficial al facultatii).
 *
 * Numele profesorilor sunt anonimizate dupa disciplina:
 * Profesor_Curs_SCD_A, Asistent_Lab_SCD, Asistent_Proiect_SCD etc.
 * Toate conturile demo au parola "parola123".
 *
 * Ruleaza doar daca tabela users este goala, deci nu dubleaza datele la reporniri.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private static final String DEMO_PASSWORD = "parola123";
    private static final int STUDY_YEAR = 4;
    private static final int STUDENTS_PER_GROUP = 28;

    private final UserRepository userRepository;
    private final ProfessorRepository professorRepository;
    private final StudentRepository studentRepository;
    private final SeriesRepository seriesRepository;
    private final StudentGroupRepository groupRepository;
    private final SubgroupRepository subgroupRepository;
    private final DisciplineRepository disciplineRepository;
    private final BuildingRepository buildingRepository;
    private final RoomRepository roomRepository;
    private final TeachingAssignmentRepository assignmentRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final SemesterWeekRepository semesterWeekRepository;
    private final ProfessorConstraintRepository constraintRepository;
    private final PasswordEncoder passwordEncoder;

    private final Map<String, Room> rooms = new HashMap<>();
    private final Map<String, Discipline> disciplines = new HashMap<>();
    private final Map<String, TimeSlot> slots = new HashMap<>();
    private String encodedDemoPassword;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Baza de date contine deja date - seeding-ul este sarit.");
            return;
        }
        encodedDemoPassword = passwordEncoder.encode(DEMO_PASSWORD);

        seedTimeSlots();
        seedSemesterWeeks();
        seedBuildingsAndRooms();
        seedDisciplines();

        // --- Serii, grupe, semigrupe ---
        Series seriesA = series("IV-A", "Automatica");
        Series seriesIA = series("IV-IA", "Informatica Aplicata");

        List<Subgroup> subA = new ArrayList<>();
        subA.addAll(group("30141", seriesA, 2));
        subA.addAll(group("30142", seriesA, 2));
        subA.addAll(group("30143", seriesA, 1));

        List<Subgroup> subIA = new ArrayList<>();
        subIA.addAll(group("30144", seriesIA, 2));
        subIA.addAll(group("30145", seriesIA, 2));
        subIA.addAll(group("30146", seriesIA, 2));
        subIA.addAll(group("30147", seriesIA, 1));

        // --- Admin ---
        user("Admin Secretariat", "admin@utcn.ro", Role.ADMIN);

        // --- Titulari de curs ---
        Professor pMad = professor("Profesor_Curs_MAD", "profesor.curs.mad@utcn.ro", "Prof. dr. ing.");
        Professor pMea = professor("Profesor_Curs_MEA", "profesor.curs.mea@utcn.ro", "Conf. dr. ing.");
        Professor pEtica = professor("Profesor_Curs_EIA", "profesor.curs.eia@utcn.ro", "Conf. dr.");
        Professor pScdA = professor("Profesor_Curs_SCD_A", "profesor.curs.scd.a@utcn.ro", "S.l. dr. ing.");
        Professor pFd = professor("Profesor_Curs_FD", "profesor.curs.fd@utcn.ro", "Prof. dr. ing.");
        Professor pScpc = professor("Profesor_Curs_SCPC", "profesor.curs.scpc@utcn.ro", "Prof. dr. ing.");
        Professor pScr = professor("Profesor_Curs_SCR", "profesor.curs.scr@utcn.ro", "Conf. dr. ing.");
        Professor pScdIA = professor("Profesor_Curs_SCD_IA", "profesor.curs.scd.ia@utcn.ro", "Prof. dr. ing.");
        Professor pRc = professor("Profesor_Curs_RC", "profesor.curs.rc@utcn.ro", "Conf. dr. ing.");
        Professor pMp = professor("Profesor_Curs_MP", "profesor.curs.mp@utcn.ro", "Conf. dr. ing.");
        Professor pMkt = professor("Profesor_Curs_MKT", "profesor.curs.mkt@utcn.ro", "Lect. dr.");
        Professor pDpp = professor("Profesor_Curs_DPP", "profesor.curs.dpp@utcn.ro", "Lect. dr.");

        // --- Asistenti de laborator si proiect ---
        Professor aMad = professor("Asistent_Lab_MAD", "asistent.lab.mad@utcn.ro", "Asist. drd. ing.");
        Professor aMea = professor("Asistent_Lab_MEA", "asistent.lab.mea@utcn.ro", "Asist. drd. ing.");
        Professor aScd = professor("Asistent_Lab_SCD", "asistent.lab.scd@utcn.ro", "Asist. dr. ing.");
        Professor aFd = professor("Asistent_Lab_FD", "asistent.lab.fd@utcn.ro", "Asist. drd. ing.");
        Professor aScpc = professor("Asistent_Lab_SCPC", "asistent.lab.scpc@utcn.ro", "Asist. dr. ing.");
        Professor aScr = professor("Asistent_Lab_SCR", "asistent.lab.scr@utcn.ro", "Asist. drd. ing.");
        Professor aRc = professor("Asistent_Lab_RC", "asistent.lab.rc@utcn.ro", "Asist. drd. ing.");
        Professor aMp = professor("Asistent_Lab_MP", "asistent.lab.mp@utcn.ro", "Asist. drd. ing.");
        Professor prScd = professor("Asistent_Proiect_SCD", "asistent.proiect.scd@utcn.ro", "Asist. dr. ing.");
        Professor prScpc = professor("Asistent_Proiect_SCPC", "asistent.proiect.scpc@utcn.ro", "Asist. dr. ing.");
        Professor prScr = professor("Asistent_Proiect_SCR", "asistent.proiect.scr@utcn.ro", "Asist. drd. ing.");

        // --- Cursuri (pe serie) ---
        course("MAD", pMad, seriesA);
        course("MEA", pMea, seriesA);
        course("EIA", pEtica, seriesA);
        course("SCD", pScdA, seriesA);
        course("FD", pFd, seriesA);
        course("SCPC", pScpc, seriesA);
        course("SCR", pScr, seriesA);
        course("MKT", pMkt, seriesA);
        course("DPP", pDpp, seriesA);

        course("EIA", pEtica, seriesIA);
        course("FD", pFd, seriesIA);
        course("SCD", pScdIA, seriesIA);
        course("SCPC", pScpc, seriesIA);
        course("SCR", pScr, seriesIA);
        course("RC", pRc, seriesIA);
        course("MP", pMp, seriesIA);
        course("MKT", pMkt, seriesIA);

        // --- Laboratoare si proiecte comune ambelor serii ---
        List<Subgroup> all = new ArrayList<>(subA);
        all.addAll(subIA);
        perSubgroup("SCD", aScd, ActivityType.LAB, Frequency.WEEKLY, "G6", all);
        perSubgroup("FD", aFd, ActivityType.LAB, Frequency.BIWEEKLY, "S3.5", all);
        perSubgroup("SCPC", aScpc, ActivityType.LAB, Frequency.BIWEEKLY, "304", all);
        perSubgroup("SCR", aScr, ActivityType.LAB, Frequency.WEEKLY, "C12", all);
        perSubgroup("SCD", prScd, ActivityType.PROJECT, Frequency.BIWEEKLY, "310", all);
        perSubgroup("SCPC", prScpc, ActivityType.PROJECT, Frequency.BIWEEKLY, "303", all);
        perSubgroup("SCR", prScr, ActivityType.PROJECT, Frequency.BIWEEKLY, "C13", all);

        // --- Laboratoare specifice fiecarei serii ---
        perSubgroup("MAD", aMad, ActivityType.LAB, Frequency.BIWEEKLY, "303", subA);
        perSubgroup("MEA", aMea, ActivityType.LAB, Frequency.BIWEEKLY, "7", subA);
        perSubgroup("RC", aRc, ActivityType.LAB, Frequency.BIWEEKLY, "505B", subIA);
        perSubgroup("MP", aMp, ActivityType.LAB, Frequency.BIWEEKLY, "308", subIA);

        // --- Studenti demo: cate 2 in fiecare semigrupa ---
        int nr = 1;
        for (Subgroup sg : all) {
            for (int i = 0; i < 2; i++, nr++) {
                student("Student_" + nr, "student" + nr + "@student.utcn.ro",
                        String.format("AIA4-%03d", nr), sg);
            }
        }

        // --- Exemple de restrictii ale profesorilor ---
        constraint(pScdA, DayOfWeek.MONDAY, 8, ConstraintType.UNAVAILABLE, true,
                "Lunea dimineata nu pot preda, am sedinte de departament.");
        constraint(pScdA, DayOfWeek.MONDAY, 10, ConstraintType.UNAVAILABLE, true,
                "Lunea dimineata nu pot preda, am sedinte de departament.");
        constraint(pFd, DayOfWeek.TUESDAY, 10, ConstraintType.PREFERRED, false,
                "As prefera sa tin cursul marti intre 10 si 12.");
        constraint(aScd, DayOfWeek.FRIDAY, 16, ConstraintType.UNAVAILABLE, true,
                "Vinerea dupa ora 16 nu sunt disponibil.");
        constraint(aScd, DayOfWeek.FRIDAY, 18, ConstraintType.UNAVAILABLE, true,
                "Vinerea dupa ora 16 nu sunt disponibil.");

        log.info("Seeding terminat: {} utilizatori, {} asignari didactice, {} sali, {} sloturi.",
                userRepository.count(), assignmentRepository.count(),
                roomRepository.count(), timeSlotRepository.count());
    }

    // ------------------------------------------------------------------
    // Date fixe: sloturi, calendar, sali, discipline
    // ------------------------------------------------------------------

    /** Luni-vineri, sloturi de 2 ore intre 8 si 22 (35 de sloturi). */
    private void seedTimeSlots() {
        for (DayOfWeek day : List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)) {
            for (int hour = 8; hour < 22; hour += 2) {
                TimeSlot ts = new TimeSlot();
                ts.setDayOfWeek(day);
                ts.setStartTime(LocalTime.of(hour, 0));
                ts.setEndTime(LocalTime.of(hour + 2, 0));
                slots.put(day + "-" + hour, timeSlotRepository.save(ts));
            }
        }
    }

    /** Calendarul semestrului I 2026-2027: 14 saptamani + vacanta de iarna. */
    private void seedSemesterWeeks() {
        LocalDate week1 = LocalDate.of(2026, 9, 28);
        for (int nr = 1; nr <= 12; nr++) {
            LocalDate start = week1.plusWeeks(nr - 1);
            week(nr, start, start.plusDays(6), false);
        }
        week(null, LocalDate.of(2026, 12, 21), LocalDate.of(2027, 1, 3), true);
        week(13, LocalDate.of(2027, 1, 4), LocalDate.of(2027, 1, 10), false);
        week(14, LocalDate.of(2027, 1, 11), LocalDate.of(2027, 1, 17), false);
    }

    private void seedBuildingsAndRooms() {
        Building observator = building("Cladirea AC Observator", "Str. Observatorului 2, Cluj-Napoca");
        Building baritiu = building("Cladirea AC Baritiu", "Str. G. Baritiu 26-28, Cluj-Napoca");
        Building dorobantilor = building("Cladirea Dorobantilor", "Str. Dorobantilor 71-73, Cluj-Napoca");
        Building somesStanga = building("Cladirea Somes Stanga", "Str. G. Baritiu 26, Cluj-Napoca");
        Building somesDreapta = building("Cladirea Somes Dreapta", "Str. G. Baritiu 26, Cluj-Napoca");
        Building sport = building("Cladirea AC noua (Sport)", "Str. G. Baritiu 26, Cluj-Napoca");

        // Amfiteatre / sali de curs
        room("356", baritiu, 120, RoomType.LECTURE_HALL);
        room("D21", baritiu, 150, RoomType.LECTURE_HALL);
        room("40", baritiu, 150, RoomType.LECTURE_HALL);
        room("D01", dorobantilor, 150, RoomType.LECTURE_HALL);
        room("D04", dorobantilor, 150, RoomType.LECTURE_HALL);
        room("P03", sport, 200, RoomType.LECTURE_HALL);
        room("H11", somesDreapta, 120, RoomType.LECTURE_HALL);

        // Laboratoare
        room("303", observator, 30, RoomType.LAB);
        room("304", observator, 30, RoomType.LAB);
        room("308", observator, 30, RoomType.LAB);
        room("310", observator, 30, RoomType.LAB);
        room("505B", observator, 30, RoomType.LAB);
        room("7", observator, 30, RoomType.LAB);
        room("G6", baritiu, 30, RoomType.LAB);
        room("C12", dorobantilor, 30, RoomType.LAB);
        room("C13", dorobantilor, 30, RoomType.LAB);
        room("S3.5", somesStanga, 30, RoomType.LAB);
    }

    private void seedDisciplines() {
        discipline("SCD", "Sisteme de Control Distribuit", 5, false);
        discipline("SCPC", "Sisteme de Conducere a Proceselor Continue", 5, false);
        discipline("SCR", "Sisteme de Conducere a Robotilor", 5, false);
        discipline("FD", "Fiabilitate si Diagnoza", 4, false);
        discipline("MAD", "Microsisteme si Achizitii de Date", 5, false);
        discipline("MEA", "Masini Electrice si Actionari", 4, false);
        discipline("RC", "Retele de Calculatoare", 5, false);
        discipline("MP", "Managementul Proiectelor", 3, false);
        discipline("EIA", "Etica si Integritate Academica", 1, false);
        discipline("MKT", "Marketing", 3, true);
        discipline("DPP", "Dezvoltare Personala si Profesionala", 2, true);
    }

    // ------------------------------------------------------------------
    // Metode ajutatoare
    // ------------------------------------------------------------------

    private void week(Integer nr, LocalDate start, LocalDate end, boolean holiday) {
        SemesterWeek w = new SemesterWeek();
        w.setWeekNumber(nr);
        w.setStartDate(start);
        w.setEndDate(end);
        w.setHoliday(holiday);
        w.setParity(holiday ? WeekParity.ALL : (nr % 2 == 1 ? WeekParity.ODD : WeekParity.EVEN));
        semesterWeekRepository.save(w);
    }

    private Building building(String name, String address) {
        Building b = new Building();
        b.setName(name);
        b.setAddress(address);
        return buildingRepository.save(b);
    }

    private void room(String code, Building building, int capacity, RoomType type) {
        Room r = new Room();
        r.setCode(code);
        r.setBuilding(building);
        r.setCapacity(capacity);
        r.setType(type);
        rooms.put(code, roomRepository.save(r));
    }

    private void discipline(String code, String name, int credits, boolean optional) {
        Discipline d = new Discipline();
        d.setCode(code);
        d.setName(name);
        d.setCredits(credits);
        d.setStudyYear(STUDY_YEAR);
        d.setOptional(optional);
        disciplines.put(code, disciplineRepository.save(d));
    }

    private Series series(String name, String specialization) {
        Series s = new Series();
        s.setName(name);
        s.setStudyYear(STUDY_YEAR);
        s.setSpecialization(specialization);
        return seriesRepository.save(s);
    }

    /** Creeaza o grupa si semigrupele ei; o grupa fara impartire are o singura semigrupa. */
    private List<Subgroup> group(String code, Series series, int subgroupCount) {
        StudentGroup g = new StudentGroup();
        g.setCode(code);
        g.setSeries(series);
        g = groupRepository.save(g);

        List<Subgroup> result = new ArrayList<>();
        for (int i = 1; i <= subgroupCount; i++) {
            Subgroup sg = new Subgroup();
            sg.setName(code + "/" + i);
            sg.setGroup(g);
            // O grupa are 28 de studenti, impartiti egal intre semigrupe
            sg.setStudentCount(STUDENTS_PER_GROUP / subgroupCount);
            result.add(subgroupRepository.save(sg));
        }
        return result;
    }

    private User user(String fullName, String email, Role role) {
        User u = new User();
        u.setFullName(fullName);
        u.setEmail(email);
        u.setPasswordHash(encodedDemoPassword);
        u.setRole(role);
        return userRepository.save(u);
    }

    private Professor professor(String name, String email, String title) {
        Professor p = new Professor();
        p.setUser(user(name, email, Role.PROFESSOR));
        p.setTitle(title);
        p.setDepartment("Automatica");
        return professorRepository.save(p);
    }

    private void student(String name, String email, String registrationNumber, Subgroup subgroup) {
        Student s = new Student();
        s.setUser(user(name, email, Role.STUDENT));
        s.setRegistrationNumber(registrationNumber);
        s.setSubgroup(subgroup);
        studentRepository.save(s);
    }

    private void course(String disciplineCode, Professor professor, Series series) {
        TeachingAssignment a = new TeachingAssignment();
        a.setDiscipline(disciplines.get(disciplineCode));
        a.setProfessor(professor);
        a.setType(ActivityType.COURSE);
        a.setFrequency(Frequency.WEEKLY);
        a.setSeries(series);
        assignmentRepository.save(a);
    }

    private void perSubgroup(String disciplineCode, Professor professor, ActivityType type,
                             Frequency frequency, String roomCode, List<Subgroup> subgroups) {
        for (Subgroup sg : subgroups) {
            TeachingAssignment a = new TeachingAssignment();
            a.setDiscipline(disciplines.get(disciplineCode));
            a.setProfessor(professor);
            a.setType(type);
            a.setFrequency(frequency);
            a.setPreferredRoom(rooms.get(roomCode));
            a.setSubgroup(sg);
            assignmentRepository.save(a);
        }
    }

    private void constraint(Professor professor, DayOfWeek day, int startHour,
                            ConstraintType type, boolean mandatory, String originalText) {
        ProfessorConstraint c = new ProfessorConstraint();
        c.setProfessor(professor);
        c.setTimeSlot(slots.get(day + "-" + startHour));
        c.setType(type);
        c.setMandatory(mandatory);
        c.setOriginalText(originalText);
        constraintRepository.save(c);
    }
}
