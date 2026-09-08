# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

`wanteat` は「今日の夕食を AI に決めてもらう」個人向け Web アプリ。Spring Boot バックエンド + Next.js フロントエンドを Docker Compose で動かす。認証は JWT を HTTP-only Cookie (`token`) に載せる方式。

要件定義は `docs/requirements.md` にある。**実装方針で迷ったら、まずそれを読むこと。** 設計原則は「選ばせない」で、AI は1案だけを提示し複数案を並列表示しない。

## Development Commands

### Docker（推奨）

```bash
docker compose up            # 全サービス起動
docker compose up -d db backend
docker compose logs -f backend
docker compose down -v       # ボリュームごと破棄（DB を作り直す）
```

ファイル名は `compose.yaml`（`docker-compose.yml` ではない）。

**compose は `backend/Dockerfile` を使わない。** backend は `eclipse-temurin:21-jdk` に `./backend` をバインドマウントして `./gradlew bootRun --no-daemon` を直接実行し、frontend は `node:22` で `npm install && npm run dev` を実行する。つまり **ホストに JDK / Node は不要**。`backend/Dockerfile` は本番用 bootJar ビルド専用で、現状 compose からは参照されていない。

### Gradle / npm の実行

backend が起動中なら `exec`、停止中なら `run --rm --no-deps` を使う。

```bash
docker compose exec backend ./gradlew test --no-daemon
docker compose run --rm --no-deps backend ./gradlew test --no-daemon --console=plain

# 単一テストクラス（パッケージ名まで含めた FQCN が必要）
docker compose run --rm --no-deps backend ./gradlew test --no-daemon \
  --tests "com.example.wanteat.service.MenuServiceTest"
```

**`bootRun` 中の backend コンテナに `exec` で別の Gradle を流すと Gradle のロックが競合することがある。** その場合は `docker compose stop backend` してから `run --rm` を使う。

Gradle キャッシュは `GRADLE_USER_HOME=/workspace/.gradle-home`（名前付きボリューム `gradle-cache`）に置かれる。

### Frontend

```bash
docker compose exec frontend npm run lint
```

## Architecture

### Stack

- **Backend**: Java 21 / Gradle 9.7.1 (Kotlin DSL) / Spring Boot **4.1.0** / Spring Security + jjwt 0.12.6 / Spring Data JPA / Flyway / Lombok / MySQL 8.4 / Anthropic Java SDK
- **Frontend**: Next.js **16** (App Router) / React 19 / TypeScript 5 / Tailwind CSS 4 / shadcn/ui (`base-nova` style, `@base-ui/react`)
- **Test**: JUnit 5 / Mockito / AssertJ / H2 インメモリ

### レイヤ構成 (`com.example.wanteat`)

`controller` → `service` → `repository` の標準構成に、**`mapper` 層**（エンティティ → レスポンス DTO の変換）が加わる。`mapper` は `@Component` のクラスとして作り、静的ファクトリメソッドは使わない。

DTO はすべて `record`。リクエスト DTO には Bean Validation（`@NotBlank` 等）を付け、**エンティティには付けない**（エンティティ側は `@Column(nullable = false)` で表現する）。

### ドメインモデル

```
users ─┬─ user_preferences (1:1)  好み設定（人数・アレルギー・苦手食材）
       ├─ menus (1:N)             献立。status: GENERATING/CONFIRMED/COOKED/FAILED
       │    └─ dishes (1:N)       主菜(MAIN)・副菜(SIDE)。steps は JSON 配列
       │         └─ ingredients   材料・分量・単位
       ├─ shopping_items (1:N)    買い物リスト。menu_id が NULL なら手動追加
       ├─ requests (1:N)          「食べたい」リクエスト。OPEN/FULFILLED
       └─ ai_generations (1:N)    AI 呼び出しログ（デバッグ・コスト把握）
```

**外部キーの持ち方に方針がある。**

- `Menu`⇄`Dish`⇄`Ingredient` は `@ManyToOne` / `@OneToMany` の関連で結ぶ（まとめて保存・削除する一体の塊のため）。`Menu#addDish()` / `Dish#addIngredient()` が関連の両側を同時にセットするので、**必ずこれを使う**（片側だけだと外部キーが NULL になって落ちる）
- `user` は全エンティティで `@ManyToOne User`。保存時は `userRepository.getReferenceById(userId)` を使い、不要な SELECT を避ける
- `ShoppingItem.menuId` と `Request.fulfilledMenuId` だけは**素の `Long`**。前者は NULL 許容（手動追加）、後者は DB 側の `ON DELETE SET NULL` を JPA が認識しないため、参照で持つとメモリと DB がズレる

**削除時の連鎖**（実機で検証済み）

