package com.ralvesper.qualitygate;

import java.time.Duration;

public record ProcessResult(
        int exitCode,
        boolean timedOut,
        Duration duration
) {
}
