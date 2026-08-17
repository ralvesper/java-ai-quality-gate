package com.ralvesper.qualitygate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ProcessRunner {

    public ProcessResult run(List<String> command, Path workingDirectory, Duration timeout, PrintStream outputStream)
            throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(workingDirectory.toFile());
        processBuilder.redirectErrorStream(true);

        Instant startedAt = Instant.now();
        Process process = processBuilder.start();

        Thread outputThread = Thread.ofVirtual().start(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (outputStream != null) {
                        outputStream.println(line);
                    }
                }
            } catch (IOException ignored) {
                // Process termination may close the stream while the reader is active.
            }
        });

        boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
        boolean timedOut = !finished;
        if (timedOut) {
            process.destroy();
            if (!process.waitFor(2, TimeUnit.SECONDS)) {
                process.destroyForcibly();
            }
        }

        outputThread.join(2_000);
        int exitCode = timedOut ? -1 : process.exitValue();
        return new ProcessResult(exitCode, timedOut, Duration.between(startedAt, Instant.now()));
    }
}
