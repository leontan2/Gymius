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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class NutritionService {

    private static final List<String> SUPPORTED_IMAGE_TYPES = List.of("image/jpeg", "image/png", "image/webp");
    private static final BigDecimal MAX_MACRO_GRAMS = new BigDecimal("10000.00");

    private final NutritionEntryRepository nutritionEntryRepository;
    private final DailyNutritionGoalRepository dailyNutritionGoalRepository;
    private final NutritionMapper nutritionMapper;
    private final OpenAiMealVisionClient openAiMealVisionClient;
    private final MockMealVisionClient mockMealVisionClient;
    private final TransactionTemplate transactionTemplate;
    private final String mealVisionProvider;
    private final long maxImageBytes;
    private final int defaultDailyCalories;

    public NutritionService(
            NutritionEntryRepository nutritionEntryRepository,
            DailyNutritionGoalRepository dailyNutritionGoalRepository,
            NutritionMapper nutritionMapper,
            OpenAiMealVisionClient openAiMealVisionClient,
            MockMealVisionClient mockMealVisionClient,
            PlatformTransactionManager transactionManager,
            @Value("${app.meal-vision.provider:mock}") String mealVisionProvider,
            @Value("${app.meal-vision.max-image-bytes:5242880}") long maxImageBytes,
            @Value("${app.nutrition.default-daily-calories:2200}") int defaultDailyCalories
    ) {
        this.nutritionEntryRepository = nutritionEntryRepository;
        this.dailyNutritionGoalRepository = dailyNutritionGoalRepository;
        this.nutritionMapper = nutritionMapper;
        this.openAiMealVisionClient = openAiMealVisionClient;
        this.mockMealVisionClient = mockMealVisionClient;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
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

    public NutritionGoalDto updateGoal(UserAccount user, NutritionGoalRequest request) {
        try {
            return inTransaction(() -> saveGoal(user, request));
        } catch (DataIntegrityViolationException conflict) {
            // Concurrent first writes can both observe no goal. The failed insert is
            // rolled back before updating the row created by the winning request.
            return inTransaction(() -> {
                DailyNutritionGoal goal = dailyNutritionGoalRepository.findByUser(user)
                        .orElseThrow(() -> conflict);
                return saveGoal(goal, request);
            });
        }
    }

    private NutritionGoalDto saveGoal(UserAccount user, NutritionGoalRequest request) {
        DailyNutritionGoal goal = dailyNutritionGoalRepository.findByUser(user)
                .orElseGet(() -> {
                    DailyNutritionGoal created = new DailyNutritionGoal();
                    created.setUser(user);
                    return created;
                });
        return saveGoal(goal, request);
    }

    private NutritionGoalDto saveGoal(DailyNutritionGoal goal, NutritionGoalRequest request) {
        goal.setDailyCalories(request.dailyCalories());
        goal.setProteinGoalGrams(request.proteinGoalGrams());
        goal.setCarbsGoalGrams(request.carbsGoalGrams());
        goal.setFatGoalGrams(request.fatGoalGrams());

        return nutritionMapper.toDto(dailyNutritionGoalRepository.saveAndFlush(goal));
    }

    public MealAnalysisDto analyzeMealImage(MultipartFile image) {
        validateImage(image);
        MealAnalysisDto analysis = chooseClient().analyze(image);

        if (analysis == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Meal analysis returned no result.");
        }

        return normalize(analysis);
    }

    private MealVisionClient chooseClient() {
        String provider = mealVisionProvider == null ? "mock" : mealVisionProvider.trim().toLowerCase(Locale.ROOT);
        if ("openai".equals(provider)) {
            return openAiMealVisionClient;
        }

        if ("mock".equals(provider)) {
            return mockMealVisionClient;
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported meal vision provider.");
    }

    private void validateImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a meal photo first.");
        }

        if (image.getSize() > maxImageBytes) {
            long maxMegabytes = Math.max(1, maxImageBytes / (1024 * 1024));
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Meal photo must be %d MB or smaller.".formatted(maxMegabytes)
            );
        }

        String contentType = image.getContentType();
        String normalizedContentType = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        if (!SUPPORTED_IMAGE_TYPES.contains(normalizedContentType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Meal photo must be a JPEG, PNG, or WebP image.");
        }

        validateImageSignature(image, normalizedContentType);
    }

    private MealAnalysisDto normalize(MealAnalysisDto analysis) {
        int estimatedCalories = clamp(analysis.estimatedCalories(), 0, 10000);
        int calorieMin = clamp(analysis.calorieMin(), 0, estimatedCalories);
        int calorieMax = clamp(analysis.calorieMax(), estimatedCalories, 10000);
        List<MealFoodItemDto> foodItems = analysis.foodItems() == null ? List.of() : analysis.foodItems();

        return new MealAnalysisDto(
                estimatedCalories,
                calorieMin,
                calorieMax,
                analysis.confidence() == null ? NutritionConfidence.MEDIUM : analysis.confidence(),
                foodItems.stream()
                        .filter(item -> item != null)
                        .limit(20)
                        .map(this::normalizeFoodItem)
                        .toList(),
                normalizeMacro(analysis.proteinGrams()),
                normalizeMacro(analysis.carbsGrams()),
                normalizeMacro(analysis.fatGrams()),
                truncate(
                        blankToDefault(analysis.confidenceNote(), "Estimate range: %d-%d kcal.".formatted(calorieMin, calorieMax)),
                        500
                ),
                truncate(
                        blankToDefault(analysis.userMessage(), "Your meal is estimated at about %d kcal.".formatted(estimatedCalories)),
                        300
                )
        );
    }

    private MealFoodItemDto normalizeFoodItem(MealFoodItemDto item) {
        return new MealFoodItemDto(
                truncate(blankToDefault(item.name(), "Meal item"), 120),
                truncate(blankToDefault(item.portionEstimate(), "visible serving"), 200),
                clamp(item.estimatedCalories(), 0, 10000)
        );
    }

    private void validateImageSignature(MultipartFile image, String contentType) {
        try (InputStream inputStream = image.getInputStream()) {
            byte[] header = inputStream.readNBytes(12);
            boolean signatureMatches = switch (contentType) {
                case "image/jpeg" -> startsWith(header, 0xff, 0xd8, 0xff);
                case "image/png" -> startsWith(header, 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a);
                case "image/webp" -> startsWith(header, 0x52, 0x49, 0x46, 0x46)
                        && startsWithAt(header, 8, 0x57, 0x45, 0x42, 0x50);
                default -> false;
            };

            if (!signatureMatches) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Meal photo contents do not match its file type."
                );
            }
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Meal photo could not be read.", exception);
        }
    }

    private boolean startsWith(byte[] value, int... prefix) {
        return startsWithAt(value, 0, prefix);
    }

    private boolean startsWithAt(byte[] value, int offset, int... expected) {
        if (value.length < offset + expected.length) {
            return false;
        }

        for (int index = 0; index < expected.length; index++) {
            if (Byte.toUnsignedInt(value[offset + index]) != expected[index]) {
                return false;
            }
        }

        return true;
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

    private BigDecimal normalizeMacro(BigDecimal value) {
        if (value == null || value.signum() < 0) {
            return null;
        }

        return value.min(MAX_MACRO_GRAMS).setScale(2, RoundingMode.HALF_UP);
    }

    private int clamp(Integer value, int min, int max) {
        int candidate = value == null ? min : value;
        return Math.min(Math.max(candidate, min), max);
    }

    private String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        return value.trim();
    }

    private <T> T inTransaction(java.util.function.Supplier<T> callback) {
        return Objects.requireNonNull(transactionTemplate.execute(status -> callback.get()));
    }

    private enum Macro {
        PROTEIN,
        CARBS,
        FAT
    }
}
