package ro.utcn.scd.courseplatform.scheduling;

import ro.utcn.scd.courseplatform.model.enums.ActivityType;
import ro.utcn.scd.courseplatform.model.enums.Frequency;
import ro.utcn.scd.courseplatform.model.enums.RoomType;
import ro.utcn.scd.courseplatform.model.enums.WeekParity;

import java.time.DayOfWeek;
import java.util.*;

/**
 * Algoritmul de generare a orarului (Java pur, fara Spring / baza de date).
 *
 * Problema: fiecare activitate (Task) trebuie pusa intr-un slot orar, intr-o sala
 * si - daca are loc o data la doua saptamani - intr-o paritate (para / impara).
 *
 * Restrictii OBLIGATORII (hard) - nu sunt incalcate niciodata:
 *   1. un profesor nu poate fi in doua locuri in acelasi timp;
 *   2. o sala nu poate gazdui doua activitati in acelasi timp;
 *   3. o semigrupa nu poate avea doua activitati in acelasi timp
 *      (un curs ocupa toate semigrupele seriei);
 *   4. sala trebuie sa aiba tipul potrivit si capacitate suficienta
 *      (sau sa fie exact sala ceruta, daca activitatea are o sala preferata);
 *   5. profesorul nu poate preda in sloturile marcate "indisponibil".
 *
 * Restrictii OPTIONALE (soft) - se minimizeaza un scor de penalizare:
 *   - ore tarzii (dupa 18) sau foarte devreme (8);
 *   - "ferestre" in orarul studentilor si zile prea incarcate;
 *   - sali mult prea mari pentru activitate;
 *   - sloturile pe care profesorul ar prefera sa le evite;
 *   - bonus pentru sloturile preferate de profesori.
 *
 * Metoda: euristica "greedy" cu ordonare dupa dificultate (cele mai constranse
 * activitati se plaseaza primele), repetata de mai multe ori cu mici variatii
 * aleatoare; se pastreaza cea mai buna varianta (cele mai putine activitati
 * neplasate, apoi cel mai mic scor).
 */
public class TimetableSolver {

    // ------------------------------------------------------------------
    // Modelul de intrare / iesire
    // ------------------------------------------------------------------

    /** O activitate de programat (corespunde unei asignari didactice). */
    public record Task(long assignmentId, long professorId, ActivityType type, Frequency frequency,
                       Long preferredRoomId, Set<Long> subgroupIds, int audienceSize, String label) {
    }

    public record Slot(long id, DayOfWeek day, int startHour) {
    }

    public record RoomOption(long id, String code, int capacity, RoomType type) {
    }

    /**
     * unavailable = indisponibilitati obligatorii (hard);
     * avoid       = indisponibilitati neobligatorii (soft, penalizate);
     * preferred   = sloturi preferate (bonus).
     */
    public record Input(List<Task> tasks, List<Slot> slots, List<RoomOption> rooms,
                       Map<Long, Set<Long>> unavailableSlotsByProfessor,
                       Map<Long, Set<Long>> avoidSlotsByProfessor,
                       Map<Long, Set<Long>> preferredSlotsByProfessor) {
    }

    public record Placement(long assignmentId, long slotId, long roomId, WeekParity parity) {
    }

    public record Unplaced(Task task, String reason) {
    }

    public record Result(List<Placement> placements, List<Unplaced> unplaced, double score, int attempts) {
    }

    // ------------------------------------------------------------------

    private final Input input;
    private final Map<Long, Slot> slotsById = new HashMap<>();

    public TimetableSolver(Input input) {
        this.input = input;
        input.slots().forEach(s -> slotsById.put(s.id(), s));
    }

    /** Ruleaza algoritmul de mai multe ori si intoarce cea mai buna solutie gasita. */
    public Result solve(int attempts, long seed) {
        Random random = new Random(seed);
        Result best = null;
        for (int i = 0; i < attempts; i++) {
            Result current = attempt(new Random(random.nextLong()), i == 0 ? 0.0 : 1.0);
            if (best == null || isBetter(current, best)) {
                best = current;
            }
        }
        return new Result(best.placements(), best.unplaced(), best.score(), attempts);
    }

    private boolean isBetter(Result a, Result b) {
        if (a.unplaced().size() != b.unplaced().size()) {
            return a.unplaced().size() < b.unplaced().size();
        }
        return a.score() < b.score();
    }

    // ------------------------------------------------------------------
    // O singura incercare greedy
    // ------------------------------------------------------------------

