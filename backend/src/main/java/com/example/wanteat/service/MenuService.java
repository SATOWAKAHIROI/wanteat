package com.example.wanteat.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.wanteat.domain.Dish;
import com.example.wanteat.domain.Ingredient;
import com.example.wanteat.domain.Menu;
import com.example.wanteat.domain.MenuStatus;
import com.example.wanteat.domain.Request;
import com.example.wanteat.domain.RequestStatus;
import com.example.wanteat.domain.ShoppingItem;
import com.example.wanteat.domain.User;
import com.example.wanteat.dto.DishRequest;
import com.example.wanteat.dto.IngredientRequest;
import com.example.wanteat.dto.MenuCreateRequest;
import com.example.wanteat.dto.MenuResponse;
import com.example.wanteat.exception.NotFoundException;
import com.example.wanteat.mapper.MenuMapper;
import com.example.wanteat.repository.MenuRepository;
import com.example.wanteat.repository.RequestRepository;
import com.example.wanteat.repository.ShoppingItemRepository;
import com.example.wanteat.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MenuService {

    private final MenuRepository menuRepository;
    private final ShoppingItemRepository shoppingItemRepository;
    private final RequestRepository requestRepository;
    private final UserRepository userRepository;
    private final MenuMapper menuMapper;

    /**
     * 献立を確定する。料理と材料をまとめて保存し、材料を買い物リストへ展開する。
     * AI 提案を採用した場合も、生成結果をこの形に詰めて呼び出す。
     */
    @Transactional
    public MenuResponse create(Long userId, MenuCreateRequest request) {
        User user = userRepository.getReferenceById(userId);

        Menu menu = new Menu();
        menu.setUser(user);
        menu.setCookedOn(request.cookedOn());
        menu.setStatus(MenuStatus.CONFIRMED);

        int dishOrder = 1;
        for (DishRequest dishRequest : request.dishes()) {
            menu.addDish(toDish(dishRequest, dishOrder++));
        }

        Menu saved = menuRepository.save(menu);

        expandToShoppingList(user, saved);
        fulfillRequests(userId, saved, request.fulfilledRequestIds());

        return menuMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<MenuResponse> findAll(Long userId, LocalDate from, LocalDate to) {
        List<Menu> menus = (from == null && to == null)
                ? menuRepository.findByUserIdOrderByCookedOnDescIdDesc(userId)
                : menuRepository.findByUserIdAndCookedOnBetweenOrderByCookedOnDescIdDesc(
                        userId,
                        from == null ? LocalDate.of(1970, 1, 1) : from,
                        to == null ? LocalDate.of(9999, 12, 31) : to);
        return menus.stream().map(menuMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public MenuResponse findById(Long userId, Long id) {
        return menuMapper.toResponse(findOrThrow(userId, id));
    }

    /** 調理済みにする。 */
    @Transactional
    public MenuResponse markCooked(Long userId, Long id) {
        Menu menu = findOrThrow(userId, id);
        menu.setStatus(MenuStatus.COOKED);
        return menuMapper.toResponse(menuRepository.save(menu));
    }

    /**
     * 献立を削除する。料理と材料は JPA のカスケードで、
     * この献立由来の買い物リストは DB の ON DELETE CASCADE で消える。
     */
    @Transactional
    public void delete(Long userId, Long id) {
        menuRepository.delete(findOrThrow(userId, id));
    }

    private Menu findOrThrow(Long userId, Long id) {
        return menuRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NotFoundException("献立が見つかりません"));
    }

    private Dish toDish(DishRequest request, int sortOrder) {
        Dish dish = new Dish();
        dish.setCategory(request.category());
        dish.setName(request.name());
        dish.setDescription(request.description());
        dish.setCookingMinutes(request.cookingMinutes());
        dish.setSteps(new ArrayList<>(request.steps()));
        dish.setSortOrder(sortOrder);

        if (request.ingredients() != null) {
            int ingredientOrder = 1;
            for (IngredientRequest ingredientRequest : request.ingredients()) {
                dish.addIngredient(toIngredient(ingredientRequest, ingredientOrder++));
            }
        }
        return dish;
    }

    private Ingredient toIngredient(IngredientRequest request, int sortOrder) {
        Ingredient ingredient = new Ingredient();
        ingredient.setName(request.name());
        ingredient.setAmount(request.amount());
        ingredient.setUnit(request.unit());
        ingredient.setPantryStaple(Boolean.TRUE.equals(request.pantryStaple()));
        ingredient.setSortOrder(sortOrder);
        return ingredient;
    }

    /**
     * 材料を買い物リストへ展開する。
     * 常備品（水・塩・油など）はレシピには残すが、買い物リストには載せない。
     * 同じ食材の名寄せは行わない（要件どおり）。
     */
    private void expandToShoppingList(User user, Menu menu) {
        int sortOrder = (int) shoppingItemRepository.countByUserId(user.getId()) + 1;
        List<ShoppingItem> items = new ArrayList<>();

        for (Dish dish : menu.getDishes()) {
            for (Ingredient ingredient : dish.getIngredients()) {
                if (ingredient.isPantryStaple()) {
                    continue;
                }
                ShoppingItem item = new ShoppingItem();
                item.setUser(user);
                item.setMenuId(menu.getId());
                item.setName(ingredient.getName());
                item.setAmount(ingredient.getAmount());
                item.setUnit(ingredient.getUnit());
                item.setChecked(false);
                item.setSortOrder(sortOrder++);
                items.add(item);
            }
        }
        shoppingItemRepository.saveAll(items);
    }

    /** 指定されたリクエストを消化済みにする。 */
    private void fulfillRequests(Long userId, Menu menu, List<Long> requestIds) {
        if (requestIds == null || requestIds.isEmpty()) {
            return;
        }
        List<Request> requests = requestRepository.findByUserIdAndIdIn(userId, requestIds);
        LocalDateTime now = LocalDateTime.now();
        for (Request request : requests) {
            request.setStatus(RequestStatus.FULFILLED);
            request.setFulfilledMenuId(menu.getId());
            request.setFulfilledAt(now);
        }
        requestRepository.saveAll(requests);
    }
}
