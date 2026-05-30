package com.gymius.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record WorkoutSummaryDto(
        UUID id,
        LocalDate workoutDate,
        int exerciseCount,
        BigDecimal totalVolume
) {
}
