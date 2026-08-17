package com.ralvesper.qualitygate;

import com.fasterxml.jackson.databind.ObjectMapper;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.util.concurrent.Callable;

@Command(
        name = "gitlab-mr",
        mixinStandardHelpOptions = true,
        description = "Carrega metadata e diff de um Merge Request GitLab a partir da URL completa."
)
public class GitLabMrCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "URL completa do Merge Request GitLab.")
    private String mergeRequestUrl;

    @Override
    public Integer call() {
        try {
            GitLabMergeRequestContext context = new GitLabMergeRequestProvider().load(mergeRequestUrl);
            System.out.println(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(context));
            return 0;
        } catch (Exception e) {
            System.err.println("Erro ao carregar Merge Request GitLab: " + e.getMessage());
            return 1;
        }
    }
}
