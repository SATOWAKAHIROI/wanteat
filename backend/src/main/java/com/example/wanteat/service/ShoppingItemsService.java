package com.example.wanteat.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.wanteat.domain.ShoppingItem;
import com.example.wanteat.domain.User;
import com.example.wanteat.dto.ShoppingItemRequest;
import com.example.wanteat.dto.ShoppingItemResponse;
import com.example.wanteat.exception.NotFoundException;
import com.example.wanteat.mapper.ShoppingItemMapper;
import com.example.wanteat.repository.ShoppingItemRepository;
import com.example.wanteat.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ShoppingItemsService {
    private final ShoppingItemRepository shoppingItemRepository;
    private final ShoppingItemMapper shoppingItemMapper;
    private final UserRepository userRepository;

    public List<ShoppingItemResponse> getShoppingItems(Long id, Long userId, Long menuId) {
        return shoppingItemRepository
                .findByUserIdAndMenuId(userId, menuId).stream().map(shoppingItemMapper::toShoppingItemResponse)
                .toList();
    }

    public ShoppingItemResponse createShoppingItem(Long id, Long userId, Long menuId,
            ShoppingItemRequest shoppingItemRequest) {
        User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("ユーザーが見つかりません"));
        ShoppingItem newShoppingItem = new ShoppingItem();
        newShoppingItem.setUser(user);
        newShoppingItem.setMenuId(menuId);
        newShoppingItem.setName(shoppingItemRequest.name());
        newShoppingItem.setAmount(shoppingItemRequest.amount());
        newShoppingItem.setUnit(shoppingItemRequest.unit());
        newShoppingItem.setChecked(shoppingItemRequest.checked());
        newShoppingItem.setSortOrder(shoppingItemRequest.sortOrder());
        ShoppingItem resultItem = shoppingItemRepository.save(newShoppingItem);
        return shoppingItemMapper.toShoppingItemResponse(resultItem);
    }

    public ShoppingItemResponse checkShoppingItem(Long id, Long userId, Long menuId) {
        ShoppingItem editShoppingItem = shoppingItemRepository.findByIdAndUserIdAndMenuId(id, userId, menuId)
                .orElseThrow(() -> new NotFoundException("アイテムが見つかりません"));

        editShoppingItem.setChecked(!editShoppingItem.isChecked());
        ShoppingItem resultItem = shoppingItemRepository.save(editShoppingItem);
        return shoppingItemMapper.toShoppingItemResponse(resultItem);
    }
}
