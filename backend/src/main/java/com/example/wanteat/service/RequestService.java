package com.example.wanteat.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.wanteat.domain.Request;
import com.example.wanteat.domain.RequestStatus;
import com.example.wanteat.dto.RequestCreateRequest;
import com.example.wanteat.dto.RequestResponse;
import com.example.wanteat.exception.NotFoundException;
import com.example.wanteat.mapper.RequestMapper;
import com.example.wanteat.repository.RequestRepository;
import com.example.wanteat.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RequestService {

    private final RequestRepository requestRepository;
    private final UserRepository userRepository;
    private final RequestMapper requestMapper;

    @Transactional(readOnly = true)
    public List<RequestResponse> findAll(Long userId, RequestStatus status) {
        List<Request> requests = (status == null)
                ? requestRepository.findByUserIdOrderByCreatedAtDescIdDesc(userId)
                : requestRepository.findByUserIdAndStatusOrderByCreatedAtDescIdDesc(userId, status);
        return requests.stream().map(requestMapper::toResponse).toList();
    }

    @Transactional
    public RequestResponse create(Long userId, RequestCreateRequest request) {
        Request created = new Request();
        created.setUser(userRepository.getReferenceById(userId));
        created.setRequestedBy(request.requestedBy());
        created.setBody(request.body());
        created.setStatus(RequestStatus.OPEN);
        return requestMapper.toResponse(requestRepository.save(created));
    }

    /** 手動で消化済みにする。献立に紐づく消化は MenuService が行う。 */
    @Transactional
    public RequestResponse fulfill(Long userId, Long id) {
        Request request = requestRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NotFoundException("リクエストが見つかりません"));
        request.setStatus(RequestStatus.FULFILLED);
        request.setFulfilledAt(LocalDateTime.now());
        return requestMapper.toResponse(requestRepository.save(request));
    }

    @Transactional
    public void delete(Long userId, Long id) {
        Request request = requestRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NotFoundException("リクエストが見つかりません"));
        requestRepository.delete(request);
    }
}
