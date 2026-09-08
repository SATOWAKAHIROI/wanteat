package com.example.wanteat.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.wanteat.domain.DishCategory;
import com.example.wanteat.dto.DishRequest;
import com.example.wanteat.dto.GeneratedDish;
import com.example.wanteat.dto.GeneratedIngredient;
import com.example.wanteat.dto.GeneratedMenuDetail;
import com.example.wanteat.dto.IngredientRequest;
import com.example.wanteat.dto.MenuConfirmRequest;
import com.example.wanteat.dto.MenuCreateRequest;
import com.example.wanteat.dto.MenuResponse;
import com.example.wanteat.dto.SuggestedDish;
import com.example.wanteat.exception.AiGenerationException;

import lombok.RequiredArgsConstructor;

/**
 * 第1段階の提案を受けて献立を確定する。
 *
 * <p>AI の呼び出し（十数秒かかる）をトランザクションの外に置くため、
 * MenuService とは別のクラスに分けている。ここに {@code @Transactional} を付けないこと。
 */
@Service
@RequiredArgsConstructor
public class MenuConfirmService {

    private final AiMenuService aiMenuService;
    private final MenuService menuService;

    public MenuResponse confirm(Long userId, MenuConfirmRequest request) {
        GeneratedMenuDetail detail =
                aiMenuService.generateDetail(userId, request.mainDish(), request.sideDish());

        MenuCreateRequest createRequest = new MenuCreateRequest(
                request.cookedOn(),
                List.of(
                        toDishRequest(DishCategory.MAIN, request.mainDish(), detail.mainDish()),
                        toDishRequest(DishCategory.SIDE, request.sideDish(), detail.sideDish())),
                request.fulfilledRequestIds());

        return menuService.create(userId, createRequest);
    }

    /**
     * 「大さじ」「小さじ」などは日本語では単位が前に来る。
     * AI が amount="1" / unit="小さじ" と分けて返すことがあるため、
     * プロンプトの指示に頼らず確定的に正規化する（「1小さじ」ではなく「小さじ1」にする）。
     */
    private static final List<String> PREFIX_UNITS =
            List.of("大さじ", "小さじ", "ひとつまみ", "少々", "適量");

    private IngredientRequest toIngredientRequest(GeneratedIngredient ingredient) {
        String amount = ingredient.amount();
        String unit = ingredient.unit();

        if (unit != null && PREFIX_UNITS.contains(unit)) {
            amount = (amount == null || amount.isBlank()) ? unit : unit + amount;
            unit = "";
        }
        return new IngredientRequest(ingredient.name(), amount, unit, ingredient.pantryStaple());
    }

    private DishRequest toDishRequest(DishCategory category, SuggestedDish suggested, GeneratedDish generated) {
        if (generated == null || generated.steps() == null || generated.steps().isEmpty()) {
            throw new AiGenerationException("レシピの生成に失敗しました。もう一度お試しください");
        }

        List<IngredientRequest> ingredients = (generated.ingredients() == null)
                ? List.of()
                : generated.ingredients().stream()
                        .map(this::toIngredientRequest)
                        .toList();

        return new DishRequest(
                category,
                suggested.name(),
                suggested.description(),
                suggested.cookingMinutes(),
                generated.steps(),
                ingredients);
    }
}
