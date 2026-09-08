package com.example.wanteat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.wanteat.domain.ShoppingItem;
import com.example.wanteat.domain.User;
import com.example.wanteat.dto.ShoppingItemRequest;
import com.example.wanteat.exception.NotFoundException;
import com.example.wanteat.mapper.ShoppingItemMapper;
import com.example.wanteat.repository.ShoppingItemRepository;
import com.example.wanteat.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class ShoppingItemServiceTest {

    @Mock
    private ShoppingItemRepository shoppingItemRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ShoppingItemMapper shoppingItemMapper;

    @InjectMocks
    private ShoppingItemService shoppingItemService;

    @Test
    void create_手動追加は献立に紐づかず未チェックで作られる() {
        // Arrange
        when(userRepository.getReferenceById(1L)).thenReturn(new User());
        when(shoppingItemRepository.countByUserId(1L)).thenReturn(3L);
        when(shoppingItemRepository.save(any(ShoppingItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        shoppingItemService.create(1L, new ShoppingItemRequest("食器用洗剤", null, null));

        // Assert
        ArgumentCaptor<ShoppingItem> captor = ArgumentCaptor.forClass(ShoppingItem.class);
        verify(shoppingItemRepository).save(captor.capture());
        assertThat(captor.getValue().getMenuId()).isNull();
        assertThat(captor.getValue().isChecked()).isFalse();
        assertThat(captor.getValue().getSortOrder()).isEqualTo(4);
    }

    @Test
    void toggleChecked_チェック状態が反転する() {
        // Arrange
        ShoppingItem item = new ShoppingItem();
        item.setChecked(false);
        when(shoppingItemRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(item));
        when(shoppingItemRepository.save(any(ShoppingItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        shoppingItemService.toggleChecked(1L, 10L);

        // Assert
        assertThat(item.isChecked()).isTrue();
    }

    @Test
    void toggleChecked_他人のアイテムの場合_NotFoundExceptionをスロー() {
        // Arrange
        when(shoppingItemRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> shoppingItemService.toggleChecked(1L, 10L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void deleteChecked_購入済みをまとめて削除する() {
        // Act
        shoppingItemService.deleteChecked(1L);

        // Assert
        verify(shoppingItemRepository).deleteByUserIdAndCheckedTrue(1L);
    }
}
