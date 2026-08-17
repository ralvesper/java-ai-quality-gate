package com.ralvesper.qualitygate;

import picocli.CommandLine.IVersionProvider;

import java.io.InputStream;
import java.util.Properties;

public class BuildVersionProvider implements IVersionProvider {
    @Override
    public String[] getVersion() {
        Properties properties = new Properties();
        try (InputStream in = BuildVersionProvider.class.getResourceAsStream(
                "/META-INF/maven/com.ralvesper/java-ai-quality-gate/pom.properties")) {
            if (in != null) {
                properties.load(in);
                return new String[]{properties.getProperty("version", "dev")};
            }
        } catch (Exception ignored) {
            // Fall back to development version when running from IDE/tests.
        }
        return new String[]{"dev"};
    }
}
