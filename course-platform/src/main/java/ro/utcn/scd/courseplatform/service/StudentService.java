package ro.utcn.scd.courseplatform.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.utcn.scd.courseplatform.dto.StudentDto;
import ro.utcn.scd.courseplatform.exception.BadRequestException;
import ro.utcn.scd.courseplatform.model.Student;
import ro.utcn.scd.courseplatform.model.User;
import ro.utcn.scd.courseplatform.model.enums.Role;
import ro.utcn.scd.courseplatform.repository.StudentRepository;
import ro.utcn.scd.courseplatform.repository.UserRepository;

import java.util.List;

/** CRUD pe studenti. Fiecare student are un cont de utilizator cu rolul STUDENT. */
@Service
@RequiredArgsConstructor
@Transactional
public class StudentService {

    private final StudentRepository repository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EntityLookup lookup;

    @Transactional(readOnly = true)
    public List<StudentDto> findAll(Long subgroupId) {
        List<Student> students = subgroupId != null
                ? repository.findBySubgroupIdOrderByRegistrationNumber(subgroupId)
                : repository.findAll(Sort.by("registrationNumber"));
        return students.stream().map(StudentDto::from).toList();
    }

    @Transactional(readOnly = true)
    public StudentDto findById(Long id) {
        return StudentDto.from(lookup.student(id));
    }

    public StudentDto create(StudentDto dto) {
        if (dto.password() == null || dto.password().isBlank()) {
            throw new BadRequestException("Parola este obligatorie la crearea unui student.");
        }
        User user = new User();
        user.setRole(Role.STUDENT);
        applyUser(user, dto);

        Student s = new Student();
        s.setUser(userRepository.saveAndFlush(user));
        applyStudent(s, dto);
        return StudentDto.from(repository.saveAndFlush(s));
    }

    public StudentDto update(Long id, StudentDto dto) {
        Student s = lookup.student(id);
        applyUser(s.getUser(), dto);
        applyStudent(s, dto);
        return StudentDto.from(repository.saveAndFlush(s));
    }

    /** Sterge studentul si contul lui. Esueaza (409) daca are inrolari la optionale. */
    public void delete(Long id) {
        Student s = lookup.student(id);
        User user = s.getUser();
        repository.delete(s);
        repository.flush();
        userRepository.delete(user);
        userRepository.flush();
    }

    private void applyUser(User user, StudentDto dto) {
        user.setFullName(dto.fullName());
        user.setEmail(dto.email().trim().toLowerCase());
        if (dto.password() != null && !dto.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(dto.password()));
        }
    }

    private void applyStudent(Student s, StudentDto dto) {
        s.setRegistrationNumber(dto.registrationNumber().trim());
        s.setSubgroup(lookup.subgroup(dto.subgroupId()));
    }
}
