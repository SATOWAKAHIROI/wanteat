package com.example.wanteat.repository;

import com.example.wanteat.domain.Sample;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.assertj.core.api.Assertions.*;

// TODO: アプリのドメインに合わせてリネーム・修正する
// H2インメモリDBを使用（Flywayは無効、テスト用DDLはJPAエンティティから自動生成）
@DataJpaTest
class SampleRepositoryTest {

    @Autowired
    private SampleRepository sampleRepository;

    @Test
    void save_正常系_保存して取得できる() {
        // Arrange
        Sample sample = new Sample();
        sample.setName("テスト");

        // Act
        Sample saved = sampleRepository.save(sample);
        Sample found = sampleRepository.findById(saved.getId()).orElseThrow();

        // Assert
        assertThat(found.getName()).isEqualTo("テスト");
    }

    @Test
    void delete_正常系_削除後に取得できない() {
        // Arrange
        Sample sample = new Sample();
        sample.setName("削除対象");
        Sample saved = sampleRepository.save(sample);

        // Act
        sampleRepository.deleteById(saved.getId());

        // Assert
        assertThat(sampleRepository.findById(saved.getId())).isEmpty();
    }
}
