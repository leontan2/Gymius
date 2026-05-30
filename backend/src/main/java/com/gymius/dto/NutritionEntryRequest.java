package com.gymius.dto;

import com.gymius.domain.NutritionConfidence;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record NutritionEntryRequest(
        @NotNull(message = "Entry date is required.")
        @PastOrPresent(message = "Entry date cannot be in the future.")
        LocalDate entryDate,

        @NotBlank(message = "Food items are required.")
        @Size(max = 1000, message = "Food items must be 1000 characters or less.")
        String foodItems,

        @NotNull(message = "Calories are required.")
        @Min(value = 0, message = "Calories cannot be negative.")
        @Max(value = 10000, message = "Calories must be 10000 or less.")
        Integer calories,

        @Min(value = 0, message = "Minimum calories cannot be negative.")
        @Max(value = 10000, message = "Minimum calories must be 10000 or less.")
        Integer calorieMin,

        @Min(value = 0, message = "Maximum calories cannot be negative.")
        @Max(value = 10000, message = "Maximum calories must be 10000 or less.")
        Integer calorieMax,

        @DecimalMin(value = "0.0", message = "Protein cannot be negative.")
        BigDecimal proteinGrams,

        @DecimalMin(value = "0.0", message = "Carbs cannot be negative.")
        BigDecimal carbsGrams,

        @DecimalMin(value = "0.0", message = "Fat cannot be negative.")
        BigDecimal fatGrams,

        NutritionConfidence confidence,

        @Size(max = 1000, message = "Notes must be 1000 characters or less.")
        String notes
) {
}
