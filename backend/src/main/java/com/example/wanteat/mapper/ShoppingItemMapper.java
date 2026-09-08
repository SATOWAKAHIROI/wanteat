package com.example.wanteat.mapper;

import org.springframework.stereotype.Component;

import com.example.wanteat.domain.ShoppingItem;
import com.example.wanteat.dto.ShoppingItemResponse;

@Component
public class ShoppingItemMapper {

    public ShoppingItemResponse toResponse(ShoppingItem shoppingItem) {
        return new ShoppingItemResponse(
                shoppingItem.getId(),
                shoppingItem.getMenuId(),
                shoppingItem.getName(),
                shoppingItem.getAmount(),
                shoppingItem.getUnit(),
                shoppingItem.isChecked(),
                shoppingItem.getSortOrder());
    }
}
