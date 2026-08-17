package com.ralvesper.qualitygate;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(
        name = "architecture",
        description = "Detecta e inicializa o contrato arquitetural do projeto.",
        subcommands = {
                ArchitectureCommand.Detect.class,
                ArchitectureCommand.Init.class
        }
)
public class ArchitectureCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("Use 'architecture detect' ou 'architecture init'.");
    }

    @Command(name = "detect", description = "Detecta o contexto arquitetural sem modificar o projeto.")
    public static class Detect implements Callable<Integer> {

        @Option(names = "--project", defaultValue = ".", description = "Diretório do projeto.")
        private Path project;

        @Override
        public Integer call() {
            ArchitectureContext context = new ArchitectureDetector().detect(project);
            print(context);
            return 0;
        }
    }

    @Command(name = "init", description = "Gera uma proposta inicial de ARCHITECTURE.md baseada em evidências locais.")
    public static class Init implements Callable<Integer> {

        @Option(names = "--project", defaultValue = ".", description = "Diretório do projeto.")
        private Path project;

        @Option(names = "--force", description = "Sobrescreve ARCHITECTURE.md existente.")
        private boolean force;

        @Override
        public Integer call() throws Exception {
            Path root = project.toAbsolutePath().normalize();
            Path output = root.resolve(".agents/ARCHITECTURE.md");

            if (Files.exists(output) && !force) {
                System.err.println("ARCHITECTURE.md já existe. Use --force somente se quiser substituir o arquivo.");
                return 1;
            }

            ArchitectureContext context = new ArchitectureDetector().detect(root, output);
            Files.createDirectories(output.getParent());
            Files.writeString(output, render(context));

            System.out.println("ARCHITECTURE.md criado em " + output);
            System.out.println("Mode: " + context.mode());
            System.out.println("Detected style: " + context.style());
            System.out.println("Confidence: " + context.confidence());
            System.out.println("Revise o arquivo antes de tratá-lo como contrato arquitetural explícito.");
            return 0;
        }
    }

    private static void print(ArchitectureContext context) {
        System.out.println("Project: " + context.projectName());
        System.out.println("Mode: " + context.mode());
        System.out.println("Style: " + context.style());
        System.out.println("Confidence: " + context.confidence());
        if (context.documentation() != null) {
            System.out.println("Documentation: " + context.documentation());
        }
        System.out.println("Evidence:");
        context.evidence().forEach(item -> System.out.println("- " + item));
    }

    private static String render(ArchitectureContext context) {
        StringBuilder evidence = new StringBuilder();
        context.evidence().forEach(item -> evidence.append("- ").append(item).append('\n'));

        return """
                # Architecture — %s

                > Status: INFERRED
                > Detected style: %s
                > Confidence: %s
                > This file was generated from local project evidence and must be reviewed before becoming an explicit architecture contract.

                ## Style

                %s

                ## Modules

                Document the real modules/layers and their responsibilities.

                ## Dependency Rules

                Document allowed and forbidden dependencies based on the actual project architecture.

                ## Integration Rules

                Document HTTP clients, persistence, messaging, files and external systems.

                ## Cross-cutting Rules

                Document security, transactions, observability, error handling and other cross-cutting concerns.

                ## Exceptions

                Document deliberate exceptions, especially legacy compatibility constraints.

                ## Architecture Evidence

                %s
                """.formatted(
                context.projectName(),
                context.style(),
                context.confidence(),
                context.style().equals("unknown")
                        ? "Architecture style could not be identified with enough confidence."
                        : "Architecture style inferred as **" + context.style() + "**.",
                evidence.toString().isBlank() ? "- No strong evidence found.\n" : evidence.toString()
        );
    }
}