- 献立を削除 → 料理・材料は JPA のカスケードで消える。献立由来の買い物アイテムは DB の `ON DELETE CASCADE` で消え、**手動追加分は残る**
- 献立を削除 → リクエストは消えず `fulfilled_menu_id` だけ NULL になる

### タイムスタンプ

`created_at` / `updated_at` は **`@PrePersist` / `@PreUpdate` でアプリ側が管理**する（Spring Data JPA Auditing は使っていない）。`@MappedSuperclass` の基底クラスも作っていないので、各エンティティに直接書く。

**注意**: JPQL の一括 UPDATE（`@Modifying @Query`）ではライフサイクルコールバックが走らず `updated_at` が古いまま残る。

## Spring Boot 4 の非互換に注意

訓練データの Spring Boot 3 系と**依存名・パッケージ・モジュール構成が変わっている**。

| 用途 | Boot 4 での正しい指定 |
|---|---|
| Web スターター | `spring-boot-starter-webmvc`（`-web` ではない） |
| MockMvc テスト | `spring-boot-starter-webmvc-test` / `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest` |
| JPA スライステスト | `spring-boot-starter-data-jpa-test` / `org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest` |
| Jackson | **Jackson 3**: `tools.jackson.databind.ObjectMapper`（`com.fasterxml.jackson.*` ではない） |

### ⚠️ 自動設定がモジュールに分割された

Boot 4 では技術ごとの自動設定が `spring-boot-<tech>` モジュールに分離された。**ライブラリ本体を依存に書くだけでは自動設定が有効にならない。**

```kotlin
implementation("org.springframework.boot:spring-boot-flyway")  // これが無いと Flyway は一切動かない
implementation("org.flywaydb:flyway-core")
```

`spring-boot-flyway` を忘れると **Flyway はログを一行も出さずに黙って何もしない**。症状は「マイグレーションが適用されず `Schema validation: missing table [...]` で起動失敗」となり、原因にたどり着きにくい。同種の問題が他のライブラリでも起きうるので、自動設定が効かないときは `spring-boot-<tech>` モジュールの有無を最初に疑うこと。

### ⚠️ Mockito が JDK 21 コンテナで初期化に失敗する

エージェントの自己アタッチが制限されるため、`build.gradle.kts` で `-javaagent` を明示している。**この設定を消すと全テストが `MockitoInitializationException` で落ちる。**

```kotlin
val mockitoAgent: Configuration by configurations.creating
// dependencies { mockitoAgent("org.mockito:mockito-core") { isTransitive = false } }

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    val agent = configurations.named("mockitoAgent")
    jvmArgumentProviders.add(CommandLineArgumentProvider {
        listOf("-javaagent:${agent.get().asPath}")
    })
}
```

### ⚠️ `@WebMvcTest` が SecurityConfig を読み込まない

そのままだと Spring Security が適用されず、`@AuthenticationPrincipal LoginUser` が解決されない。**`LoginUser` が `@ModelAttribute` として全フィールド null で束縛され、NPE も出ないまま誤動作する。** Controller テストには必ず次を付ける。

```java
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class })
@WebMvcTest(XxxController.class)
class XxxControllerTest {
    @MockitoBean private JwtService jwtService;   // JwtAuthenticationFilter の依存
}
```

認証済みリクエストは `com.example.wanteat.support.TestAuth.loginUser(userId)` を `.with(...)` に渡す。

### Next.js 16

フロントのコードを書く前に `frontend/AGENTS.md` の指示に従い `frontend/node_modules/next/dist/docs/` の該当ガイドを読むこと（`frontend/CLAUDE.md` は `@AGENTS.md` を読み込むだけのファイル）。

## DB スキーマと Flyway

- マイグレーションは `backend/src/main/resources/db/migration/V{n}__{description}.sql`
- `spring.jpa.hibernate.ddl-auto: validate` のため、**エンティティに対応する表が無いと起動時に落ちる**。新規 `@Entity` を追加したら必ず同じコミットでマイグレーションを追加する
- **一度適用したマイグレーションは編集しない。** チェックサム不一致で起動できなくなる。開発中に作り直したい場合は `docker compose down -v`
- ユーザー登録 API は無い。テストユーザーは BCrypt ハッシュを `users` に直接 INSERT する

```bash
# BCrypt ハッシュの生成（ホストに python3 + bcrypt がある場合）
python3 -c "import bcrypt; print(bcrypt.hashpw(b'password', bcrypt.gensalt(10, prefix=b'2a')).decode())"
```

## Authentication Flow

1. `POST /api/auth/login` → `AuthService` が BCrypt 照合 → `JwtService` が JWT 生成 → `AuthController` が HTTP-only Cookie `token` にセット（`secure(false)` / `SameSite=Lax` / `maxAge=3600`）
2. 以降は `apiFetch` が `credentials: "include"` で Cookie を自動送信
3. `JwtAuthenticationFilter` が Cookie からトークンを取り出し `SecurityContext` に `LoginUser` をセット
4. `POST /api/auth/logout` → maxAge 0 の Cookie で削除

