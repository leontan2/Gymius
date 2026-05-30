package com.gymius.dto;

import java.math.BigDecimal;

public record NutritionGoalDto(
        Integer dailyCalories,
        BigDecimal proteinGoalGrams,
        BigDecimal carbsGoalGrams,
        BigDecimal fatGoalGrams
) {
}
