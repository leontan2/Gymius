package com.gymius.dto;

import com.gymius.domain.NutritionConfidence;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record NutritionEntryDto(
        UUID id,
        LocalDate entryDate,
        Instant mealTime,
        String source,
        String foodItems,
        Integer calories,
        Integer calorieMin,
        Integer calorieMax,
        BigDecimal proteinGrams,
        BigDecimal carbsGrams,
        BigDecimal fatGrams,
        NutritionConfidence confidence,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
}
