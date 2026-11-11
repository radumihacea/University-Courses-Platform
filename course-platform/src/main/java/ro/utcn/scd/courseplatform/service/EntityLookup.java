package ro.utcn.scd.courseplatform.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ro.utcn.scd.courseplatform.exception.NotFoundException;
import ro.utcn.scd.courseplatform.model.*;
import ro.utcn.scd.courseplatform.repository.*;

/**
 * Incarca o entitate dupa id sau arunca NotFoundException (HTTP 404).
 * Folosita de toate serviciile, ca sa nu repetam acelasi cod.
 */
@Component
@RequiredArgsConstructor
public class EntityLookup {

    private final BuildingRepository buildingRepository;
    private final RoomRepository roomRepository;
    private final DisciplineRepository disciplineRepository;
    private final SeriesRepository seriesRepository;
    private final StudentGroupRepository groupRepository;
    private final SubgroupRepository subgroupRepository;
    private final ProfessorRepository professorRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final TeachingAssignmentRepository assignmentRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final ProfessorConstraintRepository constraintRepository;
    private final ScheduleEntryRepository scheduleEntryRepository;
    private final ChangeRequestRepository changeRequestRepository;
    private final NotificationRepository notificationRepository;

    public Building building(Long id) {
        return buildingRepository.findById(id).orElseThrow(() -> new NotFoundException("Cladirea", id));
    }

    public Room room(Long id) {
        return roomRepository.findById(id).orElseThrow(() -> new NotFoundException("Sala", id));
    }

    public Discipline discipline(Long id) {
        return disciplineRepository.findById(id).orElseThrow(() -> new NotFoundException("Disciplina", id));
    }

    public Series series(Long id) {
        return seriesRepository.findById(id).orElseThrow(() -> new NotFoundException("Seria", id));
    }

    public StudentGroup group(Long id) {
        return groupRepository.findById(id).orElseThrow(() -> new NotFoundException("Grupa", id));
    }

    public Subgroup subgroup(Long id) {
        return subgroupRepository.findById(id).orElseThrow(() -> new NotFoundException("Semigrupa", id));
    }

    public Professor professor(Long id) {
        return professorRepository.findById(id).orElseThrow(() -> new NotFoundException("Profesorul", id));
    }

    public Student student(Long id) {
        return studentRepository.findById(id).orElseThrow(() -> new NotFoundException("Studentul", id));
    }

    public User user(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new NotFoundException("Utilizatorul", id));
    }

    public TeachingAssignment assignment(Long id) {
        return assignmentRepository.findById(id).orElseThrow(() -> new NotFoundException("Asignarea didactica", id));
    }

    public TimeSlot timeSlot(Long id) {
        return timeSlotRepository.findById(id).orElseThrow(() -> new NotFoundException("Slotul orar", id));
    }

    public ProfessorConstraint constraint(Long id) {
        return constraintRepository.findById(id).orElseThrow(() -> new NotFoundException("Restrictia", id));
    }

    public ScheduleEntry scheduleEntry(Long id) {
        return scheduleEntryRepository.findById(id).orElseThrow(() -> new NotFoundException("Ora din orar", id));
    }

    public ChangeRequest changeRequest(Long id) {
        return changeRequestRepository.findById(id).orElseThrow(() -> new NotFoundException("Cererea de modificare", id));
    }

    public Notification notification(Long id) {
        return notificationRepository.findById(id).orElseThrow(() -> new NotFoundException("Notificarea", id));
    }
}