実装上の癖:

- **`JwtAuthenticationFilter` は自力で 401 を返さない。** トークンが無い／不正なら認証情報を入れずにチェーンを通すだけで、拒否は `SecurityConfig` の `authorizeHttpRequests` + `authenticationEntryPoint` が担当する。期限切れ時のみ Cookie 削除ヘッダを付与する
- **principal は `UserDetails` ではなく `LoginUser` レコード**（`userId` / `email` / `role`）。コントローラでは `@AuthenticationPrincipal LoginUser loginUser` で受け、`loginUser.userId()` を Service に渡す。`UserDetailsService` は存在しない
- **全 API は自分のデータのみ操作できる**よう、Service 側で `findByIdAndUserId` 系を使う。他人のリソースは 404 を返す（403 ではない）
- CSRF は無効。`/api/auth/**` のみ permitAll、それ以外は全て認証必須
- CORS は `http://localhost:3000` のみ許可 + `allowCredentials(true)`
- **`SecurityConfig#corsConfigurationSource` の `setAllowedMethods` は列挙式**。新しい HTTP メソッドを使う API を足したら必ずここも更新する。漏れるとプリフライトが 403 になり、ブラウザ側は `Failed to fetch`（HTTP エラーですらない）になって原因が分かりにくい
- Cookie の `maxAge`(2592000秒 = 30日) と `jwt.expiration-ms`(2592000000) は手動で揃えている。**片方だけ変えても症状が出ないので注意**：Cookie だけ延ばしてもブラウザが送り続けるだけで、JWT が期限切れなら `JwtAuthenticationFilter` が認証情報を入れず 401 になり、結局ログアウトさせられる
- **Cookie を作っている箇所は3つ**（`AuthController` のログイン / サインアップ / ログアウト）。有効期限を変えるときは全部を確認する。検証は実際にトークンを復号するのが確実

```bash
# 発行されたトークンの有効期間を確認する
curl -s -c /tmp/c.txt -o /dev/null -X POST -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password"}' http://localhost:8080/api/auth/login
python3 -c "
import base64, json, datetime, subprocess
tok = subprocess.check_output(['awk', '/token/ {print \$7}', '/tmp/c.txt']).decode().strip()
p = tok.split('.')[1]; p += '=' * (-len(p) % 4)
d = json.loads(base64.urlsafe_b64decode(p))
print(datetime.datetime.fromtimestamp(d['exp']) - datetime.datetime.fromtimestamp(d['iat']))"
```

- **JWT はステートレスなので、ログアウトしてもサーバー側で失効させられない。** Cookie を消すだけ。有効期限を30日にしたことで、トークンが漏れた場合の影響範囲も30日になる。外部公開時はリフレッシュトークンや失効リストの導入を検討すること
- **`application.yml` を変えても再起動しないと反映されない**（`docker compose restart backend`）。ソース変更と同じ

## エラーレスポンス契約

`GlobalExceptionHandler` が全例外を `ErrorResponse(status, message, errors)` に正規化する:

- `NotFoundException` → 404
- `IllegalArgumentException` → 400
- `MethodArgumentNotValidException` → 400 + `errors` にフィールド名→メッセージの Map
- `AiGenerationException` → 502（AI 生成の失敗。フロントで再試行導線を出す）
- その他 → 500（詳細はログのみ）

フロントの `apiFetch` は非 2xx 時に `error.message` を `Error` として throw する。新しい例外を足すときはこの形を崩さない。

## API エンドポイント

```
POST   /api/auth/login          POST /api/auth/logout
GET    /api/preferences         PUT  /api/preferences
POST   /api/menus/suggest       第1段階: AI が主菜＋副菜を1案提示（DB 書き込みなし）
POST   /api/menus/confirm       第2段階: AI が材料・手順を生成して確定
POST   /api/menus               手動確定（AI 不使用）
GET    /api/menus               GET  /api/menus/{id}
PATCH  /api/menus/{id}/cooked   DELETE /api/menus/{id}
GET    /api/shopping-items      POST /api/shopping-items
PATCH  /api/shopping-items/{id} DELETE /api/shopping-items/{id}
DELETE /api/shopping-items/checked
GET    /api/requests            POST /api/requests
PATCH  /api/requests/{id}       DELETE /api/requests/{id}
```

献立を確定すると**材料が買い物リストへ自動展開される**（`MenuService#expandToShoppingList`）。同じ食材の名寄せは意図的に行わない。

## AI 連携

### ⚠️ `outputConfig` は併用できない（後勝ち）

