package com.example.wanteat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record IngredientRequest(
        @NotBlank(message = "食材名は必須です") @Size(max = 100, message = "食材名は100文字以内で入力してください") String name,
        @Size(max = 50, message = "分量は50文字以内で入力してください") String amount,
        @Size(max = 20, message = "単位は20文字以内で入力してください") String unit,
        /**
         * 常備品。true なら買い物リストへ展開しない。
         * 省略された場合は false 扱い。Jackson 3 は record の primitive boolean に
         * null を入れられないため、ラッパー型で受ける。
         */
        Boolean pantryStaple) {
}
