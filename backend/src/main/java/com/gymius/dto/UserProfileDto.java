package com.gymius.dto;

import java.util.UUID;

public record UserProfileDto(
        UUID id,
        String name,
        String email,
        String pictureUrl
) {
}
