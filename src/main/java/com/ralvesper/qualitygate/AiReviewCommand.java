package com.ralvesper.qualitygate;

import com.fasterxml.jackson.databind.ObjectMapper;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(
        name = "ai-review",
        mixinStandardHelpOptions = true,
        description = "Executa revisão assistida por IA usando o ReviewContext estruturado."
)
public class AiReviewCommand implements Callable<Integer> {

    @Option(names = "--project", defaultValue = ".", description = "Diretório raiz do projeto.")
    private Path project;

    @Option(names = "--base", description = "Branch/ref base para o Git diff.")
    private String base;

    @Option(names = "--work-item", description = "ID explícito do work item.")
    private String workItemId;

    @Option(names = {"-f", "--format"}, defaultValue = "text", description = "Formato: text ou json.")
    private OutputFormat format;

    enum OutputFormat { text, json }

    @Override
    public Integer call() {
        try {
            Path root = project.toAbsolutePath().normalize();
            QualityGateConfig config = new QualityGateConfigLoader().load(root);
            QualityGateConfig.AiReviewConfig aiConfig = config.aiReview();

            if (!aiConfig.enabled()) {
                System.out.println("AI review: DISABLED");
                return 0;
            }
            if (!"command".equalsIgnoreCase(aiConfig.provider())) {
                throw new IllegalArgumentException("Unsupported AI review provider: " + aiConfig.provider());
            }

            ReviewContext context = new ReviewContextBuilder().build(root, base, workItemId);
            AiReviewProvider provider = new CommandAiReviewProvider(aiConfig.command(), root);
            AiReviewResult result = provider.review(context);
            AiReviewPolicy.Decision decision = new AiReviewPolicy().evaluate(result);

            if (format == OutputFormat.json) {
                ObjectMapper mapper = new ObjectMapper();
                System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(
                        new AiReviewReport(decision.status(), decision.block(), result)));
            } else {
                printText(result, decision);
            }
            return decision.block() ? 1 : 0;
        } catch (Exception e) {
            System.err.println("Erro no AI review: " + e.getMessage());
            return 1;
        }
    }

    private void printText(AiReviewResult result, AiReviewPolicy.Decision decision) {
        System.out.println("AI Review");
        System.out.println("---------");
        System.out.println("Provider: " + result.provider());
        System.out.println("Status: " + decision.status());
        System.out.println("Findings: " + result.findings().size());
        if (result.summary() != null && !result.summary().isBlank()) {
            System.out.println("Summary: " + result.summary());
        }
        System.out.println();
        for (AiFinding finding : result.findings()) {
            String location = finding.file() == null ? "" : " " + finding.file()
                    + (finding.line() == null ? "" : ":" + finding.line());
            System.out.printf("[%s/%s] %s%s - %s%n",
                    finding.severity(), finding.confidence(), finding.category(), location, finding.message());
        }
    }

    public record AiReviewReport(String status, boolean block, AiReviewResult review) {
    }
}
