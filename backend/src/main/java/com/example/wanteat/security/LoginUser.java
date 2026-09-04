package com.example.wanteat.security;

// JWTから復元したログインユーザー情報を保持するレコード（UserDetailsは実装しない）
public record LoginUser(
    Long userId,
    String email,
    String role
) {}
