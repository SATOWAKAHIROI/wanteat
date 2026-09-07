CREATE TABLE users (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    email      VARCHAR(255) NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    role       VARCHAR(20)  NOT NULL,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE user_preferences(
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL UNIQUE,
    household_size INT NOT NULL,
    allergies VARCHAR(255),
    disliked_foods VARCHAR(255),
    note VARCHAR(1024),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY(id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE menus(
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    cooked_on DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY(id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE dishes(
    id BIGINT NOT NULL AUTO_INCREMENT,
    menu_id BIGINT NOT NULL,
    category VARCHAR(20) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    cooking_minutes INT,
    steps JSON NOT NULL,
    sort_order INT,
    PRIMARY KEY(id),
    FOREIGN KEY(menu_id) REFERENCES menus(id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE ingredients(
    id BIGINT NOT NULL AUTO_INCREMENT,
    dish_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    amount VARCHAR(50),
    unit VARCHAR(20),
    sort_order INT,
    PRIMARY KEY(id),
    FOREIGN KEY(dish_id) REFERENCES dishes(id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE shopping_items(
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    menu_id BIGINT NULL,
    name VARCHAR(100) NOT NULL,
    amount VARCHAR(50),
    unit VARCHAR(20),
    checked BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (menu_id) REFERENCES menus(id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE requests(
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    requested_by VARCHAR(20) NOT NULL,
    body VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL,
    fulfilled_menu_id BIGINT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fulfilled_at DATETIME DEFAULT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (fulfilled_menu_id) REFERENCES menus(id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE ai_generations(
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    phase VARCHAR(20) NOT NULL,
    request_payload JSON NOT NULL,
    response_payload JSON NOT NULL,
    model VARCHAR(50),
    input_tokens INT,
    output_tokens INT,
    latency_ms INT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_menus_user_cooked_on   ON menus (user_id, cooked_on);
CREATE INDEX idx_shopping_user_checked  ON shopping_items (user_id, checked);
CREATE INDEX idx_requests_user_status   ON requests (user_id, status);

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
