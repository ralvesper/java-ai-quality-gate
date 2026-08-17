package com.ralvesper.qualitygate;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkItemIdResolverTest {

    private final WorkItemIdResolver resolver = new WorkItemIdResolver();

    @Test
    void resolvesFsKeyFromBranch() {
        var result = resolver.resolveFromText(
                "feature/FS-687-corrigir-boletos",
                List.of("(FS-\\d+)", "(PLUX-\\d+)")
        );

        assertTrue(result.isPresent());
        assertEquals("FS-687", result.get());
    }

    @Test
    void returnsEmptyWhenNoPatternMatches() {
        var result = resolver.resolveFromText(
                "feature/refactor-payment-service",
                List.of("(FS-\\d+)", "(PLUX-\\d+)")
        );

        assertTrue(result.isEmpty());
    }
}
