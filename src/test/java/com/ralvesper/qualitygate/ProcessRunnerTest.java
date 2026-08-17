package com.ralvesper.qualitygate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProcessRunnerTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldCaptureSuccessfulProcess() throws Exception {
        List<String> command = isWindows()
                ? List.of("cmd", "/c", "echo", "ok")
                : List.of("sh", "-c", "echo ok");

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ProcessResult result = new ProcessRunner().run(
                command,
                tempDir,
                Duration.ofSeconds(5),
                new PrintStream(output)
        );

        assertEquals(0, result.exitCode());
        assertFalse(result.timedOut());
        assertTrue(output.toString().contains("ok"));
        assertFalse(result.duration().isNegative());
    }

    @Test
    void shouldTimeoutLongRunningProcess() throws Exception {
        List<String> command = isWindows()
                ? List.of("cmd", "/c", "ping -n 6 127.0.0.1 > nul")
                : List.of("sh", "-c", "sleep 5");

        ProcessResult result = new ProcessRunner().run(
                command,
                tempDir,
                Duration.ofMillis(100),
                null
        );

        assertTrue(result.timedOut());
        assertEquals(-1, result.exitCode());
    }

    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }
}
