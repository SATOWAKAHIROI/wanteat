package com.example.wanteat.dto;

import java.time.LocalDateTime;

public record PreferenceResponse(
        Long id,
        int householdSize,
        String allergies,
        String dislikedFoods,
        String note,
        LocalDateTime updatedAt) {
}
