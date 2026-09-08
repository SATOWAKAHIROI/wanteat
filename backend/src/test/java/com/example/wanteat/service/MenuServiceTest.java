package com.example.wanteat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.wanteat.domain.Dish;
import com.example.wanteat.domain.DishCategory;
import com.example.wanteat.domain.Menu;
import com.example.wanteat.domain.MenuStatus;
import com.example.wanteat.domain.Request;
import com.example.wanteat.domain.RequestStatus;
import com.example.wanteat.domain.ShoppingItem;
import com.example.wanteat.domain.User;
import com.example.wanteat.dto.DishRequest;
import com.example.wanteat.dto.IngredientRequest;
import com.example.wanteat.dto.MenuCreateRequest;
import com.example.wanteat.exception.NotFoundException;
import com.example.wanteat.mapper.MenuMapper;
import com.example.wanteat.repository.MenuRepository;
import com.example.wanteat.repository.RequestRepository;
import com.example.wanteat.repository.ShoppingItemRepository;
import com.example.wanteat.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class MenuServiceTest {

    @Mock
    private MenuRepository menuRepository;

    @Mock
    private ShoppingItemRepository shoppingItemRepository;

    @Mock
    private RequestRepository requestRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MenuMapper menuMapper;

    @InjectMocks
    private MenuService menuService;

    private MenuCreateRequest 主菜と副菜のリクエスト(List<Long> fulfilledRequestIds) {
        var main = new DishRequest(
                DishCategory.MAIN, "鶏の照り焼き", "甘辛い定番", 25,
                List.of("鶏肉を切る", "焼く"),
                List.of(new IngredientRequest("鶏もも肉", "300", "g", false)));
        var side = new DishRequest(
                DishCategory.SIDE, "ほうれん草のおひたし", null, 10,
                List.of("茹でる"),
                List.of(new IngredientRequest("ほうれん草", "1", "束", false)));
        return new MenuCreateRequest(LocalDate.of(2026, 9, 8), List.of(main, side), fulfilledRequestIds);
    }

    @Test
    void create_正常系_料理と材料が確定状態で保存される() {
        // Arrange
        User user = new User();
        user.setId(1L);
        when(userRepository.getReferenceById(1L)).thenReturn(user);
        when(menuRepository.save(any(Menu.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(shoppingItemRepository.countByUserId(1L)).thenReturn(0L);

        // Act
        menuService.create(1L, 主菜と副菜のリクエスト(null));

        // Assert
        ArgumentCaptor<Menu> captor = ArgumentCaptor.forClass(Menu.class);
        verify(menuRepository).save(captor.capture());
        Menu saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(MenuStatus.CONFIRMED);
        assertThat(saved.getDishes()).hasSize(2);

        Dish first = saved.getDishes().get(0);
        assertThat(first.getMenu()).isSameAs(saved);
        assertThat(first.getSortOrder()).isEqualTo(1);
        assertThat(first.getSteps()).containsExactly("鶏肉を切る", "焼く");
        assertThat(first.getIngredients()).hasSize(1);
        assertThat(first.getIngredients().get(0).getDish()).isSameAs(first);
    }

    @Test
    void create_正常系_材料が買い物リストへ展開される() {
        // Arrange
        User user = new User();
        user.setId(1L);
        when(userRepository.getReferenceById(1L)).thenReturn(user);
        when(menuRepository.save(any(Menu.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(shoppingItemRepository.countByUserId(1L)).thenReturn(2L);

        // Act
        menuService.create(1L, 主菜と副菜のリクエスト(null));

        // Assert
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ShoppingItem>> captor = ArgumentCaptor.forClass(List.class);
        verify(shoppingItemRepository).saveAll(captor.capture());
        List<ShoppingItem> items = captor.getValue();
        assertThat(items).hasSize(2);
        assertThat(items).extracting(ShoppingItem::getName)
                .containsExactly("鶏もも肉", "ほうれん草");
        assertThat(items).allSatisfy(item -> assertThat(item.isChecked()).isFalse());
        assertThat(items.get(0).getSortOrder()).isEqualTo(3);
    }

    @Test
    void create_リクエストIDを指定した場合_消化済みになる() {
        // Arrange
        User user = new User();
        user.setId(1L);
        Request request = new Request();
        request.setStatus(RequestStatus.OPEN);
        when(userRepository.getReferenceById(1L)).thenReturn(user);
        when(menuRepository.save(any(Menu.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(shoppingItemRepository.countByUserId(1L)).thenReturn(0L);
        when(requestRepository.findByUserIdAndIdIn(1L, List.of(7L))).thenReturn(List.of(request));

        // Act
        menuService.create(1L, 主菜と副菜のリクエスト(List.of(7L)));

        // Assert
        assertThat(request.getStatus()).isEqualTo(RequestStatus.FULFILLED);
        assertThat(request.getFulfilledAt()).isNotNull();
        verify(requestRepository).saveAll(List.of(request));
    }

    @Test
    void markCooked_正常系_COOKEDになる() {
        // Arrange
        Menu menu = new Menu();
        menu.setStatus(MenuStatus.CONFIRMED);
        when(menuRepository.findByIdAndUserId(3L, 1L)).thenReturn(Optional.of(menu));
        when(menuRepository.save(any(Menu.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        menuService.markCooked(1L, 3L);

        // Assert
        assertThat(menu.getStatus()).isEqualTo(MenuStatus.COOKED);
    }

    @Test
    void findById_他人の献立の場合_NotFoundExceptionをスロー() {
        // Arrange
        when(menuRepository.findByIdAndUserId(3L, 1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> menuService.findById(1L, 3L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("献立");
    }

    @Test
    void create_常備品は買い物リストへ展開されない() {
        // Arrange
        User user = new User();
        user.setId(1L);
        when(userRepository.getReferenceById(1L)).thenReturn(user);
        when(menuRepository.save(any(Menu.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(shoppingItemRepository.countByUserId(1L)).thenReturn(0L);

        var main = new DishRequest(
                DishCategory.MAIN, "肉じゃが", null, 30,
                List.of("煮る"),
                List.of(
                        new IngredientRequest("牛こま肉", "200", "g", false),
                        new IngredientRequest("しょうゆ", "大さじ2", "", true),
                        new IngredientRequest("水", "400", "ml", true)));
        var request = new MenuCreateRequest(LocalDate.of(2026, 9, 8), List.of(main), null);

        // Act
        menuService.create(1L, request);

        // Assert
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ShoppingItem>> captor = ArgumentCaptor.forClass(List.class);
        verify(shoppingItemRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).extracting(ShoppingItem::getName).containsExactly("牛こま肉");
    }
}
