package com.ralvesper.qualitygate;

public class AiReviewPolicy {

    public Decision evaluate(AiReviewResult result) {
        boolean block = result.findings().stream().anyMatch(this::blocks);
        return new Decision(block, block ? "BLOCK" : "PASS");
    }

    private boolean blocks(AiFinding finding) {
        if (finding.confidence() != AiFinding.Confidence.HIGH) {
            return false;
        }
        return finding.severity() == AiFinding.Severity.CRITICAL
                || finding.severity() == AiFinding.Severity.HIGH;
    }

    public record Decision(boolean block, String status) {
    }
}
