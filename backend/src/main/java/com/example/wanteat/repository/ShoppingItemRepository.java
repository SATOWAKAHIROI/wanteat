package com.example.wanteat.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.wanteat.domain.ShoppingItem;

public interface ShoppingItemRepository extends JpaRepository<ShoppingItem, Long> {

    /** 買い物リストは献立由来と手動追加を区別せず、1つのリストとして扱う。 */
    List<ShoppingItem> findByUserIdOrderBySortOrderAscIdAsc(Long userId);

    Optional<ShoppingItem> findByIdAndUserId(Long id, Long userId);

    long deleteByUserIdAndCheckedTrue(Long userId);

    long countByUserId(Long userId);
}
