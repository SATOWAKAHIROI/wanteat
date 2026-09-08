package com.example.wanteat.dto;

import com.example.wanteat.domain.RequestedBy;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RequestCreateRequest(
        @NotNull(message = "リクエスト者は必須です") RequestedBy requestedBy,
        @NotBlank(message = "内容は必須です") @Size(max = 255, message = "内容は255文字以内で入力してください") String body) {
}
