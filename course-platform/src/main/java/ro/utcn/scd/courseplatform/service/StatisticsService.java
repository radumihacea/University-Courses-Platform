package ro.utcn.scd.courseplatform.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.utcn.scd.courseplatform.dto.ProfessorLoadDto;
import ro.utcn.scd.courseplatform.dto.RoomOccupancyDto;
import ro.utcn.scd.courseplatform.repository.ScheduleEntryRepository;
import ro.utcn.scd.courseplatform.repository.TimeSlotRepository;

import java.util.Comparator;
import java.util.List;

/** OPERATII COMPLEXE: statistici calculate prin query-uri de agregare (GROUP BY). */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatisticsService {

    private final ScheduleEntryRepository entryRepository;
    private final TimeSlotRepository timeSlotRepository;

    /** Incarcarea profesorilor, de la cel mai incarcat la cel mai putin incarcat. */
    public List<ProfessorLoadDto> professorLoad(Long timetableId) {
        return entryRepository.professorLoad(timetableId).stream()
                .map(row -> new ProfessorLoadDto(
                        ((Number) row[0]).longValue(),
                        (String) row[1],
                        ((Number) row[2]).longValue(),
                        ((Number) row[3]).doubleValue()))
                .sorted(Comparator.comparingDouble(ProfessorLoadDto::weeklyHours).reversed())
                .toList();
    }

    /** Gradul de ocupare al salilor, de la cea mai ocupata la cea mai libera. */
    public List<RoomOccupancyDto> roomOccupancy(Long timetableId) {
        int slotsPerWeek = (int) timeSlotRepository.count();
        return entryRepository.roomOccupancy(timetableId).stream()
                .map(row -> {
                    double occupied = ((Number) row[4]).doubleValue();
                    double percent = slotsPerWeek == 0 ? 0 : Math.round(occupied * 1000.0 / slotsPerWeek) / 10.0;
                    return new RoomOccupancyDto(
                            ((Number) row[0]).longValue(),
                            (String) row[1],
                            ((Number) row[2]).intValue(),
                            ((Number) row[3]).longValue(),
                            occupied, slotsPerWeek, percent);
                })
                .sorted(Comparator.comparingDouble(RoomOccupancyDto::occupancyPercent).reversed())
                .toList();
    }
}