    private Result attempt(Random random, double noise) {
        State state = new State();
        List<Placement> placements = new ArrayList<>();
        List<Unplaced> unplaced = new ArrayList<>();
        double totalScore = 0;

        for (Task task : orderByDifficulty(random, noise)) {
            Candidate best = null;
            for (Slot slot : input.slots()) {
                if (isUnavailable(task, slot)) {
                    continue;
                }
                for (WeekParity parity : paritiesFor(task)) {
                    if (!state.isFree(task, slot.id(), parity)) {
                        continue;
                    }
                    for (RoomOption room : roomsFor(task)) {
                        if (!state.isRoomFree(room.id(), slot.id(), parity)) {
                            continue;
                        }
                        double score = penalty(task, slot, room, state) + random.nextDouble() * noise;
                        if (best == null || score < best.score) {
                            best = new Candidate(slot, room, parity, score);
                        }
                    }
                }
            }
            if (best == null) {
                unplaced.add(new Unplaced(task, explainFailure(task)));
            } else {
                state.occupy(task, best.slot, best.room.id(), best.parity);
                placements.add(new Placement(task.assignmentId(), best.slot.id(), best.room.id(), best.parity));
                totalScore += best.score;
            }
        }
        return new Result(placements, unplaced, totalScore, 1);
    }

    private record Candidate(Slot slot, RoomOption room, WeekParity parity, double score) {
    }

    /**
     * Ordinea de plasare: intai activitatile cu public mare (cursuri), apoi cele legate
     * de o sala anume foarte solicitata, apoi cele ale profesorilor foarte incarcati.
     */
    private List<Task> orderByDifficulty(Random random, double noise) {
        Map<Long, Double> roomLoad = new HashMap<>();
        Map<Long, Double> professorLoad = new HashMap<>();
        for (Task t : input.tasks()) {
            double w = t.frequency() == Frequency.WEEKLY ? 1.0 : 0.5;
            if (t.preferredRoomId() != null) {
                roomLoad.merge(t.preferredRoomId(), w, Double::sum);
            }
            professorLoad.merge(t.professorId(), w, Double::sum);
        }
        Map<Task, Double> difficulty = new HashMap<>();
        for (Task t : input.tasks()) {
            double d = t.subgroupIds().size() * 10.0
                    + (t.preferredRoomId() != null ? roomLoad.get(t.preferredRoomId()) : 0)
                    + professorLoad.get(t.professorId())
                    + (t.frequency() == Frequency.WEEKLY ? 2 : 0)
                    + input.unavailableSlotsByProfessor().getOrDefault(t.professorId(), Set.of()).size()
                    + random.nextDouble() * 3 * noise;
            difficulty.put(t, d);
        }
        List<Task> ordered = new ArrayList<>(input.tasks());
        ordered.sort(Comparator.comparingDouble((Task t) -> difficulty.get(t)).reversed());
        return ordered;
    }

    private boolean isUnavailable(Task task, Slot slot) {
        return input.unavailableSlotsByProfessor()
                .getOrDefault(task.professorId(), Set.of())
                .contains(slot.id());
    }

    private List<WeekParity> paritiesFor(Task task) {
        return task.frequency() == Frequency.WEEKLY
                ? List.of(WeekParity.ALL)
                : List.of(WeekParity.ODD, WeekParity.EVEN);
    }

    /** Salile in care poate avea loc activitatea (restrictia 4). */
    private List<RoomOption> roomsFor(Task task) {
        if (task.preferredRoomId() != null) {
            return input.rooms().stream().filter(r -> r.id() == task.preferredRoomId()).toList();
        }
        return input.rooms().stream()
                .filter(r -> r.capacity() >= task.audienceSize())
                .filter(r -> switch (task.type()) {
                    case COURSE -> r.type() == RoomType.LECTURE_HALL;
                    case SEMINAR -> r.type() == RoomType.SEMINAR_ROOM || r.type() == RoomType.LECTURE_HALL;
                    case LAB, PROJECT -> r.type() == RoomType.LAB;
                })
                .toList();
    }

    // ------------------------------------------------------------------
    // Scorul de penalizare (restrictiile soft) - mai mic = mai bine
    // ------------------------------------------------------------------

