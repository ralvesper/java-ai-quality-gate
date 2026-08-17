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
}