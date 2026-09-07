package com.example.wanteat.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.wanteat.domain.Menu;

public interface MenuRepository extends JpaRepository<Menu, Long> {
    
}
