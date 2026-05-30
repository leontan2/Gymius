package com.gymius.controller;

import com.gymius.domain.UserAccount;
import com.gymius.dto.ExerciseLogDto;
import com.gymius.dto.ExerciseLogRequest;
import com.gymius.service.UserAccountService;
import com.gymius.service.WorkoutService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/workouts/{workoutId}/exercises")
public class ExerciseController {

    private final WorkoutService workoutService;
    private final UserAccountService userAccountService;

    public ExerciseController(WorkoutService workoutService, UserAccountService userAccountService) {
        this.workoutService = workoutService;
        this.userAccountService = userAccountService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExerciseLogDto add(
            @AuthenticationPrincipal OidcUser oidcUser,
            @PathVariable UUID workoutId,
            @Valid @RequestBody ExerciseLogRequest request
    ) {
        return workoutService.addExercise(currentUser(oidcUser), workoutId, request);
    }

    @PutMapping("/{exerciseId}")
    public ExerciseLogDto update(
            @AuthenticationPrincipal OidcUser oidcUser,
            @PathVariable UUID workoutId,
            @PathVariable UUID exerciseId,
            @Valid @RequestBody ExerciseLogRequest request
    ) {
        return workoutService.updateExercise(currentUser(oidcUser), workoutId, exerciseId, request);
    }

    @DeleteMapping("/{exerciseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal OidcUser oidcUser,
            @PathVariable UUID workoutId,
            @PathVariable UUID exerciseId
    ) {
        workoutService.deleteExercise(currentUser(oidcUser), workoutId, exerciseId);
    }

    private UserAccount currentUser(OidcUser oidcUser) {
        return userAccountService.syncFromGoogle(oidcUser);
    }
}
