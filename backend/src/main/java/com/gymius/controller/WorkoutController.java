package com.gymius.controller;

import com.gymius.domain.UserAccount;
import com.gymius.dto.WorkoutDto;
import com.gymius.dto.WorkoutRequest;
import com.gymius.service.UserAccountService;
import com.gymius.service.WorkoutService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/workouts")
public class WorkoutController {

    private final WorkoutService workoutService;
    private final UserAccountService userAccountService;

    public WorkoutController(WorkoutService workoutService, UserAccountService userAccountService) {
        this.workoutService = workoutService;
        this.userAccountService = userAccountService;
    }

    @GetMapping
    public List<WorkoutDto> list(@AuthenticationPrincipal OidcUser oidcUser) {
        return workoutService.list(currentUser(oidcUser));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkoutDto create(
            @AuthenticationPrincipal OidcUser oidcUser,
            @Valid @RequestBody WorkoutRequest request
    ) {
        return workoutService.create(currentUser(oidcUser), request);
    }

    @GetMapping("/{workoutId}")
    public WorkoutDto get(@AuthenticationPrincipal OidcUser oidcUser, @PathVariable UUID workoutId) {
        return workoutService.get(currentUser(oidcUser), workoutId);
    }

    @PutMapping("/{workoutId}")
    public WorkoutDto update(
            @AuthenticationPrincipal OidcUser oidcUser,
            @PathVariable UUID workoutId,
            @Valid @RequestBody WorkoutRequest request
    ) {
        return workoutService.update(currentUser(oidcUser), workoutId, request);
    }

    @DeleteMapping("/{workoutId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal OidcUser oidcUser, @PathVariable UUID workoutId) {
        workoutService.delete(currentUser(oidcUser), workoutId);
    }

    private UserAccount currentUser(OidcUser oidcUser) {
        return userAccountService.syncFromGoogle(oidcUser);
    }
}
