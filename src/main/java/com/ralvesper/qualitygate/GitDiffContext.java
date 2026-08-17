package com.ralvesper.qualitygate;

import java.util.List;

public record GitDiffContext(
        String currentBranch,
        String baseRef,
        String mergeBase,
        List<GitDiffFile> files,
        String patch
) {
}
