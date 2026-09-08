package com.example.wanteat.mapper;

import org.springframework.stereotype.Component;

import com.example.wanteat.domain.Request;
import com.example.wanteat.dto.RequestResponse;

@Component
public class RequestMapper {

    public RequestResponse toResponse(Request request) {
        return new RequestResponse(
                request.getId(),
                request.getRequestedBy(),
                request.getBody(),
                request.getStatus(),
                request.getFulfilledMenuId(),
                request.getCreatedAt(),
                request.getFulfilledAt());
    }
}
