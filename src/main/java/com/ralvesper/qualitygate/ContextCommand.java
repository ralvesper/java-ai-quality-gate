package com.ralvesper.qualitygate;

import com.fasterxml.jackson.databind.ObjectMapper;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(
        name = "context",
        mixinStandardHelpOptions = true,
        description = "Monta o contexto completo de revisão: Git diff, work item e arquitetura."
)
public class ContextCommand implements Callable<Integer> {

    @Option(names = "--project", defaultValue = ".", description = "Diretório raiz do projeto.")
    private Path project;

    @Option(names = "--base", description = "Branch/ref base para o Git diff.")
    private String base;

    @Option(names = "--work-item", description = "ID explícito do work item. Se omitido, tenta detectar pela branch.")
    private String workItemId;

    @Option(names = {"-f", "--format"}, defaultValue = "text", description = "Formato: text ou json.")
    private OutputFormat format;

    enum OutputFormat { text, json }

    @Override
    public Integer call() {
        try {
            ReviewContext context = new ReviewContextBuilder().build(project, base, workItemId);
            if (format == OutputFormat.json) {
                System.out.println(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(context));
            } else {
                printText(context);
            }
            return 0;
        } catch (Exception e) {
            System.err.println("Erro ao montar contexto de revisão: " + e.getMessage());
            return 1;
        }
    }

    private void printText(ReviewContext context) {
        System.out.println("Review Context");
        System.out.println("--------------");
        System.out.println("Project: " + context.project());
        System.out.println("Branch: " + context.git().currentBranch());
        System.out.println("Base: " + context.git().baseRef());
        System.out.println("Changed files: " + context.git().files().size());
        System.out.println("Architecture mode: " + context.architecture().mode());
        System.out.println("Architecture style: " + context.architecture().style());
        System.out.println("Architecture confidence: " + context.architecture().confidence());
        System.out.println("Architecture document: " + (context.architectureDocument() == null ? "NOT FOUND" : "LOADED"));

        if (context.workItem() == null) {
            System.out.println("Work item: NOT FOUND / DISABLED");
        } else {
            System.out.println("Work item: " + context.workItem().id() + " - " + context.workItem().title());
            System.out.println("Acceptance criteria: " + context.workItem().acceptanceCriteria().size());
            System.out.println("Test scenarios: " + context.workItem().testScenarios().size());
        }
    }
}
