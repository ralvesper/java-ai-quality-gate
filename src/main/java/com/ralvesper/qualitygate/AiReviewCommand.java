package com.ralvesper.qualitygate;

import com.fasterxml.jackson.databind.ObjectMapper;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.PrintStream;
import java.nio.file.Files;
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

    @Option(names = "--commit", description = "Hash/tag/ref de um commit específico. Diffa o commit contra o pai.")
    private String commitRef;

    @Option(names = {"-f", "--format"}, defaultValue = "text", description = "Formato: text ou json.")
    private OutputFormat format;

    @Option(names = {"-o", "--output"}, description = "Salva o resultado em arquivo. Com -f json, salva JSON; sem -f, salva texto.")
    private Path outputFile;

    @Option(names = "--github-comment", description = "Posta o resultado como comentário na issue do GitHub.")
    private boolean githubComment;

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

            ReviewContext context = new ReviewContextBuilder().build(root, base, workItemId, commitRef);
            AiReviewProvider provider = new CommandAiReviewProvider(aiConfig.command(), root);
            AiReviewResult result = provider.review(context);
            AiReviewPolicy.Decision decision = new AiReviewPolicy().evaluate(result);

            PrintStream out = System.out;
            if (outputFile != null) {
                Path parent = outputFile.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                out = new PrintStream(Files.newOutputStream(outputFile));
            }

            if (format == OutputFormat.json) {
                ObjectMapper mapper = new ObjectMapper();
                String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(
                        new AiReviewReport(decision.status(), decision.block(), result));
                out.println(json);
            } else {
                printText(result, decision, out);
            }

            if (out != System.out) {
                out.close();
                System.out.println("Salvo em: " + outputFile.toAbsolutePath());
            }

            if (githubComment) {
                postGitHubComment(root, config, result, decision, commitRef);
            }

            return decision.block() ? 1 : 0;
        } catch (Exception e) {
            System.err.println("Erro no AI review: " + e.getMessage());
            return 1;
        }
    }

    private void printText(AiReviewResult result, AiReviewPolicy.Decision decision, PrintStream out) {
        out.println("AI Review");
        out.println("---------");
        out.println("Provider: " + result.provider());
        out.println("Status: " + decision.status());
        out.println("Findings: " + result.findings().size());
        if (result.summary() != null && !result.summary().isBlank()) {
            out.println("Summary: " + result.summary());
        }
        out.println();
        for (AiFinding finding : result.findings()) {
            String location = finding.file() == null ? "" : " " + finding.file()
                    + (finding.line() == null ? "" : ":" + finding.line());
            out.printf("[%s/%s] %s%s - %s%n",
                    finding.severity(), finding.confidence(), finding.category(), location, finding.message());
        }
    }

    public record AiReviewReport(String status, boolean block, AiReviewResult review) {
    }

    private void postGitHubComment(Path root, QualityGateConfig config,
                                   AiReviewResult result, AiReviewPolicy.Decision decision,
                                   String commitRef) throws Exception {
        String resolvedWorkItemId = this.workItemId;
        if (resolvedWorkItemId == null || resolvedWorkItemId.isBlank()) {
            QualityGateConfig.WorkItemConfig wiConfig = config.workItem();
            if (wiConfig.enabled()) {
                resolvedWorkItemId = new WorkItemIdResolver()
                        .resolve(root, wiConfig.detection().branchPatterns())
                        .orElse(null);
            }
        }

        if (resolvedWorkItemId == null || resolvedWorkItemId.isBlank()) {
            System.err.println("github-comment: Work item não detectado. Use --work-item ou configure branchPatterns.");
            return;
        }

        String repo = config.aiReview().githubComment().repository();
        if (repo == null || repo.isBlank()) {
            System.err.println("github-comment: repository não configurado em aiReview.githubComment.repository");
            return;
        }

        String comment = new ReviewCommentFormatter().format(result, decision, commitRef);
        String url = new GitHubCommentService(repo).postComment(resolvedWorkItemId, comment);
        System.out.println("Comentário postado em: " + url);
    }
}
