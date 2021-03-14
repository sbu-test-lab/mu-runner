package sbu.testlab.mutants.runner.maven;

import dnl.utils.text.table.TextTable;
import org.apache.commons.io.FileUtils;
import org.apache.maven.shared.invoker.*;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.*;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.concurrent.Callable;

import static org.apache.commons.io.FileUtils.listFiles;

@Command
public class MavenBasedMutantRunnerCommand implements Callable<Integer> {
    final File TEMP_DIRECTORY = new File(".temp");

    @Option(names = {"-m", "--mutants"}, paramLabel = "MUTANTS-DIR", description = "The mutants directory. A mutant is a java file with the following name\n" +
            "package.name.className_type_number.java \n use [type] and [number] in the file name for categorize and number your mutants.", defaultValue = "mutants")
    File mutantDir;

    @Option(names = {"-project", "--maven-project"}, paramLabel = "<your maven project directory>", description = "The maven project directory.", defaultValue = "project")
    File projectDir;

    @Option(names = {"-module", "--maven-module"}, paramLabel = "<your maven module directory relative path>", description = "In multi-module maven project you can specify just one of the module as your project")
    String projectModuleDirRelativePath;

    @Option(names = {"-src-dir", "--source-directory"}, paramLabel = "<your src directory relative path>", description = "src directory relative to root of maven project (or maven module) like /src/main/java/", defaultValue = "/src/main/java/")
    String projectSourceDirRelativePath;

    @Option(names = {"-tests", "--compiled-tests"}, paramLabel = "<your compiled tests directory", description = "The directory containing compiled test classes ", defaultValue = "tests")
    File testsDir;

    @Option(names = {"-jars", "--test-dependencies-jars"}, paramLabel = "<jars dependencies directories needed for tests run>", description = "A list of directories containing Jars file dependency for running tests ", defaultValue = "test-jars")
    File[] testsDependencyDir;

    @Option(names = {"-maven", "--maven-home"}, paramLabel = "<maven home directory>", description = "The Maven home directory", defaultValue = "/usr/share/maven")
    File mavenHomeDirectory;

    @Parameters(paramLabel = "Tests", description = "one ore more test class to run")
    String[] testFiles;


