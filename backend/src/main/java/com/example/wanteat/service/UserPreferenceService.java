package com.example.wanteat.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.wanteat.domain.UserPreference;
import com.example.wanteat.dto.PreferenceRequest;
import com.example.wanteat.dto.PreferenceResponse;
import com.example.wanteat.exception.NotFoundException;
import com.example.wanteat.mapper.UserPreferenceMapper;
import com.example.wanteat.repository.UserPreferenceRepository;
import com.example.wanteat.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserPreferenceService {

    private final UserPreferenceRepository userPreferenceRepository;
    private final UserRepository userRepository;
    private final UserPreferenceMapper userPreferenceMapper;

    @Transactional(readOnly = true)
    public PreferenceResponse find(Long userId) {
        return userPreferenceRepository.findByUserId(userId)
                .map(userPreferenceMapper::toResponse)
                .orElseThrow(() -> new NotFoundException("好み設定が登録されていません"));
    }

    /** 未登録なら作成し、登録済みなら更新する。 */
    @Transactional
    public PreferenceResponse save(Long userId, PreferenceRequest request) {
        UserPreference preference = userPreferenceRepository.findByUserId(userId)
                .orElseGet(() -> {
                    UserPreference created = new UserPreference();
                    created.setUser(userRepository.getReferenceById(userId));
                    return created;
                });

        preference.setHouseholdSize(request.householdSize());
        preference.setAllergies(request.allergies());
        preference.setDislikedFoods(request.dislikedFoods());
        preference.setNote(request.note());

        return userPreferenceMapper.toResponse(userPreferenceRepository.save(preference));
    }
}
