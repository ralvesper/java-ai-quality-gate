package com.ralvesper.qualitygate;

public record GateResult(
        String gate,
        GateStatus status,
        String message,
        long durationMs
) {
    public GateResult(String gate, GateStatus status, String message) {
        this(gate, status, message, 0L);
    }
}
