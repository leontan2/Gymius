package com.gymius.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record WorkoutRequest(
        @NotNull(message = "Workout date is required.")
        @PastOrPresent(message = "Workout date cannot be in the future.")
        LocalDate workoutDate,

        @Size(max = 1000, message = "Notes must be 1000 characters or less.")
        String notes,

        @NotEmpty(message = "Add at least one exercise.")
        List<@Valid ExerciseLogRequest> exercises
) {
}
