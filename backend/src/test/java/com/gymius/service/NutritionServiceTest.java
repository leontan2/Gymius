package com.gymius.service;

import com.gymius.domain.NutritionConfidence;
import com.gymius.domain.DailyNutritionGoal;
import com.gymius.domain.UserAccount;
import com.gymius.dto.MealAnalysisDto;
import com.gymius.dto.MealFoodItemDto;
import com.gymius.dto.NutritionGoalDto;
import com.gymius.dto.NutritionGoalRequest;
import com.gymius.mapper.NutritionMapper;
import com.gymius.repository.DailyNutritionGoalRepository;
import com.gymius.repository.NutritionEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NutritionServiceTest {

    @Mock
    private NutritionEntryRepository nutritionEntryRepository;

    @Mock
    private DailyNutritionGoalRepository dailyNutritionGoalRepository;

    @Mock
    private NutritionMapper nutritionMapper;

    @Mock
    private OpenAiMealVisionClient openAiMealVisionClient;

    @Mock
    private MockMealVisionClient mockMealVisionClient;

    @Mock
    private PlatformTransactionManager transactionManager;

    private NutritionService nutritionService;

    @BeforeEach
    void setUp() {
        nutritionService = new NutritionService(
                nutritionEntryRepository,
                dailyNutritionGoalRepository,
                nutritionMapper,
                openAiMealVisionClient,
                mockMealVisionClient,
                transactionManager,
                "openai",
                1024,
                2200
        );
    }

    @Test
    void rejectsFilesWhoseContentsDoNotMatchTheirDeclaredType() {
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "meal.jpg",
                "image/jpeg",
                "not really an image".getBytes()
        );

        assertThatThrownBy(() -> nutritionService.analyzeMealImage(image))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(exception.getReason()).contains("do not match");
                });

        verifyNoInteractions(openAiMealVisionClient, mockMealVisionClient);
    }

    @Test
    void boundsAndSanitizesProviderOutputBeforeReturningIt() {
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "meal.jpg",
                "image/jpeg",
                new byte[]{(byte) 0xff, (byte) 0xd8, (byte) 0xff, 0x00}
        );
        List<MealFoodItemDto> oversizedItems = Collections.nCopies(
                25,
                new MealFoodItemDto(" ", " ", 20_000)
        );
        when(openAiMealVisionClient.analyze(any())).thenReturn(new MealAnalysisDto(
                20_000,
                -10,
                30_000,
                null,
                oversizedItems,
                new BigDecimal("-1"),
                new BigDecimal("42.5"),
                null,
                "x".repeat(600),
                "y".repeat(400)
        ));

        MealAnalysisDto result = nutritionService.analyzeMealImage(image);

        assertThat(result.estimatedCalories()).isEqualTo(10_000);
        assertThat(result.calorieMin()).isZero();
        assertThat(result.calorieMax()).isEqualTo(10_000);
        assertThat(result.confidence()).isEqualTo(NutritionConfidence.MEDIUM);
        assertThat(result.foodItems()).hasSize(20);
        assertThat(result.foodItems().get(0).name()).isEqualTo("Meal item");
        assertThat(result.foodItems().get(0).estimatedCalories()).isEqualTo(10_000);
        assertThat(result.proteinGrams()).isNull();
        assertThat(result.carbsGrams()).isEqualByComparingTo("42.5");
        assertThat(result.confidenceNote()).hasSize(500);
        assertThat(result.userMessage()).hasSize(300);
    }

    @Test
    @SuppressWarnings("unchecked")
    void retriesAConcurrentFirstGoalWriteInAFreshTransaction() {
        UserAccount user = new UserAccount();
        DailyNutritionGoal winningGoal = new DailyNutritionGoal();
        winningGoal.setUser(user);
        NutritionGoalRequest request = new NutritionGoalRequest(2400, null, null, null);
        NutritionGoalDto expected = new NutritionGoalDto(2400, null, null, null);
        TransactionStatus transactionStatus = mock(TransactionStatus.class);

        when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);
        when(dailyNutritionGoalRepository.findByUser(user))
                .thenReturn(Optional.empty(), Optional.of(winningGoal));
        when(dailyNutritionGoalRepository.saveAndFlush(any(DailyNutritionGoal.class)))
                .thenThrow(new DataIntegrityViolationException("concurrent insert"))
                .thenReturn(winningGoal);
        when(nutritionMapper.toDto(winningGoal)).thenReturn(expected);

        assertThat(nutritionService.updateGoal(user, request)).isEqualTo(expected);
        verify(transactionManager, times(2)).getTransaction(any());
        verify(transactionManager).rollback(transactionStatus);
        verify(transactionManager).commit(transactionStatus);
    }
}
