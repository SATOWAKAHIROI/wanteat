package com.example.wanteat.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.wanteat.dto.MenuConfirmRequest;
import com.example.wanteat.dto.MenuCreateRequest;
import com.example.wanteat.dto.MenuResponse;
import com.example.wanteat.dto.SuggestRequest;
import com.example.wanteat.dto.SuggestResponse;
import com.example.wanteat.security.LoginUser;
import com.example.wanteat.service.AiMenuService;
import com.example.wanteat.service.MenuConfirmService;
import com.example.wanteat.service.MenuService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/menus")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;
    private final AiMenuService aiMenuService;
    private final MenuConfirmService menuConfirmService;

    /** 第1段階。AI が主菜と副菜を1品ずつ提案する。DB には書き込まない。 */
    @PostMapping("/suggest")
    public ResponseEntity<SuggestResponse> suggest(
            @AuthenticationPrincipal LoginUser loginUser,
            @Valid @RequestBody(required = false) SuggestRequest request) {
        return ResponseEntity.ok(aiMenuService.suggest(loginUser.userId(), request));
    }

    /** 第2段階。提案を受けて AI が材料と手順を生成し、献立を確定する。 */
    @PostMapping("/confirm")
    public ResponseEntity<MenuResponse> confirm(
            @AuthenticationPrincipal LoginUser loginUser,
            @Valid @RequestBody MenuConfirmRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(menuConfirmService.confirm(loginUser.userId(), request));
    }

    @PostMapping
    public ResponseEntity<MenuResponse> create(
            @AuthenticationPrincipal LoginUser loginUser,
            @Valid @RequestBody MenuCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(menuService.create(loginUser.userId(), request));
    }

    @GetMapping
    public ResponseEntity<List<MenuResponse>> findAll(
            @AuthenticationPrincipal LoginUser loginUser,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(menuService.findAll(loginUser.userId(), from, to));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MenuResponse> findById(
            @AuthenticationPrincipal LoginUser loginUser,
            @PathVariable Long id) {
        return ResponseEntity.ok(menuService.findById(loginUser.userId(), id));
    }

    @PatchMapping("/{id}/cooked")
    public ResponseEntity<MenuResponse> markCooked(
            @AuthenticationPrincipal LoginUser loginUser,
            @PathVariable Long id) {
        return ResponseEntity.ok(menuService.markCooked(loginUser.userId(), id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal LoginUser loginUser,
            @PathVariable Long id) {
        menuService.delete(loginUser.userId(), id);
        return ResponseEntity.noContent().build();
    }
}
