package com.example.wanteat.domain;

/** 献立の状態。 */
public enum MenuStatus {
    /** 詳細（材料・手順）を生成中。 */
    GENERATING,
    /** 確定済み。 */
    CONFIRMED,
    /** 調理済み。 */
    COOKED,
    /** 生成に失敗。再生成が必要。 */
    FAILED
}
