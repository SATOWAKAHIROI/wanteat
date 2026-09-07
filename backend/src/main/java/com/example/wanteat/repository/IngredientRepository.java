package com.example.wanteat.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.wanteat.domain.Ingredient;

public interface IngredientRepository extends JpaRepository<Ingredient, Long> {
    
}
