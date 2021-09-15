# MuRunner: a Java Mutants Runner
Seed a bug in one of your java class files (called a mutant). Then, make a list of these buggy java classes (mutants). Now you can use `java-mutant-runner` for compiling and running tests:
* `java-mutant-runner` replaces each buggy file (mutant) with original java source file.
* After replacing a buggy file, `java-mutant-runner` compiles our project.
* Now `java-mutant-runner` runs your JUnit test suites.
* At the end, `java-mutant-runner` reports you, which buggy class is discovered by your tests (killed by your tests)

## Usage
Download the jar of the project with name of `mu-runner.java`. To use this tool do as follow:

- Create a folder named `mutants` which contains your buggy java classes. each file should be name with full package-name. also at the end of the name of file you should add type of mutant and number of mutant for this class, for example these are a valid mutants:
  - `package.className_<typeOfMutant:String>_<Number>.java`
  - `package.name.ClassName_typeOfMutant_theNumberOfMutantInThisType.java`
  - `org.example.MyClass_conditionalOperatorMutant_1.java`
- Create a folder named `project-src` which contains all of original src files of your project.
- Create a folder named `project-test-classes` which contains compiled tests. the test should be use JUnit4.
- [optional] A folder called `jars` contains all JAR file dependencies which need for running your tests.
- now run the tool like this: <br/>
`java -jar mu-runner.jar -tests org.exmple.tests.TestClass1 org.exmple.tests.TestClass2` 
- The output of the tool is something like this: 

![Screenshot](../main/docs/output.png?raw=true)

## Command Parameters
The command options and parameters are as follow:
```shell script
Usage: <MuRunner.jar> 
                    [-m=MUTANTS-DIR] 
                    [-maven=<maven home directory>]
                    [-module=<your maven module directory relative path>]
                    [-project=<your maven project directory>]
                    [-src-dir=<your src directory relative path>] 
                    [-tests=<your compiled tests directory] 
                    [-jars=<jars dependencies directories needed for tests run>]... 
                    [Tests...]
```
The following table shown the descriptions of these parameters and options:

| Option abv. |Option in long format | Description |
| -- | ---- | ----------- |
| [Tests...] | - | One ore more test class to run. You should specify the Name of the test class in Junit. |
| `-jars` | `--test-dependencies-jars` | A list of directories containing Jars file dependency for running tests |
| `-m`| `--mutants` | The mutants directory. A mutant is a java file with the following name `package.name.className_type_number.java` use `[type]` and `[number]` in the file name for categorize and number your mutants. |
| `-maven`| `--maven-home` | The Maven home directory |
| `-module`| `--maven-module` | Your maven module directory relative path. In multi-module maven project you can specify just one of the module as your project |
| `-project`| `--maven-project` | Your maven-based project directory. |
|  `-src-dir`| `--source-directory` | src directory relative to root of maven project (or maven module) like `/src/main/java/` |
| `-tests`| `--compiled-tests` | Your compiled tests directory.The directory containing compiled test classes |
