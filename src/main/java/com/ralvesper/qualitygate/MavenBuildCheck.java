package com.ralvesper.qualitygate;

import java.io.IOException;
import java.nio.file.Files;

public class MavenBuildCheck implements QualityCheck {

    @Override
    public GateResult execute(ProjectContext context) {
        if (!Files.isRegularFile(context.projectPath().resolve("pom.xml"))) {
            return new GateResult("maven-verify", GateStatus.FAIL, "pom.xml não encontrado");
        }

        ProcessBuilder processBuilder = new ProcessBuilder("mvn", "verify");
        processBuilder.directory(context.projectPath().toFile());
        processBuilder.inheritIO();

        try {
            Process process = processBuilder.start();
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                return new GateResult("maven-verify", GateStatus.PASS, "mvn verify executado com sucesso");
            }

            return new GateResult("maven-verify", GateStatus.FAIL, "mvn verify retornou exit code " + exitCode);
        } catch (IOException e) {
            return new GateResult("maven-verify", GateStatus.FAIL, "Falha ao executar Maven: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new GateResult("maven-verify", GateStatus.FAIL, "Execução interrompida");
        }
    }
}
