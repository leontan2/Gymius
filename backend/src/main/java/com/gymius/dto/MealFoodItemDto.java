package com.gymius.dto;

public record MealFoodItemDto(
        String name,
        String portionEstimate,
        Integer estimatedCalories
) {
}
