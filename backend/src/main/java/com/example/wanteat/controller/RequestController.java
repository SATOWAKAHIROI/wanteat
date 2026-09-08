package com.example.wanteat.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.wanteat.domain.RequestStatus;
import com.example.wanteat.dto.RequestCreateRequest;
import com.example.wanteat.dto.RequestResponse;
import com.example.wanteat.security.LoginUser;
import com.example.wanteat.service.RequestService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/requests")
@RequiredArgsConstructor
public class RequestController {

    private final RequestService requestService;

    @GetMapping
    public ResponseEntity<List<RequestResponse>> findAll(
            @AuthenticationPrincipal LoginUser loginUser,
            @RequestParam(required = false) RequestStatus status) {
        return ResponseEntity.ok(requestService.findAll(loginUser.userId(), status));
    }

    @PostMapping
    public ResponseEntity<RequestResponse> create(
            @AuthenticationPrincipal LoginUser loginUser,
            @Valid @RequestBody RequestCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(requestService.create(loginUser.userId(), request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<RequestResponse> fulfill(
            @AuthenticationPrincipal LoginUser loginUser,
            @PathVariable Long id) {
        return ResponseEntity.ok(requestService.fulfill(loginUser.userId(), id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal LoginUser loginUser,
            @PathVariable Long id) {
        requestService.delete(loginUser.userId(), id);
        return ResponseEntity.noContent().build();
    }
}
