package com.ralvesper.qualitygate;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MarkdownWorkItemParser {

    public WorkItem parse(String id, String title, String body, String source, String url) {
        String safeBody = body == null ? "" : body;
        String context = section(safeBody, "Contexto");
        String objective = section(safeBody, "Objetivo");
        List<String> acceptanceCriteria = checklist(section(safeBody, "Critérios de aceite"));
        List<String> testScenarios = headings(section(safeBody, "Cenários de teste"));

        return new WorkItem(
                id,
                title,
                context,
                objective,
                acceptanceCriteria,
                testScenarios,
                source,
                url
        );
    }

    String section(String markdown, String heading) {
        String[] lines = markdown.split("\\R");
        String target = heading.toLowerCase(Locale.ROOT);
        StringBuilder result = new StringBuilder();
        boolean collecting = false;
        int level = 0;

        for (String line : lines) {
            if (line.matches("^#{1,6}\\s+.*")) {
                int currentLevel = headingLevel(line);
                String text = line.substring(currentLevel).trim().toLowerCase(Locale.ROOT);
                if (!collecting && text.equals(target)) {
                    collecting = true;
                    level = currentLevel;
                    continue;
                }
                if (collecting && currentLevel <= level) {
                    break;
                }
            }

            if (collecting) {
                result.append(line).append('\n');
            }
        }

        return result.toString().trim();
    }

    private int headingLevel(String line) {
        int i = 0;
        while (i < line.length() && line.charAt(i) == '#') {
            i++;
        }
        return i;
    }

    private List<String> checklist(String section) {
        List<String> items = new ArrayList<>();
        for (String line : section.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.matches("^- \\[[ xX]\\] .+")) {
                items.add(trimmed.replaceFirst("^- \\[[ xX]\\]\\s*", ""));
            }
        }
        return items;
    }

    private List<String> headings(String section) {
        List<String> items = new ArrayList<>();
        for (String line : section.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.matches("^#{3,6}\\s+.+")) {
                items.add(trimmed.replaceFirst("^#{3,6}\\s+", ""));
            }
        }
        return items;
    }
}
