package com.example.wanteat.dto;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record MenuCreateRequest(
        @NotNull(message = "対象日は必須です") LocalDate cookedOn,
        @NotEmpty(message = "料理は1品以上必要です") @Valid List<DishRequest> dishes,
        /** この献立で消化したリクエストの ID。AI 提案が反映した際に指定する。 */
        List<Long> fulfilledRequestIds) {
}
