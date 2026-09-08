package com.example.wanteat.controller;

import com.example.wanteat.dto.LoginRequest;
import com.example.wanteat.dto.SignUpRequest;
import com.example.wanteat.security.TokenCookieFactory;
import com.example.wanteat.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final TokenCookieFactory tokenCookieFactory;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid LoginRequest request) {
        String token = authService.login(request).token();
        return withCookie(tokenCookieFactory.create(token));
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody @Valid SignUpRequest request) {
        String token = authService.signUp(request).token();
        return withCookie(tokenCookieFactory.create(token));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        return withCookie(tokenCookieFactory.expired());
    }

    /** 本文は空で Set-Cookie だけを返す。トークンはレスポンスボディに載せない。 */
    private ResponseEntity<?> withCookie(ResponseCookie cookie) {
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString()).build();
    }
}
