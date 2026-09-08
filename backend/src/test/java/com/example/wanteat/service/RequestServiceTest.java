package com.example.wanteat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.wanteat.domain.Request;
import com.example.wanteat.domain.RequestStatus;
import com.example.wanteat.domain.RequestedBy;
import com.example.wanteat.domain.User;
import com.example.wanteat.dto.RequestCreateRequest;
import com.example.wanteat.exception.NotFoundException;
import com.example.wanteat.mapper.RequestMapper;
import com.example.wanteat.repository.RequestRepository;
import com.example.wanteat.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class RequestServiceTest {

    @Mock
    private RequestRepository requestRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RequestMapper requestMapper;

    @InjectMocks
    private RequestService requestService;

    @Test
    void create_正常系_OPEN状態で作成される() {
        // Arrange
        when(userRepository.getReferenceById(1L)).thenReturn(new User());
        when(requestRepository.save(any(Request.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        requestService.create(1L, new RequestCreateRequest(RequestedBy.PARTNER, "カレーが食べたい"));

        // Assert
        ArgumentCaptor<Request> captor = ArgumentCaptor.forClass(Request.class);
        verify(requestRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(RequestStatus.OPEN);
        assertThat(captor.getValue().getRequestedBy()).isEqualTo(RequestedBy.PARTNER);
        assertThat(captor.getValue().getBody()).isEqualTo("カレーが食べたい");
    }

    @Test
    void findAll_状態を指定した場合_絞り込んだ検索が使われる() {
        // Arrange
        when(requestRepository.findByUserIdAndStatusOrderByCreatedAtDescIdDesc(1L, RequestStatus.OPEN))
                .thenReturn(List.of());

        // Act
        requestService.findAll(1L, RequestStatus.OPEN);

        // Assert
        verify(requestRepository).findByUserIdAndStatusOrderByCreatedAtDescIdDesc(1L, RequestStatus.OPEN);
    }

    @Test
    void fulfill_正常系_FULFILLEDになり消化日時が入る() {
        // Arrange
        Request request = new Request();
        request.setStatus(RequestStatus.OPEN);
        when(requestRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(request));
        when(requestRepository.save(any(Request.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        requestService.fulfill(1L, 5L);

        // Assert
        assertThat(request.getStatus()).isEqualTo(RequestStatus.FULFILLED);
        assertThat(request.getFulfilledAt()).isNotNull();
    }

    @Test
    void delete_存在しない場合_NotFoundExceptionをスロー() {
        // Arrange
        when(requestRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> requestService.delete(1L, 99L))
                .isInstanceOf(NotFoundException.class);
    }
}
