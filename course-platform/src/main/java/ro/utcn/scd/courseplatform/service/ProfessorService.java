package ro.utcn.scd.courseplatform.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.utcn.scd.courseplatform.dto.ProfessorDto;
import ro.utcn.scd.courseplatform.exception.BadRequestException;
import ro.utcn.scd.courseplatform.model.Professor;
import ro.utcn.scd.courseplatform.model.User;
import ro.utcn.scd.courseplatform.model.enums.Role;
import ro.utcn.scd.courseplatform.repository.ProfessorRepository;
import ro.utcn.scd.courseplatform.repository.UserRepository;

import java.util.Comparator;
import java.util.List;

/** CRUD pe profesori. Fiecare profesor are un cont de utilizator cu rolul PROFESSOR. */
@Service
@RequiredArgsConstructor
@Transactional
public class ProfessorService {

    private final ProfessorRepository repository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EntityLookup lookup;

    @Transactional(readOnly = true)
    public List<ProfessorDto> findAll() {
        return repository.findAll().stream()
                .map(ProfessorDto::from)
                .sorted(Comparator.comparing(ProfessorDto::fullName))
                .toList();
    }

    @Transactional(readOnly = true)
    public ProfessorDto findById(Long id) {
        return ProfessorDto.from(lookup.professor(id));
    }

    public ProfessorDto create(ProfessorDto dto) {
        if (dto.password() == null || dto.password().isBlank()) {
            throw new BadRequestException("Parola este obligatorie la crearea unui profesor.");
        }
        User user = new User();
        user.setRole(Role.PROFESSOR);
        applyUser(user, dto.fullName(), dto.email(), dto.password());

        Professor p = new Professor();
        p.setUser(userRepository.saveAndFlush(user));
        applyProfessor(p, dto);
        return ProfessorDto.from(repository.saveAndFlush(p));
    }

    public ProfessorDto update(Long id, ProfessorDto dto) {
        Professor p = lookup.professor(id);
        applyUser(p.getUser(), dto.fullName(), dto.email(), dto.password());
        applyProfessor(p, dto);
        return ProfessorDto.from(repository.saveAndFlush(p));
    }

    /** Sterge profesorul si contul lui. Esueaza (409) daca profesorul are asignari didactice. */
    public void delete(Long id) {
        Professor p = lookup.professor(id);
        User user = p.getUser();
        repository.delete(p);
        repository.flush();
        userRepository.delete(user);
        userRepository.flush();
    }

    private void applyUser(User user, String fullName, String email, String rawPassword) {
        user.setFullName(fullName);
        user.setEmail(email.trim().toLowerCase());
        if (rawPassword != null && !rawPassword.isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(rawPassword));
        }
    }

    private void applyProfessor(Professor p, ProfessorDto dto) {
        p.setTitle(dto.title());
        p.setDepartment(dto.department());
    }
}
