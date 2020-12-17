# java-mutant-runner
* Seed a bug in one of your java class files (called a mutant). 
* Make a list of these buggy java classes (mutants). 
* Now use `java-mutant-runner` for compiling and running tests:
* `java-mutant-runner` replace each buggy file (mutant) with orignal java source file.
* `java-mutant-runner` after rplacing buggy file, it compile our project.
* Now `java-mutant-runner` run your JUnit test suites.
* At the end, `java-mutant-runner` report you, which bugg is dicoverd by your test (killed by your tests)
