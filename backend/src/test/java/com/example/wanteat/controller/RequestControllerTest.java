package com.example.wanteat.controller;

import static com.example.wanteat.support.TestAuth.loginUser;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.wanteat.domain.RequestStatus;
import com.example.wanteat.domain.RequestedBy;
import com.example.wanteat.dto.RequestResponse;
import com.example.wanteat.config.SecurityConfig;
import com.example.wanteat.security.JwtAuthenticationFilter;
import com.example.wanteat.service.JwtService;
import com.example.wanteat.service.RequestService;

@Import({ SecurityConfig.class, JwtAuthenticationFilter.class })
@WebMvcTest(RequestController.class)
class RequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RequestService requestService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void 作成_正常系_201が返る() throws Exception {
        // Arrange
        when(requestService.create(eq(1L), any())).thenReturn(new RequestResponse(
                1L, RequestedBy.PARTNER, "カレーが食べたい", RequestStatus.OPEN, null,
                LocalDateTime.now(), null));

        // Act & Assert
        mockMvc.perform(post("/api/requests")
                        .with(loginUser(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requestedBy\":\"PARTNER\",\"body\":\"カレーが食べたい\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.requestedBy").value("PARTNER"));
    }

    @Test
    void 作成_バリデーションエラー_内容が空() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/requests")
                        .with(loginUser(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requestedBy\":\"SELF\",\"body\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.body").exists());
    }

    @Test
    void 一覧取得_未消化で絞り込める() throws Exception {
        // Arrange
        when(requestService.findAll(1L, RequestStatus.OPEN)).thenReturn(List.of(new RequestResponse(
                1L, RequestedBy.SELF, "魚料理", RequestStatus.OPEN, null, LocalDateTime.now(), null)));

        // Act & Assert
        mockMvc.perform(get("/api/requests").param("status", "OPEN").with(loginUser(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].body").value("魚料理"));
    }
}
