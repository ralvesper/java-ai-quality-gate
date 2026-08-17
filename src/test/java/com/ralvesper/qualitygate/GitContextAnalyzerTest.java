package com.ralvesper.qualitygate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GitContextAnalyzerTest {

    @TempDir
    Path tempDir;

    @Test
    void detectsCommittedAndWorkingTreeChangesAgainstBase() throws Exception {
        Path repo = tempDir.resolve("repo");
        Files.createDirectories(repo);

        git(repo, "init", "-b", "main");
        git(repo, "config", "user.email", "quality-gate@example.test");
        git(repo, "config", "user.name", "Quality Gate Test");

        Path file = repo.resolve("Example.java");
        Files.writeString(file, "line1\nline2\nline3\n");
        git(repo, "add", ".");
        git(repo, "commit", "-m", "initial");

        git(repo, "checkout", "-b", "feature/test");
        Files.writeString(file, "line1\nline2 changed\nline3\nline4\n");
        git(repo, "add", ".");
        git(repo, "commit", "-m", "change line");

        Files.writeString(file, "line1\nline2 changed\nline3 changed locally\nline4\nline5 local\n");

        GitDiffContext context = new GitContextAnalyzer().analyze(repo, "main");

        assertEquals("feature/test", context.currentBranch());
        assertEquals("main", context.baseRef());
        assertEquals(1, context.files().size());
        assertEquals("Example.java", context.files().getFirst().path());
        assertTrue(context.files().getFirst().addedLines().contains(2));
        assertTrue(context.files().getFirst().addedLines().contains(3));
        assertTrue(context.files().getFirst().addedLines().contains(5));
        assertTrue(context.patch().contains("line5 local"));
    }

    @Test
    void rejectsDirectoryOutsideGitRepository() throws Exception {
        Path project = tempDir.resolve("not-git");
        Files.createDirectories(project);

        Exception error = org.junit.jupiter.api.Assertions.assertThrows(
                Exception.class,
                () -> new GitContextAnalyzer().analyze(project, "main")
        );

        assertTrue(error.getMessage().contains("Git") || error.getMessage().contains("git"));
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
