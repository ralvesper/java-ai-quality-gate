package com.ralvesper.qualitygate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ReviewContextBuilderTest {

    @TempDir
    Path tempDir;

    @Test
    void buildsContextWithGitAndArchitectureWhenWorkItemIsDisabled() throws Exception {
        Path repo = tempDir.resolve("customer-service");
        Files.createDirectories(repo);

        git(repo, "init", "-b", "main");
        git(repo, "config", "user.email", "quality-gate@example.test");
        git(repo, "config", "user.name", "Quality Gate Test");

        Files.writeString(repo.resolve("pom.xml"), """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.example</groupId>
                  <artifactId>customer-service</artifactId>
                  <version>1.0.0</version>
                </project>
                """);
        Files.createDirectories(repo.resolve("src/main/java/com/example/controller"));
        Files.createDirectories(repo.resolve("src/main/java/com/example/service"));
        Files.createDirectories(repo.resolve("src/main/java/com/example/repository"));
        Files.writeString(repo.resolve("ARCHITECTURE.md"), "# Architecture — customer-service\n");

        git(repo, "add", ".");
        git(repo, "commit", "-m", "initial");
        git(repo, "checkout", "-b", "feature/FS-687-test");

        Path changed = repo.resolve("src/main/java/com/example/service/ExampleService.java");
        Files.writeString(changed, "class ExampleService {}\n");

        ReviewContext context = new ReviewContextBuilder().build(repo, "main", null);

        assertEquals("feature/FS-687-test", context.git().currentBranch());
        assertFalse(context.git().files().isEmpty());
        assertNull(context.workItem());
        assertEquals(ArchitectureContext.ArchitectureMode.EXPLICIT, context.architecture().mode());
        assertNotNull(context.architectureDocument());
        assertTrue(context.architectureDocument().contains("customer-service"));
    }

    private void git(Path directory, String... args) throws Exception {
        String[] command = new String[args.length + 1];
        command[0] = "git";
        System.arraycopy(args, 0, command, 1, args.length);

        Process process = new ProcessBuilder(command)
                .directory(directory.toFile())
                .redirectErrorStream(true)
                .start();
        String output = new String(process.getInputStream().readAllBytes());
        int exit = process.waitFor();
        if (exit != 0) {
            throw new IllegalStateException("git failed: " + String.join(" ", command) + "\n" + output);
        }
    }
}
