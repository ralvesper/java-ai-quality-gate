package com.ralvesper.qualitygate;

public record AiFinding(
        Category category,
        Severity severity,
        Confidence confidence,
        String file,
        Integer line,
        String message,
        String rationale
) {
    public enum Category {
        CORRECTNESS,
        SECURITY,
        ARCHITECTURE,
        TESTS,
        SCOPE,
        OVERENGINEERING,
        MAINTAINABILITY
    }

    public enum Severity {
        INFO,
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    public enum Confidence {
        LOW,
        MEDIUM,
        HIGH
    }
}
