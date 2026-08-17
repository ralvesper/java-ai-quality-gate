package com.ralvesper.qualitygate;

import com.fasterxml.jackson.databind.ObjectMapper;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.Callable;

@Command(
        name = "work-item",
        description = "Resolve contexto de requisito/issue associado à mudança atual."
)
public class WorkItemCommand implements Callable<Integer> {

    @Option(names = "--project", defaultValue = ".", description = "Diretório raiz do projeto.")
    private Path project;

    @Option(names = "--id", description = "ID explícito do work item. Se omitido, tenta detectar pela branch.")
    private String id;

    @Option(names = {"-f", "--format"}, defaultValue = "text", description = "Formato: text ou json.")
    private OutputFormat format;

    enum OutputFormat { text, json }

    @Override
    public Integer call() {
        try {
            Path projectPath = project.toAbsolutePath().normalize();
            QualityGateConfig config = new QualityGateConfigLoader().load(projectPath);
            QualityGateConfig.WorkItemConfig workItemConfig = config.workItem();

            if (!workItemConfig.enabled()) {
                System.out.println("Work item context: DISABLED");
                return 0;
            }

            Optional<WorkItem> result = new WorkItemContextService().resolve(projectPath, workItemConfig, id);
            if (result.isEmpty()) {
                System.out.println("Work item: NOT FOUND");
                return workItemConfig.required() ? 1 : 0;
            }

            WorkItem workItem = result.get();
            if (format == OutputFormat.json) {
                System.out.println(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(workItem));
            } else {
                printText(workItem);
            }
            return 0;
        } catch (Exception e) {
            System.err.println("Erro ao resolver work item: " + e.getMessage());
            return 1;
        }
    }

    private void printText(WorkItem workItem) {
        System.out.println("Work Item: " + workItem.id());
        System.out.println("Title: " + workItem.title());
        System.out.println("Source: " + workItem.source());
        if (workItem.url() != null && !workItem.url().isBlank()) {
            System.out.println("URL: " + workItem.url());
        }
        if (workItem.objective() != null && !workItem.objective().isBlank()) {
            System.out.println("\nObjective:\n" + workItem.objective());
        }
        if (!workItem.acceptanceCriteria().isEmpty()) {
            System.out.println("\nAcceptance Criteria:");
            workItem.acceptanceCriteria().forEach(item -> System.out.println("- " + item));
        }
        if (!workItem.testScenarios().isEmpty()) {
            System.out.println("\nTest Scenarios:");
            workItem.testScenarios().forEach(item -> System.out.println("- " + item));
        }
    }
}
