package com.example.wanteat.dto;

public record IngredientResponse(
        Long id,
        String name,
        String amount,
        String unit,
        boolean pantryStaple,
        Integer sortOrder) {
}
