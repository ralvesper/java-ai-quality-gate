package com.ralvesper.qualitygate;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
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
            required = true,
            description = "Diretório raiz do projeto Java/Maven."
    )
    private Path project;

    @Option(
            names = "--mvn-args",
            description = "Argumentos adicionais para passar ao comando Maven (ex: '-B -Pprofile')."
    )
    private String mvnArgs;

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
        if (project == null) {
            System.err.println("Erro: O diretório do projeto (--project) é obrigatório.");
            return 1;
        }

        ProjectContext context = new ProjectContext(project.toAbsolutePath().normalize());
        
        // Se o formato for JSON, redireciona o log do Maven para o stderr para manter o stdout limpo
        java.io.PrintStream mavenLogStream = (format == OutputFormat.json) ? System.err : System.out;

        List<GateResult> checkResults = new ArrayList<>();

        // Executar verificação do Maven Verify
        MavenBuildCheck mavenCheck = new MavenBuildCheck(mvnArgs, mavenLogStream);
        GateResult mavenResult = mavenCheck.execute(context);
        checkResults.add(mavenResult);

        // Determinar status consolidado
        GateStatus consolidatedStatus = GateStatus.PASS;
        for (GateResult r : checkResults) {
            if (r.status() == GateStatus.FAIL) {
                consolidatedStatus = GateStatus.FAIL;
                break;
            } else if (r.status() == GateStatus.WARNING && consolidatedStatus != GateStatus.FAIL) {
                consolidatedStatus = GateStatus.WARNING;
            }
        }

        if (format == OutputFormat.json) {
            QualityGateReport report = new QualityGateReport(
                    context.projectPath().toString(),
                    consolidatedStatus,
                    checkResults
            );
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper()
                        .enable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT);
                String json = mapper.writeValueAsString(report);
                System.out.println(json);
            } catch (Exception e) {
                System.err.println("Erro ao gerar JSON: " + e.getMessage());
                return 1;
            }
        } else {
            System.out.println();
            System.out.println("Java AI Quality Gate");
            System.out.println("--------------------");
            System.out.println("Project: " + context.projectPath());
            for (GateResult r : checkResults) {
                System.out.println(r.gate() + ": " + r.status());
                System.out.println("Message: " + r.message());
            }
            System.out.println();

            if (consolidatedStatus == GateStatus.FAIL) {
                System.out.println("QUALITY GATE: BLOCK");
            } else {
                System.out.println("QUALITY GATE: PASS");
            }
        }

        return (consolidatedStatus == GateStatus.FAIL) ? 1 : 0;
    }
}
