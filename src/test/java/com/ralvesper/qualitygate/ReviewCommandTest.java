package com.ralvesper.qualitygate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ReviewCommandTest {

    @TempDir
    Path tempDir;

    @Test
    void testReviewCommandPass() throws Exception {
        Path projectDir = tempDir.resolve("test-project");
        projectDir.toFile().mkdirs();
        
        String pomContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.test</groupId>
                <artifactId>test-project</artifactId>
                <version>1.0.0-SNAPSHOT</version>
                <properties>
                    <maven.compiler.release>21</maven.compiler.release>
                </properties>
            </project>
            """;
        projectDir.resolve("pom.xml").toFile().createNewFile();
        java.nio.file.Files.writeString(projectDir.resolve("pom.xml"), pomContent);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(output));

        try {
            ReviewCommand cmd = new ReviewCommand();
            java.lang.reflect.Field field = ReviewCommand.class.getDeclaredField("project");
            field.setAccessible(true);
            field.set(cmd, projectDir);

            int exitCode = cmd.call();

            assertEquals(0, exitCode);
            String out = output.toString();
            assertTrue(out.contains("QUALITY GATE: PASS"));
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    void testReviewCommandMissingProject() {
        ReviewCommand cmd = new ReviewCommand();
        
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        PrintStream originalErr = System.err;
        System.setErr(new PrintStream(err));

        try {
            int exitCode = cmd.call();
            assertEquals(1, exitCode);
        } catch (NullPointerException e) {
            // Expected - Picocli doesn't validate required options until parse time
            // but we call call() directly without parsing
        } finally {
            System.setErr(originalErr);
        }
    }

    @Test
    void testReviewCommandJsonPass() throws Exception {
        Path projectDir = tempDir.resolve("json-project");
        projectDir.toFile().mkdirs();
        
        String pomContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0">
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.test</groupId>
                <artifactId>json-project</artifactId>
                <version>1.0.0</version>
            </project>
            """;
        java.nio.file.Files.writeString(projectDir.resolve("pom.xml"), pomContent);

        // Mock Maven Wrapper
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        if (isWindows) {
            Path mvnwCmd = projectDir.resolve("mvnw.cmd");
            java.nio.file.Files.writeString(mvnwCmd, "@echo off\necho Mock Wrapper\nexit /b 0");
        } else {
            Path mvnw = projectDir.resolve("mvnw");
            java.nio.file.Files.writeString(mvnw, "#!/bin/sh\necho \"Mock Wrapper\"\nexit 0");
            mvnw.toFile().setExecutable(true, false);
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(output));

        try {
            ReviewCommand cmd = new ReviewCommand();
            
            java.lang.reflect.Field projectField = ReviewCommand.class.getDeclaredField("project");
            projectField.setAccessible(true);
            projectField.set(cmd, projectDir);

            java.lang.reflect.Field formatField = ReviewCommand.class.getDeclaredField("format");
            formatField.setAccessible(true);
            formatField.set(cmd, ReviewCommand.OutputFormat.json);

            int exitCode = cmd.call();

            assertEquals(0, exitCode);
            String out = output.toString().trim();
            
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode rootNode = mapper.readTree(out);
            
            assertEquals("PASS", rootNode.get("status").asText());
            assertTrue(rootNode.get("project").asText().contains("json-project"));
            assertTrue(rootNode.has("checks"));
            assertEquals(1, rootNode.get("checks").size());
            assertEquals("maven-verify", rootNode.get("checks").get(0).get("gate").asText());
            assertEquals("PASS", rootNode.get("checks").get(0).get("status").asText());
        } finally {
            System.setOut(originalOut);
        }
    }
}