package com.gymius.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ExerciseLogRequest(
        @NotBlank(message = "Exercise name is required.")
        @Size(max = 120, message = "Exercise name must be 120 characters or less.")
        String exerciseName,

        @NotNull(message = "Sets are required.")
        @Min(value = 1, message = "Sets must be at least 1.")
        @Max(value = 100, message = "Sets must be 100 or less.")
        Integer sets,

        @NotNull(message = "Reps are required.")
        @Min(value = 1, message = "Reps must be at least 1.")
        @Max(value = 1000, message = "Reps must be 1000 or less.")
        Integer reps,

        @NotNull(message = "Weight is required.")
        @DecimalMin(value = "0.0", message = "Weight cannot be negative.")
        @Digits(integer = 6, fraction = 2, message = "Weight must be a valid number.")
        BigDecimal weight,

        @Size(max = 500, message = "Notes must be 500 characters or less.")
        String notes
) {
}
