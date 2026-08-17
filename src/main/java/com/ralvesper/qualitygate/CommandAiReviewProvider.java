package com.ralvesper.qualitygate;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

public class CommandAiReviewProvider implements AiReviewProvider {

    private final List<String> command;
    private final Path workingDirectory;
    private final ObjectMapper mapper = new ObjectMapper();

    public CommandAiReviewProvider(List<String> command, Path workingDirectory) {
        if (command == null || command.isEmpty()) {
            throw new IllegalArgumentException("AI review command must not be empty");
        }
        this.command = List.copyOf(command);
        this.workingDirectory = workingDirectory;
    }

    @Override
    public AiReviewResult review(ReviewContext context) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command)
                .directory(workingDirectory.toFile())
                .redirectErrorStream(false)
                .start();

        byte[] input = mapper.writeValueAsBytes(context);
        process.getOutputStream().write(input);
        process.getOutputStream().close();

        String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new IOException("AI review command failed with exit code " + exitCode
                    + (stderr.isBlank() ? "" : ": " + stderr.strip()));
        }
        if (stdout.isBlank()) {
            throw new IOException("AI review command returned empty stdout");
        }

        return mapper.readValue(stdout, AiReviewResult.class);
    }
}
