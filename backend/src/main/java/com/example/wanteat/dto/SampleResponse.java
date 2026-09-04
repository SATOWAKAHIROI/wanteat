package com.example.wanteat.dto;

import com.example.wanteat.domain.Sample;
import java.time.LocalDateTime;

// TODO: アプリのドメインに合わせてリネーム・修正する
public record SampleResponse(
    Long id,
    String name,
    LocalDateTime createdAt
) {
    public static SampleResponse from(Sample sample) {
        return new SampleResponse(sample.getId(), sample.getName(), sample.getCreatedAt());
    }
}
