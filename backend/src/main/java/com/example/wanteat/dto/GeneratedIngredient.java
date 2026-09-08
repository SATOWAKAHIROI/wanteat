package com.example.wanteat.dto;

/** AI が第2段階で返す材料。 */
public record GeneratedIngredient(
        String name,
        String amount,
        String unit,
        /**
         * 水・塩・油など家に常備しているもの。買い物リストへは展開しない。
         * AI が省略しても落ちないよう、primitive ではなくラッパー型で受ける。
         */
        Boolean pantryStaple) {
}
