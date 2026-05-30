package com.gymius.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProgressPointDto(
        LocalDate date,
        BigDecimal maxWeight,
        BigDecimal volume
) {
}
