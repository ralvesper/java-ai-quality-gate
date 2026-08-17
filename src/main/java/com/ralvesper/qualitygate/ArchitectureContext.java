package com.ralvesper.qualitygate;

import java.nio.file.Path;
import java.util.List;

public record ArchitectureContext(
        String projectName,
        ArchitectureMode mode,
        String style,
        Confidence confidence,
        Path documentation,
        List<String> evidence
) {
    public ArchitectureContext {
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
    }

    public enum ArchitectureMode {
        EXPLICIT,
        INFERRED,
        UNKNOWN
    }

    public enum Confidence {
        HIGH,
        MEDIUM,
        LOW
    }
}
