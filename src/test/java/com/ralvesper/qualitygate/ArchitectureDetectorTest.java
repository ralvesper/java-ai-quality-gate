package com.ralvesper.qualitygate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchitectureDetectorTest {

    @TempDir
    Path tempDir;

    @Test
    void detectsExplicitArchitectureDocument() throws Exception {
        Files.writeString(tempDir.resolve("ARCHITECTURE.md"), "# Architecture");

        ArchitectureContext context = new ArchitectureDetector().detect(tempDir);

        assertEquals(ArchitectureContext.ArchitectureMode.EXPLICIT, context.mode());
        assertEquals(ArchitectureContext.Confidence.HIGH, context.confidence());
        assertTrue(context.documentation().endsWith("ARCHITECTURE.md"));
    }

    @Test
    void detectsLayeredArchitectureFromDirectoryEvidence() throws Exception {
        Path javaRoot = tempDir.resolve("src/main/java/com/example");
        Files.createDirectories(javaRoot.resolve("controller"));
        Files.createDirectories(javaRoot.resolve("service"));
        Files.createDirectories(javaRoot.resolve("repository"));
        Files.writeString(tempDir.resolve("pom.xml"), """
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.example</groupId>
                  <artifactId>customer-service</artifactId>
                  <version>1.0.0</version>
                </project>
                """);

        ArchitectureContext context = new ArchitectureDetector().detect(tempDir);

        assertEquals("customer-service", context.projectName());
        assertEquals(ArchitectureContext.ArchitectureMode.INFERRED, context.mode());
        assertEquals("layered", context.style());
        assertEquals(ArchitectureContext.Confidence.HIGH, context.confidence());
        assertTrue(context.evidence().stream().anyMatch(e -> e.contains("controller")));
    }

    @Test
    void returnsUnknownWhenNoArchitectureSignalsExist() {
        ArchitectureContext context = new ArchitectureDetector().detect(tempDir);

        assertEquals(ArchitectureContext.ArchitectureMode.UNKNOWN, context.mode());
        assertEquals("unknown", context.style());
        assertEquals(ArchitectureContext.Confidence.LOW, context.confidence());
    }
}
