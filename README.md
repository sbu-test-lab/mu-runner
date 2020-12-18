# java-mutant-runner
Seed a bug in one of your java class files (called a mutant). Then, make a list of these buggy java classes (mutants). Now you can use `java-mutant-runner` for compiling and running tests:
* `java-mutant-runner` replace each buggy file (mutant) with orignal java source file.
* After rplacing buggy file, `java-mutant-runner` compile our project.
* Now `java-mutant-runner` run your JUnit test suites.
* At the end, `java-mutant-runner` report you, which bugg class is dicoverd by your test (killed by your tests)
