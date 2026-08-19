package com.ralvesper.qualitygate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public class GitHubCommentService {

    private final String repository;
    private final ObjectMapper mapper;

    public GitHubCommentService(String repository) {
        if (repository == null || repository.isBlank()) {
            throw new IllegalArgumentException("GitHub repository não configurado para githubComment");
        }
        this.repository = repository;
        this.mapper = new ObjectMapper();
    }

    public String postComment(String workItemId, String commentBody) throws IOException, InterruptedException {
        int issueNumber = findIssue(workItemId);
        runGh("issue", "comment", String.valueOf(issueNumber),
                "--repo", repository,
                "--body", commentBody);
        return repository + "/issues/" + issueNumber;
    }

    private int findIssue(String workItemId) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(
                "gh", "issue", "list",
                "--repo", repository,
                "--state", "all",
                "--search", workItemId + " in:title",
                "--json", "number,title",
                "--limit", "50"
        ).redirectErrorStream(true).start();

        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("Falha ao buscar issues no GitHub: " + output.trim());
        }

        JsonNode issues = mapper.readTree(output);
        Pattern exactToken = Pattern.compile("\\[" + Pattern.quote(workItemId) + "\\]", Pattern.CASE_INSENSITIVE);

        for (JsonNode issue : issues) {
            String title = issue.path("title").asText("");
            if (exactToken.matcher(title).find()) {
                return issue.path("number").asInt();
            }
        }

        return createIssue(workItemId);
    }

    private int createIssue(String workItemId) throws IOException, InterruptedException {
        String title = "[" + workItemId + "] AI Review — " + workItemId;
        String body = "Issue criada automaticamente pelo java-ai-quality-gate para armazenar resultados de AI review.";

        Process process = new ProcessBuilder(
                "gh", "issue", "create",
                "--repo", repository,
                "--title", title,
                "--body", body
        ).redirectErrorStream(true).start();

        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("Falha ao criar issue no GitHub: " + output.trim());
        }

        String numberStr = output.trim().replaceAll(".*#", "").trim();
        try {
            return Integer.parseInt(numberStr);
        } catch (NumberFormatException e) {
            throw new IOException("Não foi possível extrair número da issue criada: " + output.trim());
        }
    }

    private void runGh(String... args) throws IOException, InterruptedException {
        String[] command = new String[args.length + 1];
        command[0] = "gh";
        System.arraycopy(args, 0, command, 1, args.length);

        Process process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .start();

        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("gh falhou (exit " + exitCode + "): " + output.trim());
        }
    }
}