`.outputConfig(Class)`（型付き構造化出力）と `.outputConfig(OutputConfig)`（`effort` 指定など）は
**同じ設定枠を奪い合い、後に呼んだ方が勝つ**。両方を書くとスキーマ指定が消え、
AI が JSON ではなく Markdown を返して `AnthropicInvalidDataException: Error parsing JSON` で落ちる。

```java
.outputConfig(SuggestResponse.class)                              // これだけにする
// .outputConfig(OutputConfig.builder().effort(...).build())      // ← 併記すると上記が無効化される
```

`effort` を使いたい場合は、型付き API を諦めて `OutputConfig` に `format` と `effort` の
両方を手で設定し、レスポンスの JSON も自前でパースする必要がある。
現状は**型付き API を優先し、`effort` は使っていない**（応答速度より実装の安全性を取った）。


**AI の呼び出しは必ずバックエンドから行う。** API キーをフロントに露出させない。

- `AiMenuService` が Anthropic Java SDK 経由で Claude を呼ぶ
- **2段階生成**: 第1段階（`SUGGEST`）は料理名・説明・調理時間・主な材料のみ、第2段階（`DETAIL`）は確定した1件だけ材料・分量・手順を生成する。「別の案」を何度押しても軽い生成しか走らない設計
- **実測の応答時間**: 第1段階 約13秒 / 第2段階 約19〜23秒。`effort` が使えないため当初想定（3〜5秒）には届かない。**ローディング表示は必須**
- **常備品**: 第2段階は材料に `pantryStaple` を付けて返す。水・塩・油・調味料などは true になり、レシピには表示するが買い物リストには展開しない（`MenuService#expandToShoppingList` で除外）
- **分量表記**: 「大さじ」「小さじ」「少々」は AI が `amount`/`unit` に分けて返すことがあるため、`MenuConfirmService#toIngredientRequest` で「小さじ1」の形に確定的に正規化している
- 構造化出力は SDK の型付き API（`.outputConfig(MyRecord.class)`）を使う。自由文のパースはしない
- 呼び出しは `ai_generations` テーブルに記録する（トークン数・レイテンシ・モデル名）
- プロンプトには好み設定・直近14日の献立名・未消化リクエスト・却下された料理名を渡す

モデルは `claude-opus-5`。環境変数 `AI_MODEL` で切り替えられる。

## テスト方針

- テスト設定は `backend/src/test/resources/application.yml`。メインの同名ファイルをクラスパスごと差し替えるため、**プロファイル指定なしで H2 / `ddl-auto: create-drop` / Flyway 無効になる**
- **テストは H2 がエンティティから DDL を生成する。** `@Column(nullable=false, length=...)` を書き忘れると本番 MySQL とテストでスキーマがずれる
- 既存の作法に合わせる: **テストメソッド名は日本語**（`findById_他人の献立の場合_NotFoundExceptionをスロー`）、本文は `// Arrange` `// Act` `// Assert` でブロック分け
- `@WebMvcTest` では POST/PUT/DELETE に `.with(csrf())` が必要な場合がある
- **AI を実際に呼ぶテストは書かない**（課金が発生し結果も非決定的）。`AiMenuService` をモックする

## Environment Variables

compose 起動時は `compose.yaml` が注入する。`ANTHROPIC_API_KEY` だけはリポジトリに含めないため、**ルートの `.env` に書く**（`.gitignore` 済み。雛形は `.env.example`）。

### Backend

| 変数 | compose の値 | 備考 |
|---|---|---|
| `DB_HOST` / `DB_PORT` / `DB_NAME` | `db` / `3306` / `wanteat` | |
| `DB_USER` / `DB_PASSWORD` | `appuser` / `apppass` | MySQL の root は `rootpass` |
| `JWT_SECRET` | dev 用の固定値 | HMAC-SHA 鍵なので 256bit(32バイト)以上必要 |
| `ANTHROPIC_API_KEY` | `.env` から | **未設定でもアプリは起動する**（AI 呼び出し時にエラー） |
| `AI_MODEL` | `claude-opus-5` | |

### Frontend

| 変数 | compose | 備考 |
|---|---|---|
| `NEXT_PUBLIC_API_BASE_URL` | `http://localhost:8080` | ブラウザから叩くので localhost のまま |
| `API_BASE_URL` | `http://backend:8080` | サーバーサイド用。`lib/api.ts` は未参照 |

## Frontend

- **API 呼び出しは必ず `src/lib/api.ts` の `apiFetch` を使う**（`credentials: "include"` と 204 ハンドリングが入っている）
- UI は shadcn/ui `base-nova` スタイル。テーマ変数は `src/app/globals.css` の `@theme inline` ブロックで管理（Tailwind 4 の CSS-first 設定、`tailwind.config` は無い）
- パスエイリアスは `@/*` → `src/*`
- スマホ縦画面が第一。電車内とスーパー店内での利用を想定している
