package com.example.wanteat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.wanteat.domain.User;
import com.example.wanteat.domain.UserPreference;
import com.example.wanteat.dto.PreferenceRequest;
import com.example.wanteat.exception.NotFoundException;
import com.example.wanteat.mapper.UserPreferenceMapper;
import com.example.wanteat.repository.UserPreferenceRepository;
import com.example.wanteat.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserPreferenceServiceTest {

    @Mock
    private UserPreferenceRepository userPreferenceRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserPreferenceMapper userPreferenceMapper;

    @InjectMocks
    private UserPreferenceService userPreferenceService;

    @Test
    void find_未登録の場合_NotFoundExceptionをスロー() {
        // Arrange
        when(userPreferenceRepository.findByUserId(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userPreferenceService.find(1L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("好み設定");
    }

    @Test
    void save_未登録の場合_新規作成される() {
        // Arrange
        var request = new PreferenceRequest(2, "そば", "パクチー", "和食多め");
        when(userPreferenceRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(userRepository.getReferenceById(1L)).thenReturn(new User());
        when(userPreferenceRepository.save(any(UserPreference.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        userPreferenceService.save(1L, request);

        // Assert
        org.mockito.ArgumentCaptor<UserPreference> captor =
                org.mockito.ArgumentCaptor.forClass(UserPreference.class);
        org.mockito.Mockito.verify(userPreferenceRepository).save(captor.capture());
        assertThat(captor.getValue().getHouseholdSize()).isEqualTo(2);
        assertThat(captor.getValue().getAllergies()).isEqualTo("そば");
        assertThat(captor.getValue().getUser()).isNotNull();
    }

    @Test
    void save_登録済みの場合_既存レコードが更新される() {
        // Arrange
        UserPreference existing = new UserPreference();
        existing.setHouseholdSize(1);
        when(userPreferenceRepository.findByUserId(1L)).thenReturn(Optional.of(existing));
        when(userPreferenceRepository.save(any(UserPreference.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        userPreferenceService.save(1L, new PreferenceRequest(4, null, null, null));

        // Assert
        assertThat(existing.getHouseholdSize()).isEqualTo(4);
        org.mockito.Mockito.verify(userRepository, org.mockito.Mockito.never()).getReferenceById(any());
    }
}
