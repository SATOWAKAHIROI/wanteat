package com.example.wanteat.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignUpRequest(
    @NotBlank @Email String email,
    @NotBlank
    @Size(min = 8, max = 72, message = "パスワードは8文字以上72文字以内で入力してください")
    String password,
    @NotBlank String confirmPassword,
    /**
     * 招待コード。サーバー側で {@code app.signup.invite-code} が設定されている場合のみ必須。
     * 空の設定なら誰でも登録できる（開発時の既定）。
     */
    String inviteCode
) {
    @AssertTrue(message = "確認用パスワードが一致しません")
    public boolean isPasswordConfirmed(){
        return password != null && password.equals(confirmPassword);
    }
}
