package com.example.wanteat.dto;

/** 第1段階の提案結果。主菜と副菜を1品ずつ返す。 */
public record SuggestResponse(
        SuggestedDish mainDish,
        SuggestedDish sideDish) {
}
