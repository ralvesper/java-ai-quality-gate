package com.ralvesper.qualitygate;

import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ArchitectureDetector {

    private static final List<String> DOCUMENTATION_CANDIDATES = List.of(
            "ARCHITECTURE.md",
            "docs/architecture.md",
            "DESIGN.md",
            "CONTEXT.md"
    );

    public ArchitectureContext detect(Path projectPath) {
        Path root = projectPath.toAbsolutePath().normalize();
        String projectName = detectProjectName(root);
        List<String> evidence = new ArrayList<>();

        for (String candidate : DOCUMENTATION_CANDIDATES) {
            Path documentation = root.resolve(candidate);
            if (Files.isRegularFile(documentation)) {
                evidence.add("Architecture documentation found: " + candidate);
                return new ArchitectureContext(
                        projectName,
                        ArchitectureContext.ArchitectureMode.EXPLICIT,
                        "documented",
                        ArchitectureContext.Confidence.HIGH,
                        documentation,
                        evidence
                );
            }
        }

        Path javaRoot = root.resolve("src/main/java");
        if (!Files.isDirectory(javaRoot)) {
            return new ArchitectureContext(
                    projectName,
                    ArchitectureContext.ArchitectureMode.UNKNOWN,
                    "unknown",
                    ArchitectureContext.Confidence.LOW,
                    null,
                    List.of("src/main/java not found")
            );
        }

        boolean controller = containsDirectory(javaRoot, "controller");
        boolean service = containsDirectory(javaRoot, "service");
        boolean repository = containsDirectory(javaRoot, "repository");
        boolean domain = containsDirectory(javaRoot, "domain");
        boolean application = containsDirectory(javaRoot, "application");
        boolean infrastructure = containsDirectory(javaRoot, "infrastructure");
        boolean port = containsDirectory(javaRoot, "port") || containsDirectory(javaRoot, "ports");
        boolean adapter = containsDirectory(javaRoot, "adapter") || containsDirectory(javaRoot, "adapters");

        if (controller) evidence.add("controller package/directory detected");
        if (service) evidence.add("service package/directory detected");
        if (repository) evidence.add("repository package/directory detected");
        if (domain) evidence.add("domain package/directory detected");
        if (application) evidence.add("application package/directory detected");
        if (infrastructure) evidence.add("infrastructure package/directory detected");
        if (port) evidence.add("port(s) package/directory detected");
        if (adapter) evidence.add("adapter(s) package/directory detected");

        if (port && adapter && domain) {
            return inferred(projectName, "hexagonal", ArchitectureContext.Confidence.HIGH, evidence);
        }

        if (domain && application && infrastructure) {
            return inferred(projectName, "clean-or-hexagonal", ArchitectureContext.Confidence.MEDIUM, evidence);
        }

        if (controller && service && repository) {
            return inferred(projectName, "layered", ArchitectureContext.Confidence.HIGH, evidence);
        }

        if (!evidence.isEmpty()) {
            return inferred(projectName, "custom", ArchitectureContext.Confidence.MEDIUM, evidence);
        }

        return new ArchitectureContext(
                projectName,
                ArchitectureContext.ArchitectureMode.UNKNOWN,
                "unknown",
                ArchitectureContext.Confidence.LOW,
                null,
                List.of("No strong architecture signals detected")
        );
    }

    private ArchitectureContext inferred(String projectName, String style,
                                         ArchitectureContext.Confidence confidence,
                                         List<String> evidence) {
        return new ArchitectureContext(
                projectName,
                ArchitectureContext.ArchitectureMode.INFERRED,
                style,
                confidence,
                null,
                evidence
        );
    }

    private boolean containsDirectory(Path root, String name) {
        try (var paths = Files.walk(root)) {
            String expected = name.toLowerCase(Locale.ROOT);
            return paths
                    .filter(Files::isDirectory)
                    .map(path -> path.getFileName() == null ? "" : path.getFileName().toString().toLowerCase(Locale.ROOT))
                    .anyMatch(expected::equals);
        } catch (Exception e) {
            return false;
        }
    }

    private String detectProjectName(Path root) {
        Path pom = root.resolve("pom.xml");
        if (Files.isRegularFile(pom)) {
            try {
                var factory = DocumentBuilderFactory.newInstance();
                factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
                factory.setExpandEntityReferences(false);
                Document document = factory.newDocumentBuilder().parse(pom.toFile());
                var artifactIds = document.getDocumentElement().getElementsByTagName("artifactId");
                if (artifactIds.getLength() > 0) {
                    String artifactId = artifactIds.item(0).getTextContent().trim();
                    if (!artifactId.isBlank()) {
                        return artifactId;
                    }
                }
            } catch (Exception ignored) {
                // Fallback to directory name below.
            }
        }

        Path fileName = root.getFileName();
        return fileName == null ? root.toString() : fileName.toString();
    }
}
