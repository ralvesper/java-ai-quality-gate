package com.ralvesper.qualitygate;

import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record GitLabMergeRequestRef(
        String url,
        String repositoryUrl,
        String projectPath,
        int iid
) {
    private static final Pattern MR_PATH = Pattern.compile("^(.+?)/-/merge_requests/(\\d+)(?:/.*)?$");

    public static GitLabMergeRequestRef parse(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            throw new IllegalArgumentException("URL do merge request não informada.");
        }

        URI uri = URI.create(rawUrl.trim());
        if (uri.getScheme() == null || uri.getHost() == null) {
            throw new IllegalArgumentException("URL de merge request inválida: " + rawUrl);
        }

        String path = uri.getPath();
        Matcher matcher = MR_PATH.matcher(path);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("URL não corresponde a um merge request GitLab: " + rawUrl);
        }

        String projectPath = matcher.group(1);
        if (projectPath.startsWith("/")) {
            projectPath = projectPath.substring(1);
        }

        int iid = Integer.parseInt(matcher.group(2));
        String repositoryUrl = uri.getScheme() + "://" + uri.getAuthority() + "/" + projectPath;

        return new GitLabMergeRequestRef(rawUrl.trim(), repositoryUrl, projectPath, iid);
    }
}
