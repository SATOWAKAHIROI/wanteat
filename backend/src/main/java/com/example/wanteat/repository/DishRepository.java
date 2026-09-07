package com.example.wanteat.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.wanteat.domain.Dish;

public interface DishRepository extends JpaRepository<Dish, Long> {
    
}
