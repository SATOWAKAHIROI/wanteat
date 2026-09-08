package com.example.wanteat.dto;

import java.util.List;

/** AI が第2段階で返す1品分の詳細。 */
public record GeneratedDish(
        List<GeneratedIngredient> ingredients,
        List<String> steps) {
}
