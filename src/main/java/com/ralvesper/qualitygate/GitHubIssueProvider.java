package com.ralvesper.qualitygate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.regex.Pattern;

public class GitHubIssueProvider implements WorkItemProvider {

    private final String repository;
    private final ObjectMapper mapper;
    private final MarkdownWorkItemParser parser;

    public GitHubIssueProvider(String repository) {
        this(repository, new ObjectMapper(), new MarkdownWorkItemParser());
    }

    GitHubIssueProvider(String repository, ObjectMapper mapper, MarkdownWorkItemParser parser) {
        if (repository == null || repository.isBlank()) {
            throw new IllegalArgumentException("GitHub repository não configurado para work item");
        }
        this.repository = repository;
        this.mapper = mapper;
        this.parser = parser;
    }

    @Override
    public Optional<WorkItem> resolve(String workItemId) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(
                "gh", "issue", "list",
                "--repo", repository,
                "--state", "all",
                "--search", workItemId + " in:title",
                "--json", "number,title,body,url",
                "--limit", "50"
        ).redirectErrorStream(true).start();

        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("Falha ao consultar GitHub Issues via gh: " + output.trim());
        }

        JsonNode issues = mapper.readTree(output);
        Pattern exactToken = Pattern.compile("\\[" + Pattern.quote(workItemId) + "\\]", Pattern.CASE_INSENSITIVE);

        for (JsonNode issue : issues) {
            String title = issue.path("title").asText("");
            if (!exactToken.matcher(title).find()) {
                continue;
            }

            String body = issue.path("body").asText("");
            String url = issue.path("url").asText("");
            return Optional.of(parser.parse(
                    workItemId,
                    title,
                    body,
                    "github:" + repository,
                    url
            ));
        }

        return Optional.empty();
    }
}
