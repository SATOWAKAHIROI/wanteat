CREATE TABLE users (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    email      VARCHAR(255) NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    role       VARCHAR(20)  NOT NULL,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- TODO: アプリ固有のテーブルをここに追加する
-- 例）
-- CREATE TABLE samples (
--     id         BIGINT       NOT NULL AUTO_INCREMENT,
--     name       VARCHAR(255) NOT NULL,
--     created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
--     PRIMARY KEY (id)
-- ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 初期ユーザー（password は必ず BCrypt ハッシュに変換してから INSERT すること）
-- 例）'password' を BCrypt ハッシュ化した値:
-- INSERT INTO users (email, password, role)
-- VALUES ('admin@example.com', '$2a$xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx', 'ADMIN');
