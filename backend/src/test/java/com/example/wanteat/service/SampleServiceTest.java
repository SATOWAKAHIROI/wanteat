package com.example.wanteat.service;

import com.example.wanteat.domain.Sample;
import com.example.wanteat.dto.SampleRequest;
import com.example.wanteat.exception.NotFoundException;
import com.example.wanteat.repository.SampleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// TODO: アプリのドメインに合わせてリネーム・修正する
@ExtendWith(MockitoExtension.class)
class SampleServiceTest {

    @Mock
    private SampleRepository sampleRepository;

    @InjectMocks
    private SampleService sampleService;

    @Test
    void findById_存在する場合_レスポンスを返す() {
        // Arrange
        Sample sample = new Sample();
        sample.setName("テスト");
        when(sampleRepository.findById(1L)).thenReturn(Optional.of(sample));

        // Act
        var result = sampleService.findById(1L);

        // Assert
        assertThat(result.name()).isEqualTo("テスト");
    }

    @Test
    void findById_存在しない場合_NotFoundExceptionをスロー() {
        // Arrange
        when(sampleRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> sampleService.findById(99L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void create_正常系_保存されたEntityを返す() {
        // Arrange
        var request = new SampleRequest("新しいサンプル");
        Sample saved = new Sample();
        saved.setName("新しいサンプル");
        when(sampleRepository.save(any())).thenReturn(saved);

        // Act
        var result = sampleService.create(request);

        // Assert
        assertThat(result.name()).isEqualTo("新しいサンプル");
        verify(sampleRepository, times(1)).save(any());
    }
}
