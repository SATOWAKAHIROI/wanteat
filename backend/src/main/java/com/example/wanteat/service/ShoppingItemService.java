package com.example.wanteat.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.wanteat.domain.ShoppingItem;
import com.example.wanteat.dto.ShoppingItemRequest;
import com.example.wanteat.dto.ShoppingItemResponse;
import com.example.wanteat.exception.NotFoundException;
import com.example.wanteat.mapper.ShoppingItemMapper;
import com.example.wanteat.repository.ShoppingItemRepository;
import com.example.wanteat.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ShoppingItemService {

    private final ShoppingItemRepository shoppingItemRepository;
    private final UserRepository userRepository;
    private final ShoppingItemMapper shoppingItemMapper;

    @Transactional(readOnly = true)
    public List<ShoppingItemResponse> findAll(Long userId) {
        return shoppingItemRepository.findByUserIdOrderBySortOrderAscIdAsc(userId).stream()
                .map(shoppingItemMapper::toResponse)
                .toList();
    }

    /** 献立に紐づかない手動追加。 */
    @Transactional
    public ShoppingItemResponse create(Long userId, ShoppingItemRequest request) {
        ShoppingItem item = new ShoppingItem();
        item.setUser(userRepository.getReferenceById(userId));
        item.setMenuId(null);
        item.setName(request.name());
        item.setAmount(request.amount());
        item.setUnit(request.unit());
        item.setChecked(false);
        item.setSortOrder((int) shoppingItemRepository.countByUserId(userId) + 1);
        return shoppingItemMapper.toResponse(shoppingItemRepository.save(item));
    }

    /** チェック状態を反転する。 */
    @Transactional
    public ShoppingItemResponse toggleChecked(Long userId, Long id) {
        ShoppingItem item = shoppingItemRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NotFoundException("アイテムが見つかりません"));
        item.setChecked(!item.isChecked());
        return shoppingItemMapper.toResponse(shoppingItemRepository.save(item));
    }

    @Transactional
    public void delete(Long userId, Long id) {
        ShoppingItem item = shoppingItemRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NotFoundException("アイテムが見つかりません"));
        shoppingItemRepository.delete(item);
    }

    /** 購入済みのアイテムをまとめて削除する。 */
    @Transactional
    public void deleteChecked(Long userId) {
        shoppingItemRepository.deleteByUserIdAndCheckedTrue(userId);
    }
}
