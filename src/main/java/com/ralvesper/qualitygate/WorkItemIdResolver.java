package com.ralvesper.qualitygate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WorkItemIdResolver {

    public Optional<String> resolve(Path projectPath, List<String> patterns) throws IOException, InterruptedException {
        Process process = new ProcessBuilder("git", "branch", "--show-current")
                .directory(projectPath.toFile())
                .redirectErrorStream(true)
                .start();

        String branch = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        int exitCode = process.waitFor();
        if (exitCode != 0 || branch.isBlank()) {
            return Optional.empty();
        }
        return resolveFromText(branch, patterns);
    }

    public Optional<String> resolveFromText(String text, List<String> patterns) {
        if (text == null || text.isBlank() || patterns == null) {
            return Optional.empty();
        }

        for (String expression : patterns) {
            Matcher matcher = Pattern.compile(expression, Pattern.CASE_INSENSITIVE).matcher(text);
            if (matcher.find()) {
                String value = matcher.groupCount() >= 1 ? matcher.group(1) : matcher.group();
                return Optional.of(value.toUpperCase());
            }
        }
        return Optional.empty();
    }
}
