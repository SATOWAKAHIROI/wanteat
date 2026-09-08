package com.example.wanteat.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 認証 Cookie の生成を1箇所に集約する。
 *
 * <p>以前はログイン・サインアップ・ログアウト・期限切れ処理の4箇所で個別に組み立てており、
 * 有効期限や secure 属性を片方だけ変えて不整合を起こす事故が実際に発生した。
 * 有効期限は {@code jwt.expiration-ms} から導出するため、JWT 本体とズレることがない。
 */
@Component
public class TokenCookieFactory {

    public static final String COOKIE_NAME = "token";

    /** 本番（HTTPS）では true。false のままだと Cookie が送信されずログインできない。 */
    @Value("${app.cookie.secure:false}")
    private boolean secure;

    @Value("${app.cookie.same-site:Lax}")
    private String sameSite;

    /** フロントと API でサブドメインが分かれる場合のみ指定する（例: .example.com）。 */
    @Value("${app.cookie.domain:}")
    private String domain;

    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    /** トークンを載せた Cookie。有効期限は JWT と同じになる。 */
    public ResponseCookie create(String token) {
        return build(token, expirationMs / 1000);
    }

    /** 削除用の Cookie。生成時と同じ属性でないとブラウザが消してくれない。 */
    public ResponseCookie expired() {
        return build("", 0);
    }

    private ResponseCookie build(String value, long maxAgeSeconds) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .maxAge(maxAgeSeconds)
                .sameSite(sameSite);

        if (StringUtils.hasText(domain)) {
            builder.domain(domain);
        }
        return builder.build();
    }
}
