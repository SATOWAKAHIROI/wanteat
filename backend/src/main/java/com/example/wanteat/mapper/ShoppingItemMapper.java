package com.example.wanteat.mapper;

import org.springframework.stereotype.Component;

import com.example.wanteat.domain.ShoppingItem;
import com.example.wanteat.dto.ShoppingItemResponse;

@Component
public class ShoppingItemMapper {
    public ShoppingItemResponse toShoppingItemResponse(ShoppingItem shoppingItem) {
        ShoppingItemResponse shoppingItemResponse = new ShoppingItemResponse(shoppingItem.getName(),
                shoppingItem.getAmount(), shoppingItem.getUnit(), shoppingItem.isChecked(),
                shoppingItem.getSortOrder());
        return shoppingItemResponse;
    }
}
