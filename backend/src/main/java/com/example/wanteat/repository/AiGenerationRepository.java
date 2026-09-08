package com.example.wanteat.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.wanteat.domain.AiGeneration;

public interface AiGenerationRepository extends JpaRepository<AiGeneration, Long> {
}
