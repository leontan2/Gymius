package com.gymius.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record NutritionTodayDto(
        LocalDate date,
        NutritionGoalDto goal,
        Integer caloriesConsumed,
        Integer remainingCalories,
        BigDecimal proteinGrams,
        BigDecimal carbsGrams,
        BigDecimal fatGrams,
        List<NutritionEntryDto> entries
) {
}
