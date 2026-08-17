package com.ralvesper.qualitygate;

import java.util.List;

public record AiReviewResult(
        String provider,
        List<AiFinding> findings,
        String summary
) {
    public AiReviewResult {
        findings = findings == null ? List.of() : List.copyOf(findings);
    }
}
