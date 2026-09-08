package com.example.wanteat.controller;

import static com.example.wanteat.support.TestAuth.loginUser;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.wanteat.domain.DishCategory;
import com.example.wanteat.domain.MenuStatus;
import com.example.wanteat.dto.DishResponse;
import com.example.wanteat.dto.IngredientResponse;
import com.example.wanteat.dto.MenuResponse;
import com.example.wanteat.config.SecurityConfig;
import com.example.wanteat.security.JwtAuthenticationFilter;
import com.example.wanteat.dto.SuggestResponse;
import com.example.wanteat.dto.SuggestedDish;
import com.example.wanteat.exception.AiGenerationException;
import com.example.wanteat.service.AiMenuService;
import com.example.wanteat.service.JwtService;
import com.example.wanteat.service.MenuConfirmService;
import com.example.wanteat.service.MenuService;

@Import({ SecurityConfig.class, JwtAuthenticationFilter.class })
@WebMvcTest(MenuController.class)
class MenuControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MenuService menuService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private AiMenuService aiMenuService;

    @MockitoBean
    private MenuConfirmService menuConfirmService;

    private MenuResponse 献立レスポンス() {
        var ingredient = new IngredientResponse(1L, "鶏もも肉", "300", "g", false, 1);
        var dish = new DishResponse(1L, DishCategory.MAIN, "鶏の照り焼き", "甘辛い定番",
                25, List.of("鶏肉を切る", "焼く"), 1, List.of(ingredient));
        return new MenuResponse(1L, LocalDate.of(2026, 9, 8), MenuStatus.CONFIRMED,
                List.of(dish), LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void 作成_正常系_201が返る() throws Exception {
        // Arrange
        when(menuService.create(eq(1L), any())).thenReturn(献立レスポンス());
        String body = """
                {
                  "cookedOn": "2026-09-08",
                  "dishes": [
                    {
                      "category": "MAIN",
                      "name": "鶏の照り焼き",
                      "steps": ["鶏肉を切る", "焼く"],
                      "ingredients": [{"name": "鶏もも肉", "amount": "300", "unit": "g"}]
                    }
                  ]
                }
                """;

        // Act & Assert
        mockMvc.perform(post("/api/menus")
                        .with(loginUser(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.dishes[0].name").value("鶏の照り焼き"))
                .andExpect(jsonPath("$.dishes[0].ingredients[0].name").value("鶏もも肉"));
    }

    @Test
    void 作成_バリデーションエラー_料理が空() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/menus")
                        .with(loginUser(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cookedOn\":\"2026-09-08\",\"dishes\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.dishes").exists());
    }

    @Test
    void 一覧取得_正常系() throws Exception {
        // Arrange
        when(menuService.findAll(1L, null, null)).thenReturn(List.of(献立レスポンス()));

        // Act & Assert
        mockMvc.perform(get("/api/menus").with(loginUser(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cookedOn").value("2026-09-08"));
    }

    @Test
    void 提案_正常系_主菜と副菜が返る() throws Exception {
        // Arrange
        when(aiMenuService.suggest(eq(1L), any())).thenReturn(new SuggestResponse(
                new SuggestedDish("鶏の照り焼き", "甘辛い定番", 25, List.of("鶏もも肉", "醤油")),
                new SuggestedDish("ほうれん草のおひたし", "箸休めに", 10, List.of("ほうれん草"))));

        // Act & Assert
        mockMvc.perform(post("/api/menus/suggest")
                        .with(loginUser(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"maxCookingMinutes\":30}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mainDish.name").value("鶏の照り焼き"))
                .andExpect(jsonPath("$.sideDish.mainIngredients[0]").value("ほうれん草"));
    }

    @Test
    void 提案_AI生成に失敗した場合_502が返る() throws Exception {
        // Arrange
        when(aiMenuService.suggest(eq(1L), any()))
                .thenThrow(new AiGenerationException("AI の呼び出しに失敗しました"));

        // Act & Assert
        mockMvc.perform(post("/api/menus/suggest")
                        .with(loginUser(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502));
    }

    @Test
    void 確定_正常系_201が返る() throws Exception {
        // Arrange
        when(menuConfirmService.confirm(eq(1L), any())).thenReturn(献立レスポンス());
        String body = """
                {
                  "cookedOn": "2026-09-08",
                  "mainDish": {"name": "鶏の照り焼き", "description": "甘辛い定番", "cookingMinutes": 25},
                  "sideDish": {"name": "ほうれん草のおひたし", "cookingMinutes": 10}
                }
                """;

        // Act & Assert
        mockMvc.perform(post("/api/menus/confirm")
                        .with(loginUser(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dishes[0].name").value("鶏の照り焼き"));
    }
}
