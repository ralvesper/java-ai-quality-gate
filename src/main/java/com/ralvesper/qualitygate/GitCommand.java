package com.ralvesper.qualitygate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(
        name = "git",
        mixinStandardHelpOptions = true,
        description = "Analisa o contexto Git e as alterações em relação a uma branch base."
)
public class GitCommand implements Callable<Integer> {

    @Option(names = "--project", description = "Diretório do projeto. Default: diretório atual.")
    private Path project = Path.of(".");

    @Option(names = "--base", description = "Branch/ref base. Se omitida, tenta origin/HEAD, origin/main, origin/master, main ou master.")
    private String base;

    @Option(names = "--commit", description = "Hash/tag/ref de um commit específico. Diffa o commit contra o pai.")
    private String commitRef;

    @Option(names = {"-f", "--format"}, defaultValue = "text", description = "Formato de saída: text ou json.")
    private OutputFormat format;

    public enum OutputFormat { text, json }

    @Override
    public Integer call() {
        try {
            GitDiffContext context = new GitContextAnalyzer().analyze(project, base, commitRef);
            if (format == OutputFormat.json) {
                ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
                System.out.println(mapper.writeValueAsString(context));
            } else {
                printText(context);
            }
            return 0;
        } catch (Exception e) {
            System.err.println("Erro ao analisar Git: " + e.getMessage());
            return 1;
        }
    }

    private void printText(GitDiffContext context) {
        System.out.println("Git Context");
        System.out.println("-----------");
        System.out.println("Current branch: " + context.currentBranch());
        System.out.println("Base: " + context.baseRef());
        System.out.println("Merge base: " + context.mergeBase());
        System.out.println("Changed files: " + context.files().size());
        System.out.println();

        for (GitDiffFile file : context.files()) {
            System.out.printf("%s %s (+%d/-%d)%n",
                    file.status(), file.path(), file.addedLines().size(), file.deletedLines().size());
        }
    }
}
