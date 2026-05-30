package com.gymius.dto;

import com.gymius.domain.NutritionConfidence;

import java.math.BigDecimal;
import java.util.List;

public record MealAnalysisDto(
        Integer estimatedCalories,
        Integer calorieMin,
        Integer calorieMax,
        NutritionConfidence confidence,
        List<MealFoodItemDto> foodItems,
        BigDecimal proteinGrams,
        BigDecimal carbsGrams,
        BigDecimal fatGrams,
        String confidenceNote,
        String userMessage
) {
    public MealAnalysisDto {
        foodItems = foodItems == null ? List.of() : foodItems;
    }
}
