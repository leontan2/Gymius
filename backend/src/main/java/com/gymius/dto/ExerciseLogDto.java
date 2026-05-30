package com.gymius.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ExerciseLogDto(
        UUID id,
        String exerciseName,
        Integer sets,
        Integer reps,
        BigDecimal weight,
        String notes,
        Integer sortOrder,
        BigDecimal volume
) {
}
