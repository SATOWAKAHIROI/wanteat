package com.example.wanteat.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.wanteat.domain.ShoppingItem;

public interface ShoppingItemRepository extends JpaRepository<ShoppingItem, Long> {

    List<ShoppingItem> findByUserIdAndMenuId(Long userId, Long menuId);

    Optional<ShoppingItem> findByIdAndUserIdAndMenuId(Long id, Long userId, Long menuId);
}
