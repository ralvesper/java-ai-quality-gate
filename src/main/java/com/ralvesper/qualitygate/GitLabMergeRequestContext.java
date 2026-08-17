package com.ralvesper.qualitygate;

public record GitLabMergeRequestContext(
        String provider,
        int id,
        String title,
        String description,
        String sourceBranch,
        String targetBranch,
        String state,
        String author,
        String url,
        String projectPath,
        String patch
) {
}
