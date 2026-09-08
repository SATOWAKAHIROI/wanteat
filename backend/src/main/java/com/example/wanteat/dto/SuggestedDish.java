package com.example.wanteat.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** AI が第1段階で返す1品分の提案。分量と手順はまだ含まない。 */
public record SuggestedDish(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 255) String description,
        Integer cookingMinutes,
        List<String> mainIngredients) {
}
