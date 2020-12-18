# java-mutant-runner
Seed a bug in one of your java class files (called a mutant). Then, make a list of these buggy java classes (mutants). Now you can use `java-mutant-runner` for compiling and running tests:
* `java-mutant-runner` replaces each buggy file (mutant) with orignal java source file.
* After rplacing buggy file, `java-mutant-runner` compiles our project.
* Now `java-mutant-runner` runs your JUnit test suites.
* At the end, `java-mutant-runner` reports you, which bugg class is dicoverd by your tests (killed by your tests)
