package com.example.wanteat.controller;

import java.util.List;

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
import org.springframework.web.bind.annotation.RestController;

import com.example.wanteat.dto.ShoppingItemRequest;
import com.example.wanteat.dto.ShoppingItemResponse;
import com.example.wanteat.security.LoginUser;
import com.example.wanteat.service.ShoppingItemService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/shopping-items")
@RequiredArgsConstructor
public class ShoppingItemController {

    private final ShoppingItemService shoppingItemService;

    @GetMapping
    public ResponseEntity<List<ShoppingItemResponse>> findAll(
            @AuthenticationPrincipal LoginUser loginUser) {
        return ResponseEntity.ok(shoppingItemService.findAll(loginUser.userId()));
    }

    @PostMapping
    public ResponseEntity<ShoppingItemResponse> create(
            @AuthenticationPrincipal LoginUser loginUser,
            @Valid @RequestBody ShoppingItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(shoppingItemService.create(loginUser.userId(), request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ShoppingItemResponse> toggleChecked(
            @AuthenticationPrincipal LoginUser loginUser,
            @PathVariable Long id) {
        return ResponseEntity.ok(shoppingItemService.toggleChecked(loginUser.userId(), id));
    }

    @DeleteMapping("/checked")
    public ResponseEntity<Void> deleteChecked(@AuthenticationPrincipal LoginUser loginUser) {
        shoppingItemService.deleteChecked(loginUser.userId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal LoginUser loginUser,
            @PathVariable Long id) {
        shoppingItemService.delete(loginUser.userId(), id);
        return ResponseEntity.noContent().build();
    }
}
