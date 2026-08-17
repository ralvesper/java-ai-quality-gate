package com.ralvesper.qualitygate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GitContextAnalyzer {

    private static final Pattern HUNK = Pattern.compile("@@ -(\\d+)(?:,(\\d+))? \\+(\\d+)(?:,(\\d+))? @@.*");

    public GitDiffContext analyze(Path project, String requestedBase) throws IOException, InterruptedException {
        Path root = project.toAbsolutePath().normalize();
        requireGitRepository(root);

        String branch = run(root, "git", "branch", "--show-current").trim();
        if (branch.isBlank()) {
            branch = "DETACHED_HEAD";
        }

        String base = requestedBase == null || requestedBase.isBlank()
                ? detectBase(root)
                : requestedBase.trim();

        String mergeBase = run(root, "git", "merge-base", base, "HEAD").trim();
        String trackedPatch = run(root, "git", "diff", "--no-ext-diff", "--unified=0", mergeBase);
        String nameStatus = run(root, "git", "diff", "--no-ext-diff", "--name-status", mergeBase);

        Map<String, MutableFile> files = parseNameStatus(nameStatus);
        parseChangedLines(trackedPatch, files);

        StringBuilder patch = new StringBuilder(trackedPatch);
        includeUntrackedFiles(root, files, patch);

        List<GitDiffFile> result = files.values().stream()
                .map(MutableFile::toRecord)
                .toList();

        return new GitDiffContext(branch, base, mergeBase, result, patch.toString());
    }

    private void includeUntrackedFiles(Path root, Map<String, MutableFile> files, StringBuilder patch)
            throws IOException, InterruptedException {
        String output = run(root, "git", "ls-files", "--others", "--exclude-standard");
        for (String relativePath : output.lines().filter(line -> !line.isBlank()).toList()) {
            Path file = root.resolve(relativePath).normalize();
            if (!file.startsWith(root) || !Files.isRegularFile(file)) {
                continue;
            }

            List<String> lines;
            try {
                lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            } catch (IOException e) {
                // Binary/unreadable untracked files are still reported, but without line-level content.
                files.putIfAbsent(relativePath, new MutableFile(relativePath, "A"));
                continue;
            }

            MutableFile mutable = new MutableFile(relativePath, "A");
            addRange(mutable.addedLines, 1, lines.size());
            files.put(relativePath, mutable);

            if (!patch.isEmpty() && patch.charAt(patch.length() - 1) != '\n') {
                patch.append('\n');
            }
            patch.append("diff --git a/").append(relativePath).append(" b/").append(relativePath).append('\n')
                    .append("new file mode 100644\n")
                    .append("--- /dev/null\n")
                    .append("+++ b/").append(relativePath).append('\n')
                    .append("@@ -0,0 +1,").append(lines.size()).append(" @@\n");
            for (String line : lines) {
                patch.append('+').append(line).append('\n');
            }
        }
    }

    private void requireGitRepository(Path root) throws IOException, InterruptedException {
        if (!Files.isDirectory(root)) {
            throw new IllegalArgumentException("Diretório do projeto não existe: " + root);
        }
        String result = run(root, "git", "rev-parse", "--is-inside-work-tree").trim();
        if (!"true".equals(result)) {
            throw new IllegalArgumentException("O projeto não está dentro de um repositório Git: " + root);
        }
    }

    private String detectBase(Path root) throws IOException, InterruptedException {
        String remoteHead = tryRun(root, "git", "symbolic-ref", "--quiet", "--short", "refs/remotes/origin/HEAD");
        if (remoteHead != null && !remoteHead.isBlank()) {
            return remoteHead.trim();
        }

        for (String candidate : List.of("origin/main", "origin/master", "main", "master")) {
            if (refExists(root, candidate)) {
                return candidate;
            }
        }

        throw new IllegalArgumentException("Não foi possível detectar a branch base. Informe --base <ref>.");
    }

    private boolean refExists(Path root, String ref) throws IOException, InterruptedException {
        Process process = new ProcessBuilder("git", "rev-parse", "--verify", "--quiet", ref + "^{commit}")
                .directory(root.toFile())
                .redirectErrorStream(true)
                .start();
        process.getInputStream().transferTo(java.io.OutputStream.nullOutputStream());
        return process.waitFor() == 0;
    }

    private Map<String, MutableFile> parseNameStatus(String output) {
        Map<String, MutableFile> files = new LinkedHashMap<>();
        for (String line : output.lines().toList()) {
            if (line.isBlank()) continue;
            String[] parts = line.split("\\t");
            if (parts.length < 2) continue;

            String rawStatus = parts[0];
            String status = rawStatus.substring(0, 1);
            String path = (status.equals("R") || status.equals("C")) && parts.length >= 3
                    ? parts[2]
                    : parts[1];
            files.put(path, new MutableFile(path, status));
        }
        return files;
    }

    private void parseChangedLines(String patch, Map<String, MutableFile> files) {
        MutableFile current = null;
        for (String line : patch.lines().toList()) {
            if (line.startsWith("+++ b/")) {
                String path = line.substring(6);
                current = files.computeIfAbsent(path, p -> new MutableFile(p, "M"));
                continue;
            }
            if (line.startsWith("+++ /dev/null")) {
                current = null;
                continue;
            }
            if (current == null || !line.startsWith("@@")) continue;

            Matcher matcher = HUNK.matcher(line);
            if (!matcher.matches()) continue;

            int oldStart = Integer.parseInt(matcher.group(1));
            int oldCount = matcher.group(2) == null ? 1 : Integer.parseInt(matcher.group(2));
            int newStart = Integer.parseInt(matcher.group(3));
            int newCount = matcher.group(4) == null ? 1 : Integer.parseInt(matcher.group(4));

            addRange(current.deletedLines, oldStart, oldCount);
            addRange(current.addedLines, newStart, newCount);
        }
    }

    private void addRange(List<Integer> target, int start, int count) {
        for (int i = 0; i < count; i++) {
            target.add(start + i);
        }
    }

    private String tryRun(Path root, String... command) throws IOException, InterruptedException {
        try {
            return run(root, command);
        } catch (GitCommandException ignored) {
            return null;
        }
    }

    private String run(Path root, String... command) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command)
                .directory(root.toFile())
                .redirectErrorStream(true)
                .start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new GitCommandException(String.join(" ", command), exitCode, output.trim());
        }
        return output;
    }

    private static final class MutableFile {
        private final String path;
        private final String status;
        private final List<Integer> addedLines = new ArrayList<>();
        private final List<Integer> deletedLines = new ArrayList<>();

        private MutableFile(String path, String status) {
            this.path = path;
            this.status = status;
        }

        private GitDiffFile toRecord() {
            return new GitDiffFile(path, status, List.copyOf(addedLines), List.copyOf(deletedLines));
        }
    }

    public static class GitCommandException extends RuntimeException {
        public GitCommandException(String command, int exitCode, String output) {
            super("Git command falhou (exit " + exitCode + "): " + command
                    + (output.isBlank() ? "" : " - " + output));
        }
    }
}
