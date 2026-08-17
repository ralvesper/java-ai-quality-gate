package com.ralvesper.qualitygate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class MavenBuildCheck implements QualityCheck {

    private final String mvnArgs;
    private final PrintStream outputStream;

    public MavenBuildCheck() {
        this(null, System.out);
    }

    public MavenBuildCheck(String mvnArgs, PrintStream outputStream) {
        this.mvnArgs = mvnArgs;
        this.outputStream = outputStream;
    }

    @Override
    public GateResult execute(ProjectContext context) {
        if (!Files.isRegularFile(context.projectPath().resolve("pom.xml"))) {
            return new GateResult("maven-verify", GateStatus.FAIL, "pom.xml não encontrado");
        }

        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        List<String> command = new ArrayList<>();

        if (isWindows) {
            Path mvnwCmd = context.projectPath().resolve("mvnw.cmd");
            if (Files.isRegularFile(mvnwCmd)) {
                command.add(mvnwCmd.toAbsolutePath().toString());
            } else {
                command.add("mvn.cmd");
            }
        } else {
            Path mvnw = context.projectPath().resolve("mvnw");
            if (Files.isRegularFile(mvnw)) {
                if (!Files.isExecutable(mvnw)) {
                    mvnw.toFile().setExecutable(true, false);
                }
                command.add(mvnw.toAbsolutePath().toString());
            } else {
                command.add("mvn");
            }
        }

        command.add("verify");

        if (mvnArgs != null && !mvnArgs.isBlank()) {
            String[] args = mvnArgs.trim().split("\\s+");
            command.addAll(List.of(args));
        }

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(context.projectPath().toFile());
        processBuilder.redirectErrorStream(true);

        try {
            Process process = processBuilder.start();

            // Capturar saída e escrever no outputStream em tempo real para evitar corrupção de canal do Surefire
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (outputStream != null) {
                        outputStream.println(line);
                    }
                }
            }

            int exitCode = process.waitFor();

            String commandName = command.getFirst().contains("mvnw") ? "./mvnw" : "mvn";

            if (exitCode == 0) {
                return new GateResult("maven-verify", GateStatus.PASS, commandName + " verify executado com sucesso");
            }

            return new GateResult("maven-verify", GateStatus.FAIL, commandName + " verify retornou exit code " + exitCode);
        } catch (IOException e) {
            return new GateResult("maven-verify", GateStatus.FAIL, "Falha ao executar Maven: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new GateResult("maven-verify", GateStatus.FAIL, "Execução interrompida");
        }
    }
}
