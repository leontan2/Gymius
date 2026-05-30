package com.gymius.service;

import com.gymius.domain.DailyNutritionGoal;
import com.gymius.domain.NutritionConfidence;
import com.gymius.domain.NutritionEntry;
import com.gymius.domain.UserAccount;
import com.gymius.dto.MealAnalysisDto;
import com.gymius.dto.MealFoodItemDto;
import com.gymius.dto.NutritionEntryDto;
import com.gymius.dto.NutritionEntryRequest;
import com.gymius.dto.NutritionGoalDto;
import com.gymius.dto.NutritionGoalRequest;
import com.gymius.dto.NutritionTodayDto;
import com.gymius.mapper.NutritionMapper;
import com.gymius.repository.DailyNutritionGoalRepository;
import com.gymius.repository.NutritionEntryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@Service
public class NutritionService {

    private static final List<String> SUPPORTED_IMAGE_TYPES = List.of("image/jpeg", "image/png", "image/webp");

    private final NutritionEntryRepository nutritionEntryRepository;
    private final DailyNutritionGoalRepository dailyNutritionGoalRepository;
    private final NutritionMapper nutritionMapper;
    private final OpenAiMealVisionClient openAiMealVisionClient;
    private final MockMealVisionClient mockMealVisionClient;
    private final String mealVisionProvider;
    private final long maxImageBytes;
    private final int defaultDailyCalories;

    public NutritionService(
            NutritionEntryRepository nutritionEntryRepository,
            DailyNutritionGoalRepository dailyNutritionGoalRepository,
            NutritionMapper nutritionMapper,
            OpenAiMealVisionClient openAiMealVisionClient,
            MockMealVisionClient mockMealVisionClient,
            @Value("${app.meal-vision.provider:mock}") String mealVisionProvider,
            @Value("${app.meal-vision.max-image-bytes:5242880}") long maxImageBytes,
            @Value("${app.nutrition.default-daily-calories:2200}") int defaultDailyCalories
    ) {
        this.nutritionEntryRepository = nutritionEntryRepository;
        this.dailyNutritionGoalRepository = dailyNutritionGoalRepository;
        this.nutritionMapper = nutritionMapper;
        this.openAiMealVisionClient = openAiMealVisionClient;
        this.mockMealVisionClient = mockMealVisionClient;
        this.mealVisionProvider = mealVisionProvider;
        this.maxImageBytes = maxImageBytes;
        this.defaultDailyCalories = defaultDailyCalories;
    }

    @Transactional(readOnly = true)
    public NutritionTodayDto today(UserAccount user) {
        LocalDate today = LocalDate.now();
        List<NutritionEntry> entries = nutritionEntryRepository.findByUserAndEntryDateOrderByMealTimeDescCreatedAtDesc(user, today);
        NutritionGoalDto goal = goalFor(user);

        Integer caloriesConsumed = entries.stream()
                .map(NutritionEntry::getCalories)
                .reduce(0, Integer::sum);

        BigDecimal protein = sum(entries, Macro.PROTEIN);
        BigDecimal carbs = sum(entries, Macro.CARBS);
        BigDecimal fat = sum(entries, Macro.FAT);

        return new NutritionTodayDto(
                today,
                goal,
                caloriesConsumed,
                Math.max(goal.dailyCalories() - caloriesConsumed, 0),
                protein,
                carbs,
                fat,
                entries.stream().map(nutritionMapper::toDto).toList()
        );
    }

    @Transactional
    public NutritionEntryDto createEntry(UserAccount user, NutritionEntryRequest request) {
        NutritionEntry entry = new NutritionEntry();
        entry.setUser(user);
        entry.setEntryDate(request.entryDate());
        entry.setMealTime(Instant.now());
        entry.setSource("AI_SCAN");
        entry.setFoodItems(request.foodItems().trim());
        entry.setCalories(request.calories());
        entry.setCalorieMin(request.calorieMin());
        entry.setCalorieMax(request.calorieMax());
        entry.setProteinGrams(request.proteinGrams());
        entry.setCarbsGrams(request.carbsGrams());
        entry.setFatGrams(request.fatGrams());
        entry.setConfidence(request.confidence() == null ? NutritionConfidence.MEDIUM : request.confidence());
        entry.setNotes(trimToNull(request.notes()));

        if (entry.getCalorieMin() != null && entry.getCalorieMax() != null && entry.getCalorieMin() > entry.getCalorieMax()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Minimum calories cannot be greater than maximum calories.");
        }

        return nutritionMapper.toDto(nutritionEntryRepository.save(entry));
    }

