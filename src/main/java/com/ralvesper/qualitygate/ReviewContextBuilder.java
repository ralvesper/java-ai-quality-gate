package com.ralvesper.qualitygate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class ReviewContextBuilder {

    public ReviewContext build(Path project, String baseRef, String workItemId) throws Exception {
        Path root = project.toAbsolutePath().normalize();
        QualityGateConfig config = new QualityGateConfigLoader().load(root);

        GitDiffContext git = new GitContextAnalyzer().analyze(root, baseRef);
        ArchitectureContext architecture = new ArchitectureDetector().detect(root);
        String architectureDocument = readArchitectureDocument(architecture);
        WorkItem workItem = resolveWorkItem(root, config.workItem(), workItemId).orElse(null);

        if (config.workItem().enabled() && config.workItem().required() && workItem == null) {
            throw new IllegalStateException("Work item obrigatório não encontrado para a mudança atual.");
        }

        return new ReviewContext(
                root.toString(),
                git,
                workItem,
                architecture,
                architectureDocument
        );
    }

    private Optional<WorkItem> resolveWorkItem(
            Path root,
            QualityGateConfig.WorkItemConfig config,
            String explicitId
    ) throws Exception {
        if (!config.enabled()) {
            return Optional.empty();
        }
        return new WorkItemContextService().resolve(root, config, explicitId);
    }

    private String readArchitectureDocument(ArchitectureContext architecture) throws Exception {
        if (architecture.documentation() == null || !Files.isRegularFile(architecture.documentation())) {
            return null;
        }
        return Files.readString(architecture.documentation());
    }
}
