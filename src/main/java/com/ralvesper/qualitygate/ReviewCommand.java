package com.ralvesper.qualitygate;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(
        name = "review",
        mixinStandardHelpOptions = true,
        description = "Executa o quality gate em um projeto Java/Maven."
)
public class ReviewCommand implements Callable<Integer> {

    @Option(
            names = "--project",
            required = true,
            description = "Diretório raiz do projeto Java/Maven."
    )
    private Path project;

    @Override
    public Integer call() {
        ProjectContext context = new ProjectContext(project.toAbsolutePath().normalize());
        GateResult result = new MavenBuildCheck().execute(context);

        System.out.println();
        System.out.println("Java AI Quality Gate");
        System.out.println("--------------------");
        System.out.println("Project: " + context.projectPath());
        System.out.println("Maven verify: " + result.status());
        System.out.println("Message: " + result.message());
        System.out.println();

        if (result.status() == GateStatus.FAIL) {
            System.out.println("QUALITY GATE: BLOCK");
            return 1;
        }

        System.out.println("QUALITY GATE: PASS");
        return 0;
    }
}
