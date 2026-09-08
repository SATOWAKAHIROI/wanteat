package com.example.wanteat.service;

import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.wanteat.domain.AiGeneration;
import com.example.wanteat.domain.AiPhase;
import com.example.wanteat.repository.AiGenerationRepository;
import com.example.wanteat.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * AI 呼び出しの記録。
 * AI の呼び出し自体はトランザクション外で行うため、記録だけを独立したトランザクションで処理する。
 */
@Component
@RequiredArgsConstructor
public class AiGenerationLogger {

    private final AiGenerationRepository aiGenerationRepository;
    private final UserRepository userRepository;

    @Transactional
    public void log(Long userId, AiPhase phase, Map<String, Object> requestPayload,
            Map<String, Object> responsePayload, String model,
            Integer inputTokens, Integer outputTokens, int latencyMs) {

        AiGeneration generation = new AiGeneration();
        generation.setUser(userRepository.getReferenceById(userId));
        generation.setPhase(phase);
        generation.setRequestPayload(requestPayload);
        generation.setResponsePayload(responsePayload);
        generation.setModel(model);
        generation.setInputTokens(inputTokens);
        generation.setOutputTokens(outputTokens);
        generation.setLatencyMs(latencyMs);
        aiGenerationRepository.save(generation);
    }
}
