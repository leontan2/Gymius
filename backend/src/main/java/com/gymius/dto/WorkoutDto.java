package com.gymius.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record WorkoutDto(
        UUID id,
        LocalDate workoutDate,
        String notes,
        List<ExerciseLogDto> exercises,
        BigDecimal totalVolume,
        Instant createdAt,
        Instant updatedAt
) {
}
