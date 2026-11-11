package ro.utcn.scd.courseplatform.dto;

/** Gradul de ocupare al unei sali: sloturi ocupate din cele disponibile pe saptamana. */
public record RoomOccupancyDto(
        Long roomId,
        String roomCode,
        int capacity,
        long activities,
        double occupiedSlotsPerWeek,
        int availableSlotsPerWeek,
        double occupancyPercent
) {
}
