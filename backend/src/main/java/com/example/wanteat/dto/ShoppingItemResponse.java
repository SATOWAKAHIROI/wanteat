package com.example.wanteat.dto;

public record ShoppingItemResponse(
        String name,
        String amount,
        String unit,
        Boolean checked,
        Integer sortOrder) {

}
