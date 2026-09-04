# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

`wanteat` は Spring Boot バックエンド + Next.js フロントエンドのフルスタック雛形。Docker Compose で MySQL → Backend → Frontend の順に起動する。認証は JWT を HTTP-only Cookie (`token`) に載せる方式。

`Sample*` 一式（Controller/Service/Repository/Domain/DTO/テスト）は **CRUD テンプレート** で、全ファイルに `// TODO: アプリのドメインに合わせてリネーム・修正する` が付いている。新規エンティティはこれを複製して実装する。

## Development Commands

### Docker（推奨）

```bash
docker compose up            # 全サービス起動
docker compose up -d         # バックグラウンド
docker compose logs -f backend
```

ファイル名は `compose.yaml`（`docker-compose.yml` ではない）。

**重要: compose は `backend/Dockerfile` を使わない。** backend は `eclipse-temurin:21-jdk` に `./backend` をバインドマウントして `./gradlew bootRun --no-daemon` を直接実行し、frontend は `node:22` で `npm install && npm run dev` を実行する。つまり **ホストに JDK / Node は不要**。`backend/Dockerfile` は本番用 bootJar ビルド専用で、現状 compose からは参照されていない。

コンテナ内で Gradle / npm を叩く:

```bash
docker compose exec backend ./gradlew test --no-daemon
docker compose exec backend ./gradlew build --no-daemon
docker compose exec frontend npm run lint
```

Gradle キャッシュは `GRADLE_USER_HOME=/workspace/.gradle-home`（名前付きボリューム `gradle-cache`）に置かれる。

### Backend（ホストに JDK 21 がある場合のみ）

```bash
cd backend
./gradlew bootRun      # localhost:8080
./gradlew build
./gradlew test

# 単一テストクラス（パッケージ名まで含めた FQCN が必要）
./gradlew test --tests "com.example.wanteat.service.SampleServiceTest"
```

### Frontend（ホストに Node がある場合のみ）

```bash
cd frontend
npm install
npm run dev      # localhost:3000
npm run build
npm run lint     # eslint（flat config: eslint.config.mjs）
```

## Architecture

### Stack

- **Backend**: Java 21 / Gradle 9.7.1 (Kotlin DSL) / Spring Boot **4.1.0** / Spring Security + jjwt 0.12.6 / Spring Data JPA / Flyway / Lombok / MySQL 8.4
- **Frontend**: Next.js **16** (App Router) / React 19 / TypeScript 5 / Tailwind CSS 4 / shadcn/ui (`base-nova` style, `@base-ui/react`)
- **Test**: JUnit 5 / Mockito / AssertJ / H2 インメモリ

### Backend パッケージ (`com.example.wanteat`)

`config/` `controller/` `service/` `repository/` `domain/` `dto/`(record) `security/` `exception/` の標準レイヤ構成。詳細は `ls` で確認できるためここでは列挙しない。

### Spring Boot 4 の非互換に注意

訓練データの Spring Boot 3 系と **依存名・パッケージが変わっている**。既存コードに合わせること:

| 用途 | Boot 4 での正しい指定 |
|---|---|
| Web スターター | `spring-boot-starter-webmvc`（`-web` ではない） |
| MockMvc テスト | `spring-boot-starter-webmvc-test` / `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest` |
| JPA スライステスト | `spring-boot-starter-data-jpa-test` / `org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest` |
| Jackson | **Jackson 3**: `tools.jackson.databind.ObjectMapper`（`com.fasterxml.jackson.*` ではない） |

Next.js 16 も同様に破壊的変更があるため、フロントのコードを書く前に `frontend/AGENTS.md` の指示に従い `frontend/node_modules/next/dist/docs/` の該当ガイドを読むこと（`frontend/CLAUDE.md` は `@AGENTS.md` を読み込むだけのファイル）。

## DB スキーマと Flyway

- マイグレーションは `backend/src/main/resources/db/migration/V{n}__{description}.sql`
- `spring.jpa.hibernate.ddl-auto: validate` のため、**エンティティに対応する表が無いと起動時に例外で落ちる**。新規 `@Entity` を追加したら必ず同じコミットで Flyway マイグレーションを追加する。
- **既知の状態**: `V1__init.sql` は `users` しか作らず、`samples` テーブルは雛形としてコメントアウトされたまま。`Sample` エンティティが残っている限り backend は起動に失敗する。`Sample*` を実ドメインにリネームする際に、対応するテーブルを `V2__*.sql` 等で作ること。
- ユーザー登録 API は存在しない。初期ユーザーは BCrypt ハッシュ化したパスワードを `users` に直接 INSERT する（`V1__init.sql` 末尾のコメント参照）。

## Authentication Flow

1. `POST /api/auth/login` → `AuthService` が BCrypt 照合 → `JwtService` が JWT 生成 → `AuthController` が HTTP-only Cookie `token` にセット（`secure(false)` / `SameSite=Lax` / `maxAge=3600`）
2. 以降は `apiFetch` が `credentials: "include"` で Cookie を自動送信
3. `JwtAuthenticationFilter` が Cookie からトークンを取り出し `SecurityContext` に `LoginUser` をセット
4. `POST /api/auth/logout` → maxAge 0 の Cookie で削除

