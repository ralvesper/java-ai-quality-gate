package com.ralvesper.qualitygate;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
        name = "java-ai-quality-gate",
        mixinStandardHelpOptions = true,
        versionProvider = BuildVersionProvider.class,
        description = "Quality gate determinístico e assistido por IA para projetos Java.",
        subcommands = {
                ReviewCommand.class,
                ArchitectureCommand.class
        }
)
public class Main implements Runnable {

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public void run() {
        CommandLine.usage(this, System.out);
    }
}
