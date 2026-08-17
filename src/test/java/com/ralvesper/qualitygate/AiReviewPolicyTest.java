package com.ralvesper.qualitygate;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiReviewPolicyTest {

    private final AiReviewPolicy policy = new AiReviewPolicy();

    @Test
    void blocksHighSeverityWithHighConfidence() {
        AiReviewResult result = new AiReviewResult("test", List.of(
                finding(AiFinding.Severity.HIGH, AiFinding.Confidence.HIGH)
        ), "summary");

        assertTrue(policy.evaluate(result).block());
    }

    @Test
    void blocksCriticalSeverityWithHighConfidence() {
        AiReviewResult result = new AiReviewResult("test", List.of(
                finding(AiFinding.Severity.CRITICAL, AiFinding.Confidence.HIGH)
        ), "summary");

        assertTrue(policy.evaluate(result).block());
    }

    @Test
    void doesNotBlockMediumOrLowConfidenceFindings() {
        AiReviewResult result = new AiReviewResult("test", List.of(
                finding(AiFinding.Severity.HIGH, AiFinding.Confidence.MEDIUM),
                finding(AiFinding.Severity.MEDIUM, AiFinding.Confidence.HIGH)
        ), "summary");

        assertFalse(policy.evaluate(result).block());
    }

    private AiFinding finding(AiFinding.Severity severity, AiFinding.Confidence confidence) {
        return new AiFinding(
                AiFinding.Category.CORRECTNESS,
                severity,
                confidence,
                "Example.java",
                10,
                "Example finding",
                "Evidence"
        );
    }
}
