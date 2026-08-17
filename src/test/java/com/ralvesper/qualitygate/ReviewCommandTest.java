package com.ralvesper.qualitygate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ReviewCommandTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldDefaultProjectToCurrentDirectory() throws Exception {
        ReviewCommand command = new ReviewCommand();
        new CommandLine(command).parseArgs();

        Field field = ReviewCommand.class.getDeclaredField("project");
        field.setAccessible(true);
        Path project = (Path) field.get(command);

        assertEquals(Path.of("."), project);
    }

    @Test
    void shouldReturnPassForSuccessfulProject() throws Exception {
        Path projectDir = createProject("text-project");
        createSuccessfulWrapper(projectDir);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(output));
        try {
            int exitCode = new CommandLine(new ReviewCommand()).execute(
                    "--project", projectDir.toString()
            );

            assertEquals(0, exitCode);
            assertTrue(output.toString().contains("QUALITY GATE: PASS"));
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    void shouldProduceValidJson() throws Exception {
        Path projectDir = createProject("json-project");
        createSuccessfulWrapper(projectDir);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(output));
        try {
            int exitCode = new CommandLine(new ReviewCommand()).execute(
                    "--project", projectDir.toString(),
                    "--format", "json"
            );

            assertEquals(0, exitCode);
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            var root = mapper.readTree(output.toString().trim());

            assertEquals("PASS", root.get("status").asText());
            assertEquals("maven-verify", root.get("checks").get(0).get("gate").asText());
            assertTrue(root.get("checks").get(0).has("durationMs"));
        } finally {
            System.setOut(originalOut);
        }
    }

    private Path createProject(String name) throws Exception {
        Path projectDir = tempDir.resolve(name);
        Files.createDirectories(projectDir);
        Files.writeString(projectDir.resolve("pom.xml"), """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.test</groupId>
                    <artifactId>fixture</artifactId>
                    <version>1.0.0</version>
                </project>
                """);
        return projectDir;
    }

    private void createSuccessfulWrapper(Path projectDir) throws Exception {
        boolean windows = System.getProperty("os.name").toLowerCase().contains("win");
        if (windows) {
            Files.writeString(projectDir.resolve("mvnw.cmd"), "@echo off\necho Mock Wrapper\nexit /b 0");
        } else {
            Path wrapper = projectDir.resolve("mvnw");
            Files.writeString(wrapper, "#!/bin/sh\necho \"Mock Wrapper\"\nexit 0");
            wrapper.toFile().setExecutable(true, false);
        }
    }
}
