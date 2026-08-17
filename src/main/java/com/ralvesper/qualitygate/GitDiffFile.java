package com.ralvesper.qualitygate;

import java.util.List;

public record GitDiffFile(
        String path,
        String status,
        List<Integer> addedLines,
        List<Integer> deletedLines
) {
}
