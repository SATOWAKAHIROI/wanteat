package com.example.wanteat.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.wanteat.domain.Request;
import com.example.wanteat.domain.RequestStatus;

public interface RequestRepository extends JpaRepository<Request, Long> {

    List<Request> findByUserIdOrderByCreatedAtDescIdDesc(Long userId);

    List<Request> findByUserIdAndStatusOrderByCreatedAtDescIdDesc(Long userId, RequestStatus status);

    Optional<Request> findByIdAndUserId(Long id, Long userId);

    List<Request> findByUserIdAndIdIn(Long userId, List<Long> ids);
}
