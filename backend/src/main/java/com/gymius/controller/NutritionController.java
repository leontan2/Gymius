package com.gymius.controller;

import com.gymius.domain.UserAccount;
import com.gymius.dto.MealAnalysisDto;
import com.gymius.dto.NutritionEntryDto;
import com.gymius.dto.NutritionEntryRequest;
import com.gymius.dto.NutritionGoalDto;
import com.gymius.dto.NutritionGoalRequest;
import com.gymius.dto.NutritionTodayDto;
import com.gymius.service.NutritionService;
import com.gymius.service.UserAccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/nutrition")
public class NutritionController {

    private final NutritionService nutritionService;
    private final UserAccountService userAccountService;

    public NutritionController(NutritionService nutritionService, UserAccountService userAccountService) {
        this.nutritionService = nutritionService;
        this.userAccountService = userAccountService;
    }

    @GetMapping("/today")
    public NutritionTodayDto today(@AuthenticationPrincipal OidcUser oidcUser) {
        return nutritionService.today(currentUser(oidcUser));
    }

    @PostMapping("/analyze-image")
    public MealAnalysisDto analyzeImage(
            @AuthenticationPrincipal OidcUser oidcUser,
            @RequestParam("image") MultipartFile image
    ) {
        return nutritionService.analyzeMealImage(currentUser(oidcUser), image);
    }

    @PostMapping("/entries")
    @ResponseStatus(HttpStatus.CREATED)
    public NutritionEntryDto createEntry(
            @AuthenticationPrincipal OidcUser oidcUser,
            @Valid @RequestBody NutritionEntryRequest request
    ) {
        return nutritionService.createEntry(currentUser(oidcUser), request);
    }

    @PutMapping("/goals")
    public NutritionGoalDto updateGoal(
            @AuthenticationPrincipal OidcUser oidcUser,
            @Valid @RequestBody NutritionGoalRequest request
    ) {
        return nutritionService.updateGoal(currentUser(oidcUser), request);
    }

    private UserAccount currentUser(OidcUser oidcUser) {
        return userAccountService.syncFromGoogle(oidcUser);
    }
}
