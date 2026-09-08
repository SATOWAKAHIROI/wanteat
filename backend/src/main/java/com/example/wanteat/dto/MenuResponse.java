package com.example.wanteat.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.example.wanteat.domain.MenuStatus;

public record MenuResponse(
        Long id,
        LocalDate cookedOn,
        MenuStatus status,
        List<DishResponse> dishes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
