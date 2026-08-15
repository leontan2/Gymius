package com.gymius.dto;

import java.time.LocalDate;

public record DashboardInsightDto(
        int weeklyGoal,
        int weeklyProgressPercent,
        long workoutsRemaining,
        long trainingStreakWeeks,
        Long daysSinceLastWorkout,
        LocalDate lastWorkoutDate,
        String topExerciseName,
        long topExerciseLogCount,
        String guidanceTitle,
        String guidanceBody
) {
}
