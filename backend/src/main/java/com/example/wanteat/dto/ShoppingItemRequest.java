package com.example.wanteat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 買い物リストへの手動追加。チェック状態と並び順はサーバー側で決める。 */
public record ShoppingItemRequest(
        @NotBlank(message = "アイテム名は必須です") @Size(max = 100, message = "アイテム名は100文字以内で入力してください") String name,
        @Size(max = 50, message = "分量は50文字以内で入力してください") String amount,
        @Size(max = 20, message = "単位は20文字以内で入力してください") String unit) {
}
