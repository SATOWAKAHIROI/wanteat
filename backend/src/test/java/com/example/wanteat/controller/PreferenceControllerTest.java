package com.example.wanteat.controller;

import static com.example.wanteat.support.TestAuth.loginUser;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.wanteat.dto.PreferenceResponse;
import com.example.wanteat.config.SecurityConfig;
import com.example.wanteat.security.JwtAuthenticationFilter;
import com.example.wanteat.service.JwtService;
import com.example.wanteat.service.UserPreferenceService;

import tools.jackson.databind.ObjectMapper;

@Import({ SecurityConfig.class, JwtAuthenticationFilter.class })
@WebMvcTest(PreferenceController.class)
class PreferenceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserPreferenceService userPreferenceService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void 取得_正常系() throws Exception {
        // Arrange
        when(userPreferenceService.find(1L))
                .thenReturn(new PreferenceResponse(1L, 2, "そば", "パクチー", "和食多め", LocalDateTime.now()));

        // Act & Assert
        mockMvc.perform(get("/api/preferences").with(loginUser(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.householdSize").value(2))
                .andExpect(jsonPath("$.allergies").value("そば"));
    }

    @Test
    void 更新_正常系() throws Exception {
        // Arrange
        when(userPreferenceService.save(eq(1L), any()))
                .thenReturn(new PreferenceResponse(1L, 3, null, null, null, LocalDateTime.now()));

        // Act & Assert
        mockMvc.perform(put("/api/preferences")
                        .with(loginUser(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"householdSize\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.householdSize").value(3));
    }

    @Test
    void 更新_バリデーションエラー_世帯人数が0() throws Exception {
        // Act & Assert
        mockMvc.perform(put("/api/preferences")
                        .with(loginUser(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"householdSize\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.householdSize").exists());
    }

    @Test
    void 取得_未認証の場合_401() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/preferences"))
                .andExpect(status().isUnauthorized());
    }
}
