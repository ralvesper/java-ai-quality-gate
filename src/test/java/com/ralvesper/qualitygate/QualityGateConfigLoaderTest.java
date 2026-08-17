package com.ralvesper.qualitygate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QualityGateConfigLoaderTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldUseDefaultsWhenConfigDoesNotExist() throws Exception {
        QualityGateConfig config = new QualityGateConfigLoader().load(tempDir);

        assertEquals(600, config.maven().timeoutSeconds());
        assertTrue(config.maven().args().isEmpty());
    }

    @Test
    void shouldLoadYamlConfig() throws Exception {
        Files.writeString(tempDir.resolve(".quality-gate.yml"), """
                maven:
                  timeoutSeconds: 120
                  args:
                    - -B
                    - -Ptest
                """);

        QualityGateConfig config = new QualityGateConfigLoader().load(tempDir);

        assertEquals(120, config.maven().timeoutSeconds());
        assertEquals(2, config.maven().args().size());
        assertEquals("-B", config.maven().args().getFirst());
    }
}
