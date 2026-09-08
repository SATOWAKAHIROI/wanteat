package com.example.wanteat.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PreferenceRequest(
        @NotNull(message = "世帯人数は必須です") @Min(value = 1, message = "世帯人数は1以上で入力してください") @Max(value = 20, message = "世帯人数は20以下で入力してください") Integer householdSize,
        @Size(max = 255, message = "アレルギーは255文字以内で入力してください") String allergies,
        @Size(max = 255, message = "苦手な食材は255文字以内で入力してください") String dislikedFoods,
        @Size(max = 1024, message = "備考は1024文字以内で入力してください") String note) {
}
