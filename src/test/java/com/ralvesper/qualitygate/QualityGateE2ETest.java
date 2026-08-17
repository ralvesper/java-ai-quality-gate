package com.ralvesper.qualitygate;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QualityGateE2ETest {

    @Test
    void shouldRunReviewAgainstFixtureProject() throws Exception {
        Path project = Path.of(getClass().getResource("/projects/passing").toURI());

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(output));
        try {
            int exitCode = new CommandLine(new ReviewCommand()).execute(
                    "--project", project.toString()
            );

            assertEquals(0, exitCode);
            assertTrue(output.toString().contains("QUALITY GATE: PASS"));
            assertTrue(output.toString().contains("maven-verify: PASS"));
        } finally {
            System.setOut(originalOut);
        }
    }
}
