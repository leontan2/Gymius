package com.gymius.mapper;

import com.gymius.domain.DailyNutritionGoal;
import com.gymius.domain.NutritionEntry;
import com.gymius.dto.NutritionEntryDto;
import com.gymius.dto.NutritionGoalDto;
import org.springframework.stereotype.Component;

@Component
public class NutritionMapper {

    public NutritionEntryDto toDto(NutritionEntry entry) {
        return new NutritionEntryDto(
                entry.getId(),
                entry.getEntryDate(),
                entry.getMealTime(),
                entry.getSource(),
                entry.getFoodItems(),
                entry.getCalories(),
                entry.getCalorieMin(),
                entry.getCalorieMax(),
                entry.getProteinGrams(),
                entry.getCarbsGrams(),
                entry.getFatGrams(),
                entry.getConfidence(),
                entry.getNotes(),
                entry.getCreatedAt(),
                entry.getUpdatedAt()
        );
    }

    public NutritionGoalDto toDto(DailyNutritionGoal goal) {
        return new NutritionGoalDto(
                goal.getDailyCalories(),
                goal.getProteinGoalGrams(),
                goal.getCarbsGoalGrams(),
                goal.getFatGoalGrams()
        );
    }
}
