package com.gymius.service;

import com.gymius.domain.NutritionConfidence;
import com.gymius.dto.MealAnalysisDto;
import com.gymius.dto.MealFoodItemDto;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

@Component
public class MockMealVisionClient implements MealVisionClient {

    @Override
    public MealAnalysisDto analyze(MultipartFile image) {
        return new MealAnalysisDto(
                450,
                400,
                520,
                NutritionConfidence.MEDIUM,
                List.of(
                        new MealFoodItemDto("Grilled chicken", "about one palm-sized serving", 220),
                        new MealFoodItemDto("Rice", "about one cup", 180),
                        new MealFoodItemDto("Vegetables", "small side portion", 50)
                ),
                BigDecimal.valueOf(38),
                BigDecimal.valueOf(46),
                BigDecimal.valueOf(12),
                "Mock estimate. Add OPENAI_API_KEY and set MEAL_VISION_PROVIDER=openai to use live analysis.",
                "Great snapshot! Your meal is estimated at ~450 kcal."
        );
    }
}
