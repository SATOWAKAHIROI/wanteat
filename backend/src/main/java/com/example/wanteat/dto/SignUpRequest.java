package com.example.wanteat.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SignUpRequest(
    @NotBlank @Email String email,
    @NotBlank String password,
    @NotBlank String confirmPassword
) {
    @AssertTrue(message = "確認用パスワードが一致しません")
    public boolean isPasswordConfirmed(){
        return password != null && password.equals(confirmPassword);
    }
}
