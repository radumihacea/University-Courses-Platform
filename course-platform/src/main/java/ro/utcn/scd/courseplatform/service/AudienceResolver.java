package ro.utcn.scd.courseplatform.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ro.utcn.scd.courseplatform.model.Subgroup;
import ro.utcn.scd.courseplatform.model.TeachingAssignment;
import ro.utcn.scd.courseplatform.repository.SubgroupRepository;

import java.util.*;

/**
 * Determina "publicul" unei activitati ca multime de semigrupe:
 * curs -> toate semigrupele seriei, seminar -> semigrupele grupei, laborator/proiect -> semigrupa.
 * Doua activitati se suprapun pentru studenti daca multimile lor au elemente comune.
 */
@Component
@RequiredArgsConstructor
public class AudienceResolver {

    private final SubgroupRepository subgroupRepository;

    public record Audience(Set<Long> subgroupIds, int size) {
    }

    /** Creeaza un "rezolvator" care incarca semigrupele o singura data. Se foloseste intr-o tranzactie. */
    public Resolver create() {
        return new Resolver(subgroupRepository.findAll());
    }

    public static class Resolver {
        private final Map<Long, List<Subgroup>> bySeries = new HashMap<>();
        private final Map<Long, List<Subgroup>> byGroup = new HashMap<>();
        private final Map<Long, Subgroup> byId = new HashMap<>();

        Resolver(List<Subgroup> subgroups) {
            for (Subgroup s : subgroups) {
                byId.put(s.getId(), s);
                byGroup.computeIfAbsent(s.getGroup().getId(), k -> new ArrayList<>()).add(s);
                bySeries.computeIfAbsent(s.getGroup().getSeries().getId(), k -> new ArrayList<>()).add(s);
            }
        }

        public Audience of(TeachingAssignment a) {
            List<Subgroup> list;
            if (a.getSeries() != null) {
                list = bySeries.getOrDefault(a.getSeries().getId(), List.of());
            } else if (a.getGroup() != null) {
                list = byGroup.getOrDefault(a.getGroup().getId(), List.of());
            } else if (a.getSubgroup() != null) {
                Subgroup s = byId.get(a.getSubgroup().getId());
                list = s != null ? List.of(s) : List.of();
            } else {
                list = List.of();
            }
            Set<Long> ids = new LinkedHashSet<>();
            int size = 0;
            for (Subgroup s : list) {
                ids.add(s.getId());
                size += s.getStudentCount();
            }
            return new Audience(ids, size);
        }
    }
}
