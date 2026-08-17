package com.ralvesper.qualitygate;

public record GateResult(
        String gate,
        GateStatus status,
        String message
) {
}
