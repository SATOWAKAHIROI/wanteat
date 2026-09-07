package com.example.wanteat.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.wanteat.domain.UserPreference;

public interface UserPreferenceRepository extends JpaRepository<UserPreference, Long> {
    
}
