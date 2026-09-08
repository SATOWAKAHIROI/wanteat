package com.example.wanteat.dto;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** 第1段階の提案を受けて献立を確定する。材料と手順は AI が生成する。 */
public record MenuConfirmRequest(
        @NotNull(message = "対象日は必須です") LocalDate cookedOn,
        @NotNull(message = "主菜は必須です") @Valid SuggestedDish mainDish,
        @NotNull(message = "副菜は必須です") @Valid SuggestedDish sideDish,
        List<Long> fulfilledRequestIds) {
}
