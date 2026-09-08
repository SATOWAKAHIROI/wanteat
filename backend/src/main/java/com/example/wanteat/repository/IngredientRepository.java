package com.example.wanteat.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.wanteat.domain.Ingredient;

public interface IngredientRepository extends JpaRepository<Ingredient, Long> {

    List<Ingredient> findByDishIdOrderBySortOrderAscIdAsc(Long dishId);
}
