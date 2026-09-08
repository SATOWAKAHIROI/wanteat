package com.example.wanteat.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.wanteat.dto.PreferenceRequest;
import com.example.wanteat.dto.PreferenceResponse;
import com.example.wanteat.security.LoginUser;
import com.example.wanteat.service.UserPreferenceService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/preferences")
@RequiredArgsConstructor
public class PreferenceController {

    private final UserPreferenceService userPreferenceService;

    @GetMapping
    public ResponseEntity<PreferenceResponse> get(@AuthenticationPrincipal LoginUser loginUser) {
        return ResponseEntity.ok(userPreferenceService.find(loginUser.userId()));
    }

    @PutMapping
    public ResponseEntity<PreferenceResponse> save(
            @AuthenticationPrincipal LoginUser loginUser,
            @Valid @RequestBody PreferenceRequest request) {
        return ResponseEntity.ok(userPreferenceService.save(loginUser.userId(), request));
    }
}
