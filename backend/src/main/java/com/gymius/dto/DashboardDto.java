package com.gymius.dto;

import java.math.BigDecimal;
import java.util.List;

public record DashboardDto(
        List<WorkoutSummaryDto> recentWorkouts,
        long weeklyWorkoutCount,
        long totalWorkouts,
        BigDecimal totalVolumeLifted
) {
}
