package com.ralvesper.qualitygate;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MavenBuildCheck implements QualityCheck {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(10);

    private final List<String> mvnArgs;
    private final PrintStream outputStream;
    private final Duration timeout;
    private final ProcessRunner processRunner;

    public MavenBuildCheck() {
        this(List.of(), System.out, DEFAULT_TIMEOUT, new ProcessRunner());
    }

    public MavenBuildCheck(String mvnArgs, PrintStream outputStream) {
        this(parseLegacyArgs(mvnArgs), outputStream, DEFAULT_TIMEOUT, new ProcessRunner());
    }

    public MavenBuildCheck(List<String> mvnArgs, PrintStream outputStream, Duration timeout) {
        this(mvnArgs, outputStream, timeout, new ProcessRunner());
    }

    MavenBuildCheck(List<String> mvnArgs, PrintStream outputStream, Duration timeout, ProcessRunner processRunner) {
        this.mvnArgs = List.copyOf(mvnArgs == null ? List.of() : mvnArgs);
        this.outputStream = outputStream;
        this.timeout = timeout == null ? DEFAULT_TIMEOUT : timeout;
        this.processRunner = processRunner;
    }

    @Override
    public GateResult execute(ProjectContext context) {
        if (!Files.isDirectory(context.projectPath())) {
            return new GateResult("maven-verify", GateStatus.ERROR, "diretório do projeto não encontrado");
        }
        if (!Files.isRegularFile(context.projectPath().resolve("pom.xml"))) {
            return new GateResult("maven-verify", GateStatus.FAIL, "pom.xml não encontrado");
        }

        List<String> command = resolveMavenCommand(context.projectPath());
        command.add("verify");
        command.addAll(mvnArgs);

        try {
            ProcessResult processResult = processRunner.run(command, context.projectPath(), timeout, outputStream);
            long durationMs = processResult.duration().toMillis();
            String commandName = displayCommand(command);

            if (processResult.timedOut()) {
                return new GateResult(
                        "maven-verify",
                        GateStatus.ERROR,
                        commandName + " excedeu timeout de " + timeout.toSeconds() + "s",
                        durationMs
                );
            }
            if (processResult.exitCode() == 0) {
                return new GateResult(
                        "maven-verify",
                        GateStatus.PASS,
                        commandName + " executado com sucesso",
                        durationMs
                );
            }
            return new GateResult(
                    "maven-verify",
                    GateStatus.FAIL,
                    commandName + " retornou exit code " + processResult.exitCode(),
                    durationMs
            );
        } catch (IOException e) {
            return new GateResult("maven-verify", GateStatus.ERROR, "Falha ao executar Maven: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new GateResult("maven-verify", GateStatus.ERROR, "Execução interrompida");
        }
    }

    private List<String> resolveMavenCommand(Path projectPath) {
        boolean windows = System.getProperty("os.name").toLowerCase().contains("win");
        List<String> command = new ArrayList<>();

        if (windows) {
            Path wrapper = projectPath.resolve("mvnw.cmd");
            command.add(Files.isRegularFile(wrapper) ? wrapper.toAbsolutePath().toString() : "mvn.cmd");
            return command;
        }

        Path wrapper = projectPath.resolve("mvnw");
        if (Files.isRegularFile(wrapper)) {
            if (Files.isExecutable(wrapper)) {
                command.add(wrapper.toAbsolutePath().toString());
            } else {
                command.add("sh");
                command.add(wrapper.toAbsolutePath().toString());
            }
        } else {
            command.add("mvn");
        }
        return command;
    }

    private String displayCommand(List<String> command) {
        if (command.size() >= 2 && "sh".equals(command.getFirst()) && command.get(1).endsWith("mvnw")) {
            return "sh ./mvnw verify";
        }
        return command.getFirst().contains("mvnw") ? "./mvnw verify" : "mvn verify";
    }

    private static List<String> parseLegacyArgs(String args) {
        if (args == null || args.isBlank()) {
            return List.of();
        }
        return Arrays.asList(args.trim().split("\\s+"));
    }
}