実装上の癖:

- **`JwtAuthenticationFilter` は自力で 401 を返さない。** トークンが無い／不正なら認証情報を入れずにチェーンを通すだけで、拒否は `SecurityConfig` の `authorizeHttpRequests` + `authenticationEntryPoint` が担当する。期限切れ時のみ Cookie 削除ヘッダを付与する。
- **principal は `UserDetails` ではなく `LoginUser` レコード**（`userId` / `email` / `role`）。コントローラでは `@AuthenticationPrincipal LoginUser user` で受ける。`UserDetailsService` は存在しない。
- 権限は `ROLE_ + role` の `SimpleGrantedAuthority` として付与される。
- CSRF は無効。`/api/auth/**` のみ permitAll、それ以外は全て認証必須。
- CORS は `http://localhost:3000` のみ許可 + `allowCredentials(true)`。オリジンを増やす場合は `SecurityConfig#corsConfigurationSource` を変更する。
- Cookie の `maxAge`(3600秒) と `jwt.expiration-ms`(3600000) は手動で揃えている。片方だけ変えないこと。

## エラーレスポンス契約

`GlobalExceptionHandler` が全例外を `ErrorResponse(status, message, errors)` に正規化する:

- `NotFoundException` → 404
- `IllegalArgumentException` → 400（ログイン時のパスワード不一致もこれ）
- `MethodArgumentNotValidException` → 400 + `errors` にフィールド名→メッセージの Map
- その他 → 500（`Internal Server Error`、詳細はログのみ）

フロントの `apiFetch` は非 2xx 時に `error.message` を `Error` として throw する。新しい例外を足すときはこの形を崩さない。

## Frontend

- **API 呼び出しは必ず `src/lib/api.ts` の `apiFetch` を使う**（`credentials: "include"` と 204 ハンドリングが入っている）
- `apiFetch` は `NEXT_PUBLIC_API_BASE_URL` **のみ**を参照する。`API_BASE_URL`（= `http://backend:8080`）は compose で注入されているが現状どこからも使われていない。サーバーコンポーネント／Route Handler からバックエンドを呼ぶ場合は、コンテナ間通信用にこちらを使う実装を別途書く必要がある。
- UI は shadcn/ui `base-nova` スタイル。新規コンポーネントは `src/components/ui/` に追加。テーマ変数は `src/app/globals.css` の `@theme inline` ブロックで管理（Tailwind 4 の CSS-first 設定、`tailwind.config` は無い）。
- パスエイリアスは `@/*` → `src/*`。

## テスト方針

- テスト設定は `backend/src/test/resources/application.yml`。メインの同名ファイルをクラスパスごと差し替えるため、**プロファイル指定なしで H2 / `ddl-auto: create-drop` / Flyway 無効になる**。
- 3層それぞれに雛形がある: `SampleServiceTest`（Mockito 単体）、`SampleControllerTest`（`@WebMvcTest` + `@MockitoBean` + `@WithMockUser`）、`SampleRepositoryTest`（`@DataJpaTest`）。
- 既存の作法に合わせる: **テストメソッド名は日本語**（`findById_存在しない場合_NotFoundExceptionをスロー`）、本文は `// Arrange` `// Act` `// Assert` でブロック分け。
- `@WebMvcTest` では Security が有効なので `@WithMockUser` と、POST/PUT/DELETE には `.with(csrf())` が必要（本番設定では CSRF 無効だがテストスライスでは有効になるため）。

## Environment Variables

compose 起動時は `compose.yaml` が全て注入するので `.env` 系ファイルは不要（リポジトリにも存在しない）。ホストで直接起動する場合のみ `frontend/.env.local` を自作する。

### Backend

| 変数 | compose の値 | `application.yml` のデフォルト |
|---|---|---|
| `DB_HOST` | `db` | `db` |
| `DB_PORT` | `3306` | `3306` |
| `DB_NAME` | `wanteat` | `wanteat` |
| `DB_USER` | `appuser` | `appuser` |
| `DB_PASSWORD` | `apppass` | `apppass` |
| `JWT_SECRET` | `dev-secret-key-please-change-in-production-env` | プレースホルダ（本番で要変更） |

`JWT_SECRET` は HMAC-SHA 鍵に使われるため 256bit（32バイト）以上必要。MySQL の root は `rootpass`、ポート 3306 はホストに公開されている。

### Frontend

| 変数 | ローカル | compose |
|---|---|---|
| `NEXT_PUBLIC_API_BASE_URL` | `http://localhost:8080` | `http://localhost:8080`（ブラウザから叩くので localhost のまま） |
| `API_BASE_URL` | `http://localhost:8080` | `http://backend:8080`（サーバーサイド用・現状未使用） |
