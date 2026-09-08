package com.example.wanteat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.wanteat.domain.DishCategory;
import com.example.wanteat.dto.DishRequest;
import com.example.wanteat.dto.GeneratedDish;
import com.example.wanteat.dto.GeneratedIngredient;
import com.example.wanteat.dto.GeneratedMenuDetail;
import com.example.wanteat.dto.MenuConfirmRequest;
import com.example.wanteat.dto.MenuCreateRequest;
import com.example.wanteat.dto.SuggestedDish;
import com.example.wanteat.exception.AiGenerationException;

@ExtendWith(MockitoExtension.class)
class MenuConfirmServiceTest {

    @Mock
    private AiMenuService aiMenuService;

    @Mock
    private MenuService menuService;

    @InjectMocks
    private MenuConfirmService menuConfirmService;

    private final SuggestedDish 主菜 =
            new SuggestedDish("鶏の照り焼き", "甘辛い定番", 25, List.of("鶏もも肉", "醤油"));
    private final SuggestedDish 副菜 =
            new SuggestedDish("ほうれん草のおひたし", "箸休めに", 10, List.of("ほうれん草"));

    private MenuConfirmRequest 確定リクエスト() {
        return new MenuConfirmRequest(LocalDate.of(2026, 9, 8), 主菜, 副菜, List.of(7L));
    }

    @Test
    void confirm_正常系_AIの生成結果が献立作成に渡される() {
        // Arrange
        var detail = new GeneratedMenuDetail(
                new GeneratedDish(
                        List.of(new GeneratedIngredient("鶏もも肉", "300", "g", false)),
                        List.of("切る", "焼く")),
                new GeneratedDish(
                        List.of(new GeneratedIngredient("ほうれん草", "1", "束", false)),
                        List.of("茹でる")));
        when(aiMenuService.generateDetail(1L, 主菜, 副菜)).thenReturn(detail);

        // Act
        menuConfirmService.confirm(1L, 確定リクエスト());

        // Assert
        ArgumentCaptor<MenuCreateRequest> captor = ArgumentCaptor.forClass(MenuCreateRequest.class);
        verify(menuService).create(eq(1L), captor.capture());
        MenuCreateRequest created = captor.getValue();

        assertThat(created.cookedOn()).isEqualTo(LocalDate.of(2026, 9, 8));
        assertThat(created.fulfilledRequestIds()).containsExactly(7L);
        assertThat(created.dishes()).hasSize(2);

        DishRequest main = created.dishes().get(0);
        assertThat(main.category()).isEqualTo(DishCategory.MAIN);
        assertThat(main.name()).isEqualTo("鶏の照り焼き");
        assertThat(main.cookingMinutes()).isEqualTo(25);
        assertThat(main.steps()).containsExactly("切る", "焼く");
        assertThat(main.ingredients()).hasSize(1);
        assertThat(created.dishes().get(1).category()).isEqualTo(DishCategory.SIDE);
    }

    @Test
    void confirm_AIが手順を返さない場合_AiGenerationExceptionをスロー() {
        // Arrange
        var detail = new GeneratedMenuDetail(
                new GeneratedDish(List.of(), List.of()),
                new GeneratedDish(List.of(), List.of("茹でる")));
        when(aiMenuService.generateDetail(1L, 主菜, 副菜)).thenReturn(detail);

        // Act & Assert
        assertThatThrownBy(() -> menuConfirmService.confirm(1L, 確定リクエスト()))
                .isInstanceOf(AiGenerationException.class);
        verify(menuService, never()).create(any(), any());
    }

    @Test
    void confirm_大さじ小さじは単位を前に付けた形に正規化される() {
        // Arrange
        var detail = new GeneratedMenuDetail(
                new GeneratedDish(
                        List.of(
                                new GeneratedIngredient("しょうゆ", "1", "小さじ", true),
                                new GeneratedIngredient("塩", "", "少々", true),
                                new GeneratedIngredient("豚こま肉", "200", "g", false)),
                        List.of("炒める")),
                new GeneratedDish(List.of(), List.of("和える")));
        when(aiMenuService.generateDetail(1L, 主菜, 副菜)).thenReturn(detail);

        // Act
        menuConfirmService.confirm(1L, 確定リクエスト());

        // Assert
        ArgumentCaptor<MenuCreateRequest> captor = ArgumentCaptor.forClass(MenuCreateRequest.class);
        verify(menuService).create(eq(1L), captor.capture());
        var ingredients = captor.getValue().dishes().get(0).ingredients();

        assertThat(ingredients.get(0).amount()).isEqualTo("小さじ1");
        assertThat(ingredients.get(0).unit()).isEmpty();
        assertThat(ingredients.get(0).pantryStaple()).isTrue();

        assertThat(ingredients.get(1).amount()).isEqualTo("少々");
        assertThat(ingredients.get(1).unit()).isEmpty();

        // 通常の単位はそのまま
        assertThat(ingredients.get(2).amount()).isEqualTo("200");
        assertThat(ingredients.get(2).unit()).isEqualTo("g");
        assertThat(ingredients.get(2).pantryStaple()).isFalse();
    }
}
