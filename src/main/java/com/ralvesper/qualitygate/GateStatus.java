package com.ralvesper.qualitygate;

public enum GateStatus {
    PASS,
    WARNING,
    FAIL,
    ERROR,
    SKIPPED;

    public boolean blocksMerge() {
        return this == FAIL || this == ERROR;
    }
}
