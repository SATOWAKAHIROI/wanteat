package com.example.wanteat.controller;

import static com.example.wanteat.support.TestAuth.loginUser;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.wanteat.dto.ShoppingItemResponse;
import com.example.wanteat.config.SecurityConfig;
import com.example.wanteat.security.JwtAuthenticationFilter;
import com.example.wanteat.security.TokenCookieFactory;
import com.example.wanteat.service.JwtService;
import com.example.wanteat.service.ShoppingItemService;

@Import({ SecurityConfig.class, JwtAuthenticationFilter.class, TokenCookieFactory.class })
@WebMvcTest(ShoppingItemController.class)
class ShoppingItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ShoppingItemService shoppingItemService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void 一覧取得_献立由来と手動追加が混在して返る() throws Exception {
        // Arrange
        when(shoppingItemService.findAll(1L)).thenReturn(List.of(
                new ShoppingItemResponse(1L, 10L, "鶏もも肉", "300", "g", false, 1),
                new ShoppingItemResponse(2L, null, "食器用洗剤", null, null, false, 2)));

        // Act & Assert
        mockMvc.perform(get("/api/shopping-items").with(loginUser(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].menuId").value(10))
                .andExpect(jsonPath("$[1].menuId").doesNotExist())
                .andExpect(jsonPath("$[1].name").value("食器用洗剤"));
    }

    @Test
    void 手動追加_正常系_201が返る() throws Exception {
        // Arrange
        when(shoppingItemService.create(eq(1L), any()))
                .thenReturn(new ShoppingItemResponse(3L, null, "牛乳", "1", "本", false, 3));

        // Act & Assert
        mockMvc.perform(post("/api/shopping-items")
                        .with(loginUser(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"牛乳\",\"amount\":\"1\",\"unit\":\"本\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(3));
    }

    @Test
    void 手動追加_バリデーションエラー_名前が空() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/shopping-items")
                        .with(loginUser(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void チェック切替_正常系() throws Exception {
        // Arrange
        when(shoppingItemService.toggleChecked(1L, 5L))
                .thenReturn(new ShoppingItemResponse(5L, null, "牛乳", null, null, true, 1));

        // Act & Assert
        mockMvc.perform(patch("/api/shopping-items/5").with(loginUser(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checked").value(true));
    }

    @Test
    void 購入済み一括削除_正常系_204が返る() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/api/shopping-items/checked").with(loginUser(1L)))
                .andExpect(status().isNoContent());
        verify(shoppingItemService).deleteChecked(1L);
    }
}
