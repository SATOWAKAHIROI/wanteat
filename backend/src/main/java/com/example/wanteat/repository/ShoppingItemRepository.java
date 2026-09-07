package com.example.wanteat.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.wanteat.domain.ShoppingItem;

public interface ShoppingItemRepository extends JpaRepository<ShoppingItem, Long> {
    
}
