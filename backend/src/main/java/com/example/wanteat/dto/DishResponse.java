package com.example.wanteat.dto;

import java.util.List;

import com.example.wanteat.domain.DishCategory;

public record DishResponse(
        Long id,
        DishCategory category,
        String name,
        String description,
        Integer cookingMinutes,
        List<String> steps,
        Integer sortOrder,
        List<IngredientResponse> ingredients) {
}
