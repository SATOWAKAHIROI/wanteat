package com.example.wanteat.mapper;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import com.example.wanteat.domain.Dish;
import com.example.wanteat.domain.Ingredient;
import com.example.wanteat.domain.Menu;
import com.example.wanteat.dto.DishResponse;
import com.example.wanteat.dto.IngredientResponse;
import com.example.wanteat.dto.MenuResponse;

@Component
public class MenuMapper {

    private static final Comparator<Dish> DISH_ORDER =
            Comparator.comparing(Dish::getSortOrder, Comparator.nullsLast(Comparator.naturalOrder()));

    private static final Comparator<Ingredient> INGREDIENT_ORDER =
            Comparator.comparing(Ingredient::getSortOrder, Comparator.nullsLast(Comparator.naturalOrder()));

    public MenuResponse toResponse(Menu menu) {
        List<DishResponse> dishes = menu.getDishes().stream()
                .sorted(DISH_ORDER)
                .map(this::toDishResponse)
                .toList();
        return new MenuResponse(
                menu.getId(),
                menu.getCookedOn(),
                menu.getStatus(),
                dishes,
                menu.getCreatedAt(),
                menu.getUpdatedAt());
    }

    public DishResponse toDishResponse(Dish dish) {
        List<IngredientResponse> ingredients = dish.getIngredients().stream()
                .sorted(INGREDIENT_ORDER)
                .map(this::toIngredientResponse)
                .toList();
        return new DishResponse(
                dish.getId(),
                dish.getCategory(),
                dish.getName(),
                dish.getDescription(),
                dish.getCookingMinutes(),
                List.copyOf(dish.getSteps()),
                dish.getSortOrder(),
                ingredients);
    }

    public IngredientResponse toIngredientResponse(Ingredient ingredient) {
        return new IngredientResponse(
                ingredient.getId(),
                ingredient.getName(),
                ingredient.getAmount(),
                ingredient.getUnit(),
                ingredient.isPantryStaple(),
                ingredient.getSortOrder());
    }
}
