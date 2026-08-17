package com.ralvesper.qualitygate;

import java.util.List;

public record QualityGateConfig(
        MavenConfig maven,
        WorkItemConfig workItem
) {
    public QualityGateConfig {
        maven = maven == null ? MavenConfig.defaults() : maven;
        workItem = workItem == null ? WorkItemConfig.defaults() : workItem;
    }

    public static QualityGateConfig defaults() {
        return new QualityGateConfig(MavenConfig.defaults(), WorkItemConfig.defaults());
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

    public record WorkItemConfig(
            boolean enabled,
            boolean required,
            String provider,
            GithubConfig github,
            DetectionConfig detection
    ) {
        public WorkItemConfig {
            provider = provider == null || provider.isBlank() ? "github" : provider;
            github = github == null ? new GithubConfig(null) : github;
            detection = detection == null ? DetectionConfig.defaults() : detection;
        }

        public static WorkItemConfig defaults() {
            return new WorkItemConfig(false, false, "github", new GithubConfig(null), DetectionConfig.defaults());
        }
    }

    public record GithubConfig(String repository) {
    }

    public record DetectionConfig(List<String> branchPatterns) {
        public DetectionConfig {
            branchPatterns = branchPatterns == null || branchPatterns.isEmpty()
                    ? defaults().branchPatterns()
                    : List.copyOf(branchPatterns);
        }

        public static DetectionConfig defaults() {
            return new DetectionConfig(List.of(
                    "(FS-\\d+)",
                    "(PLUX-\\d+)",
                    "(ENH-\\d+)",
                    "(GSI-\\d+)"
            ));
        }
    }
}
