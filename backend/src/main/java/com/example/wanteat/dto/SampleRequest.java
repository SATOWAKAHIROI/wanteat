package com.example.wanteat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// TODO: アプリのドメインに合わせてリネーム・修正する
public record SampleRequest(
    @NotBlank @Size(max = 100) String name
    // TODO: フィールドを追加する
) {}
