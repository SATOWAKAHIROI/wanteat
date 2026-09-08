package com.example.wanteat.mapper;

import org.springframework.stereotype.Component;

import com.example.wanteat.domain.UserPreference;
import com.example.wanteat.dto.PreferenceResponse;

@Component
public class UserPreferenceMapper {

    public PreferenceResponse toResponse(UserPreference preference) {
        return new PreferenceResponse(
                preference.getId(),
                preference.getHouseholdSize(),
                preference.getAllergies(),
                preference.getDislikedFoods(),
                preference.getNote(),
                preference.getUpdatedAt());
    }
}
