package com.ralvesper.qualitygate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class MavenBuildCheckTest {

    @TempDir
    Path tempDir;

    @Test
    void testExecutePass() throws Exception {
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

        ProjectContext context = new ProjectContext(projectDir);
        GateResult result = new MavenBuildCheck().execute(context);

        assertEquals(GateStatus.PASS, result.status());
        assertTrue(result.message().contains("executado com sucesso"));
    }

    @Test
    void testExecuteFailNoPom() {
        Path projectDir = tempDir.resolve("empty-project");
        projectDir.toFile().mkdirs();

        ProjectContext context = new ProjectContext(projectDir);
        GateResult result = new MavenBuildCheck().execute(context);

        assertEquals(GateStatus.FAIL, result.status());
        assertTrue(result.message().contains("pom.xml não encontrado"));
    }

    @Test
    void testExecuteFailInvalidPom() throws Exception {
        Path projectDir = tempDir.resolve("bad-project");
        projectDir.toFile().mkdirs();
        
        String pomContent = "not valid xml";
        projectDir.resolve("pom.xml").toFile().createNewFile();
        java.nio.file.Files.writeString(projectDir.resolve("pom.xml"), pomContent);

        ProjectContext context = new ProjectContext(projectDir);
        GateResult result = new MavenBuildCheck().execute(context);

        assertEquals(GateStatus.FAIL, result.status());
    }

    @Test
    void testExecuteWithMavenWrapper() throws Exception {
        Path projectDir = tempDir.resolve("wrapper-project");
        projectDir.toFile().mkdirs();
        
        String pomContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.test</groupId>
                <artifactId>wrapper-project</artifactId>
                <version>1.0.0-SNAPSHOT</version>
            </project>
            """;
        java.nio.file.Files.writeString(projectDir.resolve("pom.xml"), pomContent);

        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        if (isWindows) {
            Path mvnwCmd = projectDir.resolve("mvnw.cmd");
            java.nio.file.Files.writeString(mvnwCmd, "@echo off\necho Mock Maven Wrapper\nexit /b 0");
        } else {
            Path mvnw = projectDir.resolve("mvnw");
            java.nio.file.Files.writeString(mvnw, "#!/bin/sh\necho \"Mock Maven Wrapper\"\nexit 0");
            mvnw.toFile().setExecutable(true, false);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(out);

        ProjectContext context = new ProjectContext(projectDir);
        GateResult result = new MavenBuildCheck(null, printStream).execute(context);

        assertEquals(GateStatus.PASS, result.status());
        assertTrue(result.message().contains("./mvnw verify"));
        assertTrue(out.toString().contains("Mock Maven Wrapper"));
    }

    @Test
    void testExecuteWithCustomArgs() throws Exception {
        Path projectDir = tempDir.resolve("args-project");
        projectDir.toFile().mkdirs();
        
        java.nio.file.Files.writeString(projectDir.resolve("pom.xml"), """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0">
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.test</groupId>
                <artifactId>args-project</artifactId>
                <version>1.0.0</version>
            </project>
            """);

        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        if (isWindows) {
            Path mvnwCmd = projectDir.resolve("mvnw.cmd");
            java.nio.file.Files.writeString(mvnwCmd, "@echo off\necho Mock Wrapper Args: %*\nexit /b 0");
        } else {
            Path mvnw = projectDir.resolve("mvnw");
            java.nio.file.Files.writeString(mvnw, "#!/bin/sh\necho \"Mock Wrapper Args: $@\"\nexit 0");
            mvnw.toFile().setExecutable(true, false);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(out);

        ProjectContext context = new ProjectContext(projectDir);
        MavenBuildCheck check = new MavenBuildCheck("-B -Pprofile", printStream);
        GateResult result = check.execute(context);

        assertEquals(GateStatus.PASS, result.status());
        String logs = out.toString();
        assertTrue(logs.contains("-B"));
        assertTrue(logs.contains("-Pprofile"));
    }
}