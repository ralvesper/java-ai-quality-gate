package com.ralvesper.qualitygate;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GitLabMergeRequestRefTest {

    @Test
    void parsesNestedGitLabMergeRequestUrl() {
        GitLabMergeRequestRef ref = GitLabMergeRequestRef.parse(
                "https://git.pluxee.com.br/core-backoffice/pedefacil1/ebs/-/merge_requests/734"
        );

        assertEquals("https://git.pluxee.com.br/core-backoffice/pedefacil1/ebs", ref.repositoryUrl());
        assertEquals("core-backoffice/pedefacil1/ebs", ref.projectPath());
        assertEquals(734, ref.iid());
    }

    @Test
    void rejectsNonMergeRequestUrl() {
        assertThrows(
                IllegalArgumentException.class,
                () -> GitLabMergeRequestRef.parse("https://git.pluxee.com.br/core-backoffice/pedefacil1/ebs")
        );
    }
}
