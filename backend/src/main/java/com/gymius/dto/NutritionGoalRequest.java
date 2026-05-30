package com.gymius.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record NutritionGoalRequest(
        @NotNull(message = "Daily calories are required.")
        @Min(value = 800, message = "Daily calories must be at least 800.")
        @Max(value = 8000, message = "Daily calories must be 8000 or less.")
        Integer dailyCalories,

        @DecimalMin(value = "0.0", message = "Protein goal cannot be negative.")
        BigDecimal proteinGoalGrams,

        @DecimalMin(value = "0.0", message = "Carbs goal cannot be negative.")
        BigDecimal carbsGoalGrams,

        @DecimalMin(value = "0.0", message = "Fat goal cannot be negative.")
        BigDecimal fatGoalGrams
) {
}
