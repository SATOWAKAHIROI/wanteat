package com.example.wanteat.dto;

import java.util.List;

import com.example.wanteat.domain.DishCategory;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DishRequest(
        @NotNull(message = "区分は必須です") DishCategory category,
        @NotBlank(message = "料理名は必須です") @Size(max = 100, message = "料理名は100文字以内で入力してください") String name,
        @Size(max = 255, message = "説明は255文字以内で入力してください") String description,
        @Min(value = 1, message = "調理時間は1分以上で入力してください") Integer cookingMinutes,
        @NotEmpty(message = "手順は1つ以上必要です") List<@NotBlank(message = "手順に空の項目は含められません") String> steps,
        @Valid List<IngredientRequest> ingredients) {
}
