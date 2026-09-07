package com.example.wanteat.domain;

/** リクエストの状態。 */
public enum RequestStatus {
    /** 未消化。提案時に AI へ渡される。 */
    OPEN,
    /** 献立に反映済み。 */
    FULFILLED
}
