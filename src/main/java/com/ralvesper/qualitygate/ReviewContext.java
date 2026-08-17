package com.ralvesper.qualitygate;

public record ReviewContext(
        String project,
        GitDiffContext git,
        WorkItem workItem,
        ArchitectureContext architecture,
        String architectureDocument
) {
}
