package com.gymius.dto;

import java.util.Map;

public record ApiErrorDto(
        String message,
        Map<String, String> fieldErrors
) {
}
