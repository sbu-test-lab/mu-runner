package sbu.testlab.mutants.runner;


import picocli.CommandLine;

public class MavenBasedMutantRunnerLauncher {
    public static void main(String... args) {
        int exitCode;
        exitCode = new CommandLine(new MavenBasedMutantRunnerCommand()).execute(args);
        System.exit(exitCode);

    }
}
