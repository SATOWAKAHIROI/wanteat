package com.example.wanteat.domain;

/** AI 生成の段階。 */
public enum AiPhase {
    /** 第1段階。料理名・説明・調理時間・主な材料のみを生成する。 */
    SUGGEST,
    /** 第2段階。材料・分量・手順を生成する。 */
    DETAIL
}
