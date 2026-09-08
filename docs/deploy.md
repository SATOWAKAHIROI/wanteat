# デプロイ手順（VPS）

フロント・バックエンド・DB を1台の VPS に同居させ、Caddy が HTTPS を終端して
同一ドメイン上で振り分ける構成。

```
              https://example.com
                      │
                  [ Caddy ]     HTTPS を自動取得・更新（Let's Encrypt）
                ┌─────┴─────┐
        /api/*  │           │  それ以外
                ▼           ▼
          [ backend ]  [ frontend ]
                │
                ▼
            [ MySQL ]   外部に公開しない
```

**同一オリジンなので CORS 設定は不要**で、Cookie も `SameSite=Lax` のまま動く。

## 前提

| 項目 | 要件 |
|---|---|
| VPS | **メモリ 2GB 以上**（Spring Boot + MySQL + Next.js が同居するため） |
| OS | Docker と Docker Compose が動くもの |
| ドメイン | A レコードを VPS の IP に向けておく。**証明書取得の前提** |
| ポート | 80 と 443 を開放（Let's Encrypt の検証に 80 が必要） |

## 初回セットアップ

### 1. リポジトリを配置

```bash
git clone <リポジトリ URL> wanteat && cd wanteat
```

### 2. `.env` を作成

**`.env.example` をコピーして値を埋める。開発用の値を流用しないこと。**

```bash
cp .env.example .env
openssl rand -base64 48    # JWT_SECRET 用
openssl rand -base64 24    # DB パスワード用（2つ生成する）
```

最低限、次の項目が必要。未設定なら `docker compose` が起動を拒否する。

```
DOMAIN=example.com
JWT_SECRET=<生成した値>
MYSQL_ROOT_PASSWORD=<生成した値>
MYSQL_PASSWORD=<生成した値>
ANTHROPIC_API_KEY=sk-ant-...
SIGNUP_INVITE_CODE=<任意の合言葉>
```

`SIGNUP_INVITE_CODE` を空のままにすると**誰でもアカウントを作れる**。
公開する以上、必ず値を設定すること（AI の利用料が請求される）。

### 3. 起動

```bash
docker compose -f compose.prod.yaml up -d --build
```

初回はビルドに数分かかる。Caddy が証明書を取得するまで少し待つ。

### 4. 最初のユーザーを登録

`https://example.com/signup` から登録する。
**招待コードを設定している場合、フロントの登録画面に入力欄が必要**。
未実装なら、先に API を直接叩いて登録する。

```bash
curl -X POST https://example.com/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"email":"you@example.com","password":"********","confirmPassword":"********","inviteCode":"<合言葉>"}'
```

### 5. 登録を閉じる

2人の登録が済んだら、`SIGNUP_INVITE_CODE` を別の値に変えて再起動する。
以降、古いコードでは登録できない。

```bash
docker compose -f compose.prod.yaml up -d backend
```

## 更新手順

```bash
git pull
docker compose -f compose.prod.yaml up -d --build
```

Flyway が未適用のマイグレーションを自動で流す。**マイグレーションは追記のみ**とし、
適用済みのファイルは編集しないこと（チェックサム不一致で起動できなくなる）。

## バックアップ

DB のダンプを定期取得する。

```bash
docker compose -f compose.prod.yaml exec -T db \
  mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" wanteat > backup-$(date +%F).sql
```

`.env` も**別途保管**すること。`JWT_SECRET` を失うと全員がログアウトされ、
`MYSQL_PASSWORD` を失うと既存ボリュームに接続できなくなる。

## ローカルで本番構成を検証する

証明書なしで動作確認できる。`DOMAIN` にポートだけ指定すると Caddy は平文 HTTP になる。

```bash
docker compose down          # 開発スタックを停止（ボリュームは残る）

DOMAIN=:80 HTTP_PORT=8000 HTTPS_PORT=8443 COOKIE_SECURE=false \
  docker compose -f compose.prod.yaml up -d --build

# http://localhost:8000 で確認

docker compose -f compose.prod.yaml down
docker compose up -d         # 開発スタックに戻す
```

**平文 HTTP では `COOKIE_SECURE=false` が必要**。true のままだとブラウザが
Cookie を保存せず、ログインできない。

開発用（`compose.yaml`）と本番用（`compose.prod.yaml`）は
**別プロジェクト名**（`wanteat-prod`）なので、ボリュームもコンテナも混ざらない。

## つまずきやすい点

| 症状 | 原因 |
|---|---|
| 起動が `required variable ... is missing` で止まる | `.env` の必須項目が未設定。**意図した動作** |
| ログインしても弾かれる | HTTPS なのに `COOKIE_SECURE=false`、または逆 |
| 証明書が取得できない | DNS の A レコード未設定、80 番が塞がっている |
| マイグレーションで起動失敗 | 適用済みファイルを編集した。`down -v` は**本番データを消す**ので厳禁 |
