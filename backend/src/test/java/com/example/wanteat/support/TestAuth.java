package com.example.wanteat.support;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.example.wanteat.security.LoginUser;

/** テストで @AuthenticationPrincipal LoginUser を注入するためのヘルパー。 */
public final class TestAuth {

    private TestAuth() {
    }

    public static RequestPostProcessor loginUser(Long userId) {
        LoginUser loginUser = new LoginUser(userId, "test@example.com", "USER");
        return authentication(new UsernamePasswordAuthenticationToken(
                loginUser, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }
}
