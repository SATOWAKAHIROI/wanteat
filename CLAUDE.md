# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

`wanteat` is a full-stack web application with a Spring Boot backend and Next.js frontend, orchestrated via Docker Compose. JWT authentication uses HTTP-only cookies.

## Development Commands

### Docker (推奨: 全サービス起動)
```bash
# 全サービス起動 (MySQL + Backend + Frontend)
docker compose up

# バックグラウンド起動
docker compose up -d

# ビルドし直して起動
docker compose up --build
```

### Frontend (ローカル開発)
```bash
cd frontend
npm install
npm run dev      # localhost:3000
npm run build
npm run lint
```

### Backend (ローカル開発)
```bash
cd backend
./gradlew bootRun   # localhost:8080
./gradlew build
./gradlew test

# 単一テストクラスの実行
./gradlew test --tests "com.example.wanteat.SampleServiceTest"
```

## Architecture

### Stack
- **Backend**: Java 21, Spring Boot 4.1.0, Spring Security + JWT, Spring Data JPA, Flyway, MySQL 8.4
- **Frontend**: Next.js 16 (App Router), React 19, TypeScript 5, Tailwind CSS 4, shadcn/ui (base-nova style)
- **Infrastructure**: Docker Compose (MySQL → Backend → Frontend の依存順)

### Backend Package Structure (`com.example.wanteat`)
```
config/       - SecurityConfig (CORS, JWT filter, BCrypt)
controller/   - AuthController, SampleController (CRUD テンプレート)
service/      - AuthService, JwtService, SampleService
repository/   - UserRepository (findByEmail), SampleRepository
domain/       - User, Sample (@Entity)
dto/          - LoginRequest/Response, SampleRequest/Response (record型)
security/     - JwtAuthenticationFilter, LoginUser
exception/    - GlobalExceptionHandler, NotFoundException, ErrorResponse
```

### Frontend Structure (`src/`)
```
app/          - Next.js App Router ページ
components/ui/ - shadcn/ui コンポーネント (Button, Card, Input, Label)
lib/api.ts    - apiFetch ユーティリティ (credentials: "include" でCookie送信)
lib/utils.ts  - cn() クラスマージユーティリティ
```

## Authentication Flow

1. `POST /api/auth/login` → JWT をサーバーで生成し `token` という名前のHTTP-onlyクッキーにセット
2. 以降のリクエストは `apiFetch` が `credentials: "include"` でクッキーを自動送信
3. `JwtAuthenticationFilter` が各リクエストのクッキーからトークンを検証
4. `POST /api/auth/logout` → クッキーを削除

## Environment Variables

### Frontend (`.env.local`)
| 変数 | ローカル値 | Docker内の値 |
|------|-----------|-------------|
| `NEXT_PUBLIC_API_BASE_URL` | `http://localhost:8080` | `http://localhost:8080` |
| `API_BASE_URL` | `http://localhost:8080` | `http://backend:8080` |

### Backend (Docker Compose経由)
| 変数 | デフォルト |
|------|-----------|
| `DB_HOST` | `db` |
| `DB_PORT` | `3306` |
| `DB_NAME` | `wanteat` |
| `DB_USER` | `wanteat` |
| `DB_PASSWORD` | `wanteat` |
| `JWT_SECRET` | (application.yml参照) |

## Key Conventions

- **DBマイグレーション**: `backend/src/main/resources/db/migration/` にFlyway形式 (`V{n}__{description}.sql`) で追加
- **新規エンティティ追加**: `SampleController/Service/Repository/Domain` がCRUDテンプレートとして存在し、これを参考に実装する
- **APIクライアント**: フロントエンドからのAPI呼び出しは必ず `src/lib/api.ts` の `apiFetch` を使用する
- **UIコンポーネント**: shadcn/ui の base-nova スタイル。新規コンポーネントは `src/components/ui/` に追加
- **Tailwind**: グローバルテーマ変数は `src/app/globals.css` で管理
- **バックエンドテスト**: H2インメモリDBを使用 (`src/test/resources/application.yml`)
