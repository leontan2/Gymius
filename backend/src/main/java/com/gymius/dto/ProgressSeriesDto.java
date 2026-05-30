package com.gymius.dto;

import java.util.List;

public record ProgressSeriesDto(
        String exerciseName,
        List<ProgressPointDto> points
) {
}
