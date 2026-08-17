package com.ralvesper.qualitygate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class QualityGateConfigLoader {

    private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    public QualityGateConfig load(Path projectPath) throws IOException {
        Path configPath = projectPath.resolve(".quality-gate.yml");
        if (!Files.isRegularFile(configPath)) {
            return QualityGateConfig.defaults();
        }
        return mapper.readValue(configPath.toFile(), QualityGateConfig.class);
    }
}
