package com.ralvesper.qualitygate;

import java.util.List;

public record WorkItem(
        String id,
        String title,
        String context,
        String objective,
        List<String> acceptanceCriteria,
        List<String> testScenarios,
        String source,
        String url
) {
    public WorkItem {
        acceptanceCriteria = acceptanceCriteria == null ? List.of() : List.copyOf(acceptanceCriteria);
        testScenarios = testScenarios == null ? List.of() : List.copyOf(testScenarios);
    }
}
