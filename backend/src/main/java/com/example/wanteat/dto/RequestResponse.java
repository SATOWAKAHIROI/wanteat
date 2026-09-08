package com.example.wanteat.dto;

import java.time.LocalDateTime;

import com.example.wanteat.domain.RequestStatus;
import com.example.wanteat.domain.RequestedBy;

public record RequestResponse(
        Long id,
        RequestedBy requestedBy,
        String body,
        RequestStatus status,
        Long fulfilledMenuId,
        LocalDateTime createdAt,
        LocalDateTime fulfilledAt) {
}
