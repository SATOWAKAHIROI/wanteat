package com.example.wanteat.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.wanteat.domain.Menu;

public interface MenuRepository extends JpaRepository<Menu, Long> {

    List<Menu> findByUserIdOrderByCookedOnDescIdDesc(Long userId);

    List<Menu> findByUserIdAndCookedOnBetweenOrderByCookedOnDescIdDesc(
            Long userId, LocalDate from, LocalDate to);

    Optional<Menu> findByIdAndUserId(Long id, Long userId);
}
