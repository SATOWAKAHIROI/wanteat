package com.example.wanteat.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.wanteat.domain.Dish;

public interface DishRepository extends JpaRepository<Dish, Long> {

    List<Dish> findByMenuIdOrderBySortOrderAscIdAsc(Long menuId);

    /**
     * 直近に作った料理名。AI へ渡して重複提案を避けるために使う。
     * 遅延ロードを避けるため、エンティティではなく名前だけを取得する。
     */
    @Query("""
            SELECT d.name FROM Dish d
            WHERE d.menu.user.id = :userId AND d.menu.cookedOn >= :from
            ORDER BY d.menu.cookedOn DESC
            """)
    List<String> findRecentDishNames(@Param("userId") Long userId, @Param("from") LocalDate from);
}
