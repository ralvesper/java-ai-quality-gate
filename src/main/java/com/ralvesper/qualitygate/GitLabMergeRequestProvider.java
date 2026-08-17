package com.ralvesper.qualitygate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class GitLabMergeRequestProvider {

    private final ObjectMapper mapper = new ObjectMapper();

    public GitLabMergeRequestContext load(String mergeRequestUrl) throws IOException, InterruptedException {
        GitLabMergeRequestRef ref = GitLabMergeRequestRef.parse(mergeRequestUrl);

        String metadataJson = run(List.of(
                "glab", "mr", "view", String.valueOf(ref.iid()),
                "-R", ref.repositoryUrl(),
                "-F", "json"
        ));

        String patch = run(List.of(
                "glab", "mr", "diff", String.valueOf(ref.iid()),
                "-R", ref.repositoryUrl(),
                "--color=never"
        ));

        JsonNode node = mapper.readTree(metadataJson);
        return new GitLabMergeRequestContext(
                "gitlab",
                ref.iid(),
                text(node, "title"),
                firstText(node, "description", "body"),
                firstText(node, "source_branch", "sourceBranch"),
                firstText(node, "target_branch", "targetBranch"),
                text(node, "state"),
                author(node),
                firstText(node, "web_url", "webUrl", "url", ref.url()),
                ref.projectPath(),
                patch
        );
    }

    private String run(List<String> command) throws IOException, InterruptedException {
        Process process;
        try {
            process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();
        } catch (IOException e) {
            throw new IOException("Não foi possível executar 'glab'. Instale/autentique o GitLab CLI e tente novamente.", e);
        }

        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("glab falhou (exit " + exitCode + "): " + output.trim());
        }
        return output;
    }

    private String author(JsonNode node) {
        JsonNode author = node.get("author");
        if (author == null || author.isNull()) return null;
        if (author.isTextual()) return author.asText();
        return firstText(author, "username", "name");
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private String firstText(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node.get(field);
            if (value != null && !value.isNull() && !value.asText().isBlank()) {
                return value.asText();
            }
        }
        return null;
    }
}
