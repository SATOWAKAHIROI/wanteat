-- 常備品フラグ。レシピには表示するが、買い物リストへは展開しない材料を区別する。
ALTER TABLE ingredients
    ADD COLUMN pantry_staple BOOLEAN NOT NULL DEFAULT FALSE;
