package com.ralvesper.qualitygate;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

@Command(
        name = "review",
        mixinStandardHelpOptions = true,
        description = "Executa o quality gate em um projeto Java/Maven."
)
public class ReviewCommand implements Callable<Integer> {

    @Option(
            names = "--project",
            description = "Diretório raiz do projeto Java/Maven. Padrão: diretório atual.",
            defaultValue = "."
    )
    private Path project;

    @Option(
            names = "--mvn-arg",
            description = "Argumento adicional para Maven. Pode ser repetido."
    )
    private List<String> mvnArgs = new ArrayList<>();

    @Option(
            names = "--mvn-args",
            description = "Compatibilidade: argumentos Maven separados por espaço.",
            hidden = true
    )
    private String legacyMvnArgs;

    @Option(
            names = "--timeout-seconds",
            description = "Timeout do Maven em segundos. Sobrescreve .quality-gate.yml."
    )
    private Long timeoutSeconds;

    @Option(
            names = {"-f", "--format"},
            description = "Formato de saída: ${COMPLETION-CANDIDATES}",
            defaultValue = "text"
    )
    private OutputFormat format;

    public enum OutputFormat {
        text, json
    }

    @Override
    public Integer call() {
        ProjectContext context = new ProjectContext(project.toAbsolutePath().normalize());
        java.io.PrintStream mavenLogStream = format == OutputFormat.json ? System.err : System.out;

        QualityGateConfig config;
        try {
            config = new QualityGateConfigLoader().load(context.projectPath());
        } catch (Exception e) {
            QualityGateReport report = new QualityGateReport(
                    context.projectPath().toString(),
                    GateStatus.ERROR,
                    List.of(new GateResult("config", GateStatus.ERROR,
                            "Falha ao ler .quality-gate.yml: " + e.getMessage()))
            );
            return writeReport(report);
        }

        List<String> effectiveArgs = new ArrayList<>(config.maven().args());
        effectiveArgs.addAll(mvnArgs);
        if (legacyMvnArgs != null && !legacyMvnArgs.isBlank()) {
            effectiveArgs.addAll(List.of(legacyMvnArgs.trim().split("\\s+")));
        }

        long effectiveTimeout = timeoutSeconds != null ? timeoutSeconds : config.maven().timeoutSeconds();
        MavenBuildCheck mavenCheck = new MavenBuildCheck(
                effectiveArgs,
                mavenLogStream,
                Duration.ofSeconds(effectiveTimeout)
        );

        QualityGateEngine engine = new QualityGateEngine(List.of(mavenCheck));
        QualityGateReport report = engine.execute(context);
        return writeReport(report);
    }

    private int writeReport(QualityGateReport report) {
        ReportWriter writer = format == OutputFormat.json ? new JsonReportWriter() : new TextReportWriter();
        try {
            writer.write(report, System.out);
        } catch (Exception e) {
            System.err.println("Erro ao gerar relatório: " + e.getMessage());
            return 1;
        }
        return report.status().blocksMerge() ? 1 : 0;
    }
}
