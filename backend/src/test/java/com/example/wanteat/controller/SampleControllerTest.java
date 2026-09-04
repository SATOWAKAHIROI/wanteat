package com.example.wanteat.controller;

import tools.jackson.databind.ObjectMapper;
import com.example.wanteat.dto.SampleRequest;
import com.example.wanteat.dto.SampleResponse;
import com.example.wanteat.service.SampleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// TODO: アプリのドメインに合わせてリネーム・修正する
@WebMvcTest(SampleController.class)
class SampleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SampleService sampleService;

    @Test
    @WithMockUser
    void 一覧取得_正常系() throws Exception {
        // Arrange
        var response = new SampleResponse(1L, "テスト", LocalDateTime.now());
        when(sampleService.findAll()).thenReturn(List.of(response));

        // Act & Assert
        mockMvc.perform(get("/api/samples"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("テスト"));
    }

    @Test
    @WithMockUser
    void 作成_正常系() throws Exception {
        // Arrange
        var request = new SampleRequest("新しいサンプル");
        var response = new SampleResponse(1L, "新しいサンプル", LocalDateTime.now());
        when(sampleService.create(any())).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/samples")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("新しいサンプル"));
    }

    @Test
    @WithMockUser
    void 作成_バリデーションエラー_名前が空() throws Exception {
        // Arrange
        var request = new SampleRequest("");

        // Act & Assert
        mockMvc.perform(post("/api/samples")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