    @Transactional
    public NutritionGoalDto updateGoal(UserAccount user, NutritionGoalRequest request) {
        DailyNutritionGoal goal = dailyNutritionGoalRepository.findByUser(user)
                .orElseGet(() -> {
                    DailyNutritionGoal created = new DailyNutritionGoal();
                    created.setUser(user);
                    return created;
                });

        goal.setDailyCalories(request.dailyCalories());
        goal.setProteinGoalGrams(request.proteinGoalGrams());
        goal.setCarbsGoalGrams(request.carbsGoalGrams());
        goal.setFatGoalGrams(request.fatGoalGrams());

        return nutritionMapper.toDto(dailyNutritionGoalRepository.save(goal));
    }

    public MealAnalysisDto analyzeMealImage(UserAccount user, MultipartFile image) {
        validateImage(image);
        MealAnalysisDto analysis = chooseClient().analyze(image);
        return normalize(analysis);
    }

    private MealVisionClient chooseClient() {
        String provider = mealVisionProvider == null ? "mock" : mealVisionProvider.trim().toLowerCase(Locale.ROOT);
        if ("openai".equals(provider) && openAiMealVisionClient.isConfigured()) {
            return openAiMealVisionClient;
        }

        if ("openai".equals(provider) || "mock".equals(provider)) {
            return mockMealVisionClient;
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported meal vision provider.");
    }

    private void validateImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a meal photo first.");
        }

        if (image.getSize() > maxImageBytes) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Meal photo must be 5 MB or smaller.");
        }

        String contentType = image.getContentType();
        if (contentType == null || !SUPPORTED_IMAGE_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Meal photo must be a JPEG, PNG, or WebP image.");
        }
    }

    private MealAnalysisDto normalize(MealAnalysisDto analysis) {
        int estimatedCalories = clamp(analysis.estimatedCalories(), 0, 10000);
        int calorieMin = clamp(analysis.calorieMin(), 0, estimatedCalories);
        int calorieMax = clamp(analysis.calorieMax(), estimatedCalories, 10000);

        return new MealAnalysisDto(
                estimatedCalories,
                calorieMin,
                calorieMax,
                analysis.confidence() == null ? NutritionConfidence.MEDIUM : analysis.confidence(),
                analysis.foodItems().stream()
                        .map(this::normalizeFoodItem)
                        .toList(),
                nonNegative(analysis.proteinGrams()),
                nonNegative(analysis.carbsGrams()),
                nonNegative(analysis.fatGrams()),
                blankToDefault(analysis.confidenceNote(), "Estimate range: %d-%d kcal.".formatted(calorieMin, calorieMax)),
                blankToDefault(analysis.userMessage(), "Great snapshot! Your meal is estimated at ~%d kcal.".formatted(estimatedCalories))
        );
    }

    private MealFoodItemDto normalizeFoodItem(MealFoodItemDto item) {
        return new MealFoodItemDto(
                blankToDefault(item.name(), "Meal item"),
                blankToDefault(item.portionEstimate(), "visible serving"),
                clamp(item.estimatedCalories(), 0, 10000)
        );
    }

    private NutritionGoalDto goalFor(UserAccount user) {
        return dailyNutritionGoalRepository.findByUser(user)
                .map(nutritionMapper::toDto)
                .orElseGet(() -> new NutritionGoalDto(defaultDailyCalories, null, null, null));
    }

    private BigDecimal sum(List<NutritionEntry> entries, Macro macro) {
        return entries.stream()
                .map(entry -> switch (macro) {
                    case PROTEIN -> entry.getProteinGrams();
                    case CARBS -> entry.getCarbsGrams();
                    case FAT -> entry.getFatGrams();
                })
                .filter(value -> value != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal nonNegative(BigDecimal value) {
        if (value == null || value.signum() < 0) {
            return null;
        }

        return value;
    }

    private int clamp(Integer value, int min, int max) {
        int candidate = value == null ? min : value;
        return Math.min(Math.max(candidate, min), max);
    }

    private String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        return value.trim();
    }

    private enum Macro {
        PROTEIN,
        CARBS,
        FAT
    }
}
