package com.ralvesper.qualitygate;

import java.util.List;

public record QualityGateConfig(
        MavenConfig maven
) {
    public QualityGateConfig {
        maven = maven == null ? MavenConfig.defaults() : maven;
    }

    public static QualityGateConfig defaults() {
        return new QualityGateConfig(MavenConfig.defaults());
    }

    public record MavenConfig(
            long timeoutSeconds,
            List<String> args
    ) {
        public MavenConfig {
            timeoutSeconds = timeoutSeconds <= 0 ? 600 : timeoutSeconds;
            args = args == null ? List.of() : List.copyOf(args);
        }

        public static MavenConfig defaults() {
            return new MavenConfig(600, List.of());
        }
    }
}
