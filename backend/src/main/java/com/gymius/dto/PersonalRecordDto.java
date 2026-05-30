package com.gymius.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PersonalRecordDto(
        String exerciseName,
        BigDecimal highestWeight,
        Integer repsAtHighestWeight,
        LocalDate highestWeightDate,
        UUID highestWeightWorkoutId,
        Integer highestReps,
        BigDecimal weightAtHighestReps,
        LocalDate highestRepsDate,
        UUID highestRepsWorkoutId
) {
}
