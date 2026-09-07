package com.example.wanteat.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.wanteat.domain.Request;

public interface RequestRepository extends JpaRepository<Request, Long> {
    
}
