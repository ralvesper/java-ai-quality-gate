package com.ralvesper.qualitygate;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

public class WorkItemContextService {

    private final WorkItemIdResolver idResolver;

    public WorkItemContextService() {
        this(new WorkItemIdResolver());
    }

    WorkItemContextService(WorkItemIdResolver idResolver) {
        this.idResolver = idResolver;
    }

    public Optional<WorkItem> resolve(Path projectPath, QualityGateConfig.WorkItemConfig config, String explicitId)
            throws IOException, InterruptedException {
        if (!config.enabled()) {
            return Optional.empty();
        }

        String id = explicitId;
        if (id == null || id.isBlank()) {
            id = idResolver.resolve(projectPath, config.detection().branchPatterns()).orElse(null);
        }
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }

        WorkItemProvider provider = provider(config);
        return provider.resolve(id);
    }

    WorkItemProvider provider(QualityGateConfig.WorkItemConfig config) {
        if (!"github".equalsIgnoreCase(config.provider())) {
            throw new IllegalArgumentException("Work item provider não suportado: " + config.provider());
        }
        return new GitHubIssueProvider(config.github().repository());
    }
}
