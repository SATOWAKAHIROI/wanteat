package com.example.wanteat.dto;

import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/** 提案時に利用者が任意で指定できる条件。 */
public record SuggestRequest(
        @Min(value = 5, message = "調理時間は5分以上で指定してください") @Max(value = 180, message = "調理時間は180分以下で指定してください") Integer maxCookingMinutes,
        @Size(max = 255, message = "使いたい食材は255文字以内で入力してください") String ingredients,
        @Size(max = 255, message = "気分・要望は255文字以内で入力してください") String mood,
        /** 「別の案」で却下された料理名。同じものを再提案させないために送る。 */
        List<String> excludeDishNames) {
}
