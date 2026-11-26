package ro.utcn.scd.courseplatform.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

/**
 * Un slot in care o ora poate fi mutata fara conflicte (profesorul si studentii sunt liberi),
 * impreuna cu salile potrivite si libere in acel slot.
 * preferred = profesorul a marcat slotul ca preferat.
 */
public record MoveOptionDto(
        Long timeSlotId,
        String label,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        boolean preferred,
        List<RoomDto> rooms
) {
}