    private double penalty(Task task, Slot slot, RoomOption room, State state) {
        double p = 0;

        // Ore tarzii / foarte devreme
        if (slot.startHour() >= 20) p += 6;
        else if (slot.startHour() >= 18) p += 3;
        else if (slot.startHour() == 8) p += 1;

        // Preferintele profesorului
        if (input.avoidSlotsByProfessor().getOrDefault(task.professorId(), Set.of()).contains(slot.id())) {
            p += 5;
        }
        if (input.preferredSlotsByProfessor().getOrDefault(task.professorId(), Set.of()).contains(slot.id())) {
            p -= 5;
        }

        // Ferestre si incarcarea zilei pentru studenti (media pe semigrupele afectate)
        double studentPenalty = 0;
        for (Long sg : task.subgroupIds()) {
            Set<Integer> hours = state.hoursOf(sg, slot.day());
            if (hours.isEmpty()) {
                studentPenalty += 1;                      // zi noua: preferam zile compacte
            } else {
                Set<Integer> after = new HashSet<>(hours);
                after.add(slot.startHour());
                int span = (Collections.max(after) - Collections.min(after)) / 2 + 1;
                int gaps = span - after.size();
                int gapsBefore = (Collections.max(hours) - Collections.min(hours)) / 2 + 1 - hours.size();
                studentPenalty += 2.0 * (gaps - gapsBefore);  // ferestre noi create
                if (hours.size() >= 4) studentPenalty += 3;  // zi prea incarcata
            }
        }
        p += studentPenalty / Math.max(1, task.subgroupIds().size());

        // Sala mult prea mare (pastram amfiteatrele pentru cursuri mari)
        p += Math.max(0, room.capacity() - task.audienceSize()) / 60.0;
        return p;
    }

    /** Explica de ce o activitate nu a putut fi plasata (pentru raportul catre admin). */
    private String explainFailure(Task task) {
        if (roomsFor(task).isEmpty()) {
            return "Nu exista nicio sala potrivita (tip/capacitate) pentru " + task.audienceSize() + " studenti.";
        }
        Set<Long> unavailable = input.unavailableSlotsByProfessor().getOrDefault(task.professorId(), Set.of());
        if (unavailable.size() >= input.slots().size()) {
            return "Profesorul este indisponibil in toate sloturile.";
        }
        return "Toate sloturile compatibile sunt ocupate (profesor, sala sau studenti).";
    }

    // ------------------------------------------------------------------
    // Starea curenta: ce resurse sunt ocupate, in ce slot si paritate
    // ------------------------------------------------------------------

    private class State {
        private final Set<String> occupied = new HashSet<>();
        private final Map<Long, Map<DayOfWeek, Set<Integer>>> subgroupHours = new HashMap<>();

        boolean isFree(Task task, long slotId, WeekParity parity) {
            if (!free("P" + task.professorId(), slotId, parity)) return false;
            for (Long sg : task.subgroupIds()) {
                if (!free("S" + sg, slotId, parity)) return false;
            }
            return true;
        }

        boolean isRoomFree(long roomId, long slotId, WeekParity parity) {
            return free("R" + roomId, slotId, parity);
        }

        /** O activitate saptamanala (ALL) ocupa atat saptamanile impare cat si cele pare. */
        private boolean free(String resource, long slotId, WeekParity parity) {
            for (WeekParity p : expand(parity)) {
                if (occupied.contains(resource + ":" + slotId + ":" + p)) return false;
            }
            return true;
        }

        void occupy(Task task, Slot slot, long roomId, WeekParity parity) {
            for (WeekParity p : expand(parity)) {
                occupied.add("P" + task.professorId() + ":" + slot.id() + ":" + p);
                occupied.add("R" + roomId + ":" + slot.id() + ":" + p);
                for (Long sg : task.subgroupIds()) {
                    occupied.add("S" + sg + ":" + slot.id() + ":" + p);
                }
            }
            for (Long sg : task.subgroupIds()) {
                subgroupHours.computeIfAbsent(sg, k -> new EnumMap<>(DayOfWeek.class))
                        .computeIfAbsent(slot.day(), k -> new HashSet<>())
                        .add(slot.startHour());
            }
        }

        Set<Integer> hoursOf(Long subgroupId, DayOfWeek day) {
            return subgroupHours.getOrDefault(subgroupId, Map.of()).getOrDefault(day, Set.of());
        }

        private List<WeekParity> expand(WeekParity parity) {
            return parity == WeekParity.ALL ? List.of(WeekParity.ODD, WeekParity.EVEN) : List.of(parity);
        }
    }
}
