package com.example.wanteat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ShoppingItemRequest(
        @NotBlank(message = "アイテム名は必須です") @Size(max = 100, message = "アイテム名は100字以内で入力してください") String name,
        @Size(max = 50, message = "分量は50文字以内で入力してください") String amount,
        @Size(max = 20, message = "単位は20文字以内で入力してください") String unit,
        @NotNull(message = "完了状態は必須です") Boolean checked,
        @NotNull(message = "並び順は必須です") Integer sortOrder

) {
}