    @Override
    public Integer call() throws Exception {
        final Invoker mavenInvoker = new DefaultInvoker();
        mavenInvoker.setMavenHome(mavenHomeDirectory);

        //check precondition
        if (!projectDir.exists()) {
            System.out.println("ERROR: The <maven-project> directory does not exist : " + projectDir.getAbsolutePath());
            return 1;
        }
        if (projectModuleDirRelativePath != null) {
            if (!new File(projectDir + "/" + projectModuleDirRelativePath).exists()) {
                System.out.println("ERROR: The <maven-module> does not exist in the project directory : " + new File(projectDir + "/" + projectModuleDirRelativePath).getAbsolutePath());
                return 1;
            }
        }
        if (!testsDir.exists()) {
            System.out.println("ERROR: The <tests> which should contain compiled test classes dose not exist : " + testsDir.getAbsolutePath());
            return 1;
        }
        if (!mavenHomeDirectory.exists()) {
            System.out.println("ERROR: <maven-home> directory need for compilation does not exist : " + mavenHomeDirectory.getAbsolutePath());
            return 1;
        }
        if (testFiles == null || testFiles.length == 0) {
            System.out.println("WARN: you do not specify any test class name. we run any test will be found");
            Collection<File> allTestFiles = listFiles(testsDir, new String[]{"class"}, true);
            List<String > foundTestFiles=new ArrayList<>();
            for (File testFile : allTestFiles) {
                if(!testFile.getName().endsWith(".class"))
                    continue;
                System.out.println("test class file found: " + testFile);
                String testClassName;
                if(File.separator.equals("\\"))
                    testClassName=testFile.getPath().replaceAll("\\\\",".");
                else
                    testClassName=testFile.getPath().replaceAll("/",".");

                //remove dir path from the begging of test class name
                testClassName=testClassName.substring(testsDir.getPath().length());
                if(testClassName.startsWith("."))
                    testClassName=testClassName.substring(1);
                //remove .class from the end of file name
                testClassName=testClassName.substring(0,testClassName.length()-".class".length());
                foundTestFiles.add(testClassName);
            }
            if(foundTestFiles.size()==0){
                System.out.println("ERROR: There is not test class file in "+testsDir.getPath());
                return 1;
            }
            testFiles=foundTestFiles.toArray(new String[0]);
        }

        //warning missing optional parameters
        if (testsDependencyDir.length==0) {
            for(File dependencyDir:testsDependencyDir)
                if(!dependencyDir.exists())
                    System.out.println("WARN: <test-dependencies> directory which should contains JARs file needed executing test dose not exist: " + dependencyDir.getAbsolutePath());
        }


        //load single file mutant
        List<Mutant> mutants = new ArrayList<>();
        Collection<File> allMutants = listFiles(mutantDir, new String[]{"java"}, false);
        for (File f : allMutants) {
            System.out.println("Single-file mutant found: " + f);
            Mutant m = new Mutant(f);
            mutants.add(m);
        }
        //load multiple-files mutants which are directories
        File[] allMultipleMutants= mutantDir.listFiles(File::isDirectory);
        for (File f : allMultipleMutants) {
            System.out.println("Multiple-files mutant found: " + f);
            Mutant m = new Mutant(f);
            Collection<File> mutantsFiles = listFiles(m.getFile(), new String[]{"java"}, false);
            for(File mutantFile:mutantsFiles)
                    m.files.add(mutantFile);
            mutants.add(m);
        }


        //check any mutants exists?
        if (mutants.size() == 0) {
            System.out.println("Error: There are not mutants in mutant folder at: " + mutantDir.getPath());
            return 1;
        }

        //foreach mutant we replace mutant in src file, compile and run the tests
        for (Mutant mutant : mutants) {

            //clean temp folder
            if (!TEMP_DIRECTORY.exists())
                TEMP_DIRECTORY.mkdir();
            FileUtils.cleanDirectory(TEMP_DIRECTORY);

            //copy source files to temp folder
            FileUtils.copyDirectory(projectDir, TEMP_DIRECTORY);

            //set maven-src-directory
            File mavenProjectTempDir;
            if (projectModuleDirRelativePath == null) {
                mavenProjectTempDir = TEMP_DIRECTORY;
            } else {
                // if the target is one of the module of the project
                // we must get path of the maven module
                mavenProjectTempDir = new File(TEMP_DIRECTORY + "/" + projectModuleDirRelativePath);

            }

            //replace mutant in temp-dir with the original source file
            if(!mutant.getFile().isDirectory()) {
                FileUtils.copyFile(mutant.getFile(), new File(mavenProjectTempDir + projectSourceDirRelativePath + mutant.getPathAndFileName()));
            }
            else {
                //mutant is a directory containing multiple java files
                for(File mutantFile: mutant.files){
                    if(!mutantFile.getName().toLowerCase().endsWith(".java"))
                        continue;
                    String filenameWithoutExtension=mutantFile.getName().substring(0,mutantFile.getName().lastIndexOf('.'));
                    String pathAndFilename=filenameWithoutExtension.replaceAll("\\.","/")+".java";
                    FileUtils.copyFile(mutantFile, new File(mavenProjectTempDir + projectSourceDirRelativePath + pathAndFilename));
                }
            }


            //compile project using maven
            System.out.println("try to compile project with a mutant: [" + mutant.getSimpleName() + "] by Maven");
            InvocationRequest request = new DefaultInvocationRequest();
            request.setBaseDirectory(mavenProjectTempDir);
            request.setGoals(Collections.singletonList("compile"));

            InvocationResult result = null;
            try {
                result = mavenInvoker.execute(request);
            } catch (MavenInvocationException e) {
                e.printStackTrace();
                System.out.println("error through maven compilation");
                mutant.setStatus("compile-error");
                continue;
            }
            if (result.getExitCode() != 0) {
                System.out.println("error through maven compilation");
                mutant.setStatus("compile-error");
                continue;
            }
            System.out.println("Maven Compilation is successful");

            //run tests for this compilation
            try {
                // Prepare URLs for classloader or classpath
                URL classesUrl = new File(mavenProjectTempDir + "/target/classes").toURI().toURL();
                URL compiledTestClassesUrl = testsDir.toURI().toURL();
                List<URL> urlList = new ArrayList<URL>();
                urlList.add(classesUrl);
                urlList.add(compiledTestClassesUrl);
                //also add all jars in test-jars directory if exist
                for(File dependencyDir:testsDependencyDir)
                if (dependencyDir.exists()) {
                    Collection<File> jars = listFiles(dependencyDir, new String[]{"jar"}, true);
                    for (File jar : jars)
                        urlList.add(jar.toURI().toURL());
                }

                //run junit as external process
                {
                    final List<String> actualArgs = new ArrayList<String>();
                    actualArgs.add("java");
                    actualArgs.add("-cp");
                    actualArgs.add(toClassPathString(urlList));
                    //System.out.println("class-path: "+urlList.toString());
                    actualArgs.add("org.junit.runner.JUnitCore");
                    for (String testClassName : testFiles) {
                        actualArgs.add(testClassName);

                    }

                    {
                        final Runtime re = Runtime.getRuntime();
                        final Process command = re.exec(actualArgs.toArray(new String[0]));
                        BufferedReader output = new BufferedReader(new InputStreamReader(command.getInputStream()));
                        BufferedReader errorOutput = new BufferedReader(new InputStreamReader(command.getErrorStream()));
                        //boolean commandResult = command.waitFor(1, TimeUnit.MINUTES);

                        String s = null;
                        String junitResultLine = "";
                        while ((s = output.readLine()) != null) {
                            System.out.println(s);
                            if (s.startsWith("OK (") || s.startsWith("Tests run:")) {
                                junitResultLine = s;
                            }
                        }
                        while ((s = errorOutput.readLine()) != null) {
                            System.out.println(s);
                        }
                        //check the last lin
                        if (junitResultLine.startsWith("OK")) {
                            //s would be like to => OK (120 runs)
                            String numberOnly = junitResultLine.replaceAll("[^0-9]", "");
                            mutant.setNumberOfTestRun(Integer.valueOf(numberOnly));
                            mutant.setNumberOfTestFails(0);
                            mutant.setStatus("live");
                        } else if (junitResultLine.startsWith("Tests run:")) {
                            //s would be like to => Tests run: 11,  Failures: 2
                            String[] runAndFailures = junitResultLine.split(",");
                            mutant.setNumberOfTestRun(Integer.valueOf(runAndFailures[0].replaceAll("[^0-9]", "")));
                            mutant.setNumberOfTestFails(Integer.valueOf(runAndFailures[1].replaceAll("[^0-9]", "")));
                            mutant.setStatus("killed");
                        }
                    }

//                    if (!commandResult) {
//                        //timeout - kill the process.
//                        command.destroy(); // consider using destroyForcibly instead
//                        mutant.setStatus("junit-timeout");
//                        continue;
//                    }
                }



/*                //running using classloader and junit programmatically
                {
                    //ClassLoader classLoader = new URLClassLoader(urlList.toArray(new URL[0]),ClassLoader.getSystemClassLoader());
                    for (URL url : urlList)
                        addUrlToCurrentClassloader(url);

                    ClassLoader classLoader = ClassLoader.getSystemClassLoader();

                    mutant.setStatus("live");
                    mutant.setNumberOfTestRun(0);
                    mutant.setNumberOfTestFails(0);

                    for (File testClassFile : testFiles) {
                        Class testClass = classLoader.loadClass(testClassFile.getName());
                        JUnitCore junit = new JUnitCore();
                        junit.addListener(new TextListener(System.out));
                        Result testResult = junit.run(testClass);
                        mutant.incNumberOfTestRun(testResult.getRunCount());
                        if (!testResult.wasSuccessful()) {
                            mutant.setStatus("killed");
                            mutant.incNumberOfTestFails(testResult.getFailureCount());
                        }
                    }
                }*/

            } catch (MalformedURLException e) {
                e.printStackTrace();
                System.out.println("error in running test");
                return -1;
            }
            /*catch (ClassNotFoundException e) {
                e.printStackTrace();
                System.out.println("error in running test");
                return -1;
            }*/
        }


        //show data
        String[][] mutantsData = new String[mutants.size()][];
        for (
                int i = 0; i < mutants.size(); i++) {
            Mutant m = mutants.get(i);
            mutantsData[i] = new String[5];
            mutantsData[i][0] = m.getSimpleName();
            mutantsData[i][1] = m.getType();
            mutantsData[i][2] = m.getStatus();
            mutantsData[i][3] = m.getNumberOfTestRun() + "";
            mutantsData[i][4] = m.getNumberOfTestFails() + "";
        }

        //print final result
        TextTable textTable = new TextTable(new String[]{"Mutant", "Mutant Type", "Status ", " #Tests ", " #Fails "}, mutantsData);
        textTable.setSort(0);
        textTable.setAddRowNumbering(true);
        textTable.printTable();
        System.out.println("");
        long numberOfKilledMutants=mutants.stream().filter(m->m.getStatus().equals("killed")).count();
        System.out.println(String.format("Overall Result: %d of %d (%.2f percent) mutants killed",numberOfKilledMutants, mutants.size(),100f*numberOfKilledMutants/mutants.size()));

        //save final result to file
        try (
                PrintStream out = new PrintStream(new FileOutputStream("mutants-result.txt"))) {
            textTable.printTable(out, 0);
            out.println("");
            out.println(String.format("Overall Result: %d of %d (%.2f percent) mutants killed",numberOfKilledMutants, mutants.size(),100f*numberOfKilledMutants/mutants.size()));
        }

        //save final result to csv-file
        try (
                PrintStream out = new PrintStream(new FileOutputStream("mutants-result.csv"))) {
            textTable.toCsv(out);
        }

        //clean and delete temp folder
        FileUtils.deleteDirectory(TEMP_DIRECTORY);
        return 0;
    }


    private String toClassPathString(List<URL> urlList) {
        if (urlList.size() == 0)
            return "";
        String result = urlList.get(0).getPath();
        for (int i = 1; i < urlList.size(); i++) {
            result += ":" + urlList.get(i).getPath();
        }
        return result;
    }

    private void addUrlToCurrentClassloader(URL url) throws Exception {
        Method method = URLClassLoader.class.getDeclaredMethod("addURL", new Class[]{URL.class});
        method.setAccessible(true);
        method.invoke(ClassLoader.getSystemClassLoader(), new Object[]{url});
    }
}
