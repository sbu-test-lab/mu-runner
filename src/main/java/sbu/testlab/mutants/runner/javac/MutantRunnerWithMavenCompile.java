package sbu.testlab.mutants.runner.javac;

import dnl.utils.text.table.TextTable;
import org.apache.commons.io.FileUtils;
import org.apache.maven.shared.invoker.*;
import org.junit.internal.TextListener;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import sbu.testlab.mutants.runner.maven.Mutant;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

import static org.apache.commons.io.FileUtils.listFiles;

public class MutantRunnerWithMavenCompile {
    static File mutantDir = new File("mutants");
    static File tempProjectDir = new File("temp-project");
    static File projectDir = new File("project-dir");
    static File projectTestClassesDir = new File("test-classes");
    static File jarsDependencyDir = new File("jars");

    // define a field for the Invoker instance.
    static final Invoker mavenInvoker=new DefaultInvoker();

    static List<Mutant> mutants = new ArrayList<Mutant>();
    static List<File> testClasses = new ArrayList<File>();
    static Collection<File> jarsDependency = new ArrayList<File>();

    public static void main(String[] args) throws IOException {
//        String mavenHomeDir=System.getenv().get("MAVEN_HOME");
//        if(mavenHomeDir==null || "".equals(mavenHomeDir)){
//            System.out.println("Could not find Maven tool. You must specify it in the environment variable MAVEN_HOME");
//            return;
//        }

        mavenInvoker.setMavenHome(new File("/usr/share/maven"));

        if (args.length < 2) {
            System.out.println("you must specify one or more test class name with option -tests <test1> <test2>");
            return;
        }

        //extract test classes from input args
        testClasses = getTestFromArgs(args);

        //load dependency jars
        jarsDependency=FileUtils.listFiles(jarsDependencyDir,new String[]{"jar"}, true );

        //load mutants file
        Collection<File> allMutants = listFiles(mutantDir, new String[]{"java"}, true);
        for (File f : allMutants) {
            System.out.println(f);
            Mutant m = new Mutant(f);
            mutants.add(m);
        }

        //foreach mutant we replace mutant in src file, compile and run the tests
        for (Mutant mutant : mutants) {

            //clean temp folder
            if (!tempProjectDir.exists())
                tempProjectDir.mkdir();
            FileUtils.cleanDirectory(tempProjectDir);

            //copy source files to temp folder
            FileUtils.copyDirectory(projectDir, tempProjectDir);

            //replacement of mutant in temp-dir directory and since it is a maven project in src's folder of temp-dir
            FileUtils.copyFile(mutant.getFile(), new File(tempProjectDir + "/src/main/java/" + mutant.getPathAndFileName()));


            //compile project using maven
            System.out.println("try to compile project with a mutant: "+mutant.getSimpleName()+" by Maven");
            InvocationRequest request = new DefaultInvocationRequest();
            request.setBaseDirectory( tempProjectDir );
            request.setGoals(Collections.singletonList("compile"));

            InvocationResult result = null;
            try {
                result = mavenInvoker.execute( request );
            } catch (MavenInvocationException e) {
                e.printStackTrace();
                System.out.println("error through maven compilation");
                mutant.setStatus("compile-error");
                continue;
            }

            if ( result.getExitCode() != 0 )
            {
                System.out.println("error through maven compilation");
                mutant.setStatus("compile-error");
                continue;
            }
            System.out.println("Maven Compilation is successful");

            //load test class for running test

            try {
                // Prepare URLs
                URL classesUrl = new File(tempProjectDir + "/target/classes").toURI().toURL();
                URL projectTestClassesUrl = projectTestClassesDir.toURI().toURL();
                List<URL> urlList=new ArrayList<URL>();
                urlList.add(classesUrl);
                urlList.add(projectTestClassesUrl);
                for(File jar:jarsDependency)
                    urlList.add(jar.toURI().toURL());

                // Create a new class loader with the directory
                ClassLoader classLoader = new URLClassLoader(urlList.toArray(new URL[0]));

                mutant.setStatus("live");
                mutant.setNumberOfTestRun(0);
                mutant.setNumberOfTestFails(0);
                for(File testClassFile:testClasses){
                    Class testClass = classLoader.loadClass(testClassFile.getName());
                    JUnitCore junit = new JUnitCore();
                    junit.addListener(new TextListener(System.out));
                    Result testResult = junit.run(testClass);
                    mutant.incNumberOfTestRun(testResult.getRunCount());
                    if(!testResult.wasSuccessful()){
                        mutant.setStatus("killed");
                        mutant.incNumberOfTestFails(testResult.getFailureCount());
                    }
                }

            } catch (MalformedURLException e) {
                e.printStackTrace();
                System.out.println("error in running test");
                return;
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                System.out.println("error in running test");
                return;
            }
        }

        //show data
        String[][] mutantsData = new String[mutants.size()][];
        for (int i = 0; i < mutants.size(); i++) {
            Mutant m = mutants.get(i);
            mutantsData[i] = new String[4];
            mutantsData[i][0] = m.getSimpleName();
            mutantsData[i][1] = m.getStatus();
            mutantsData[i][2] = m.getNumberOfTestRun()+"";
            mutantsData[i][3] = m.getNumberOfTestFails()+"";
        }

        //print final result
        TextTable textTable = new TextTable(new String[]{"Mutant", "Status ", " #Tests ", " #Fails "}, mutantsData);
        textTable.setSort(0);
        textTable.setAddRowNumbering(true);
        textTable.printTable();

        //save final result to file
        try (PrintStream out = new PrintStream(new FileOutputStream("mutants-result.txt"))) {
            textTable.printTable(out,0);
        }

        //save final result to csv-file
        try (PrintStream out = new PrintStream(new FileOutputStream("mutants-result.csv"))) {
            textTable.toCsv(out);
        }

        //clean and delete temp folder
        FileUtils.deleteDirectory(tempProjectDir);


    }

    private static List<File> getTestFromArgs(String[] args) {
        List<File> result = new ArrayList<File>();
        int testIndex = 0;
        for (; testIndex < args.length; testIndex++) {
            if (args[testIndex].startsWith("-tests"))
                break;
        }
        for (int i = testIndex + 1; i < args.length; i++) {
            String testClass = args[i];
            if(testClass.startsWith("-")){
                //end of -test args
                return result;
            }
            if (testClass.endsWith(".class")) {
                //remove .class from name of class
                testClass = testClass.replace(".class","");
            }
            //testClass = testClass.replaceAll("\\.", "/");
            result.add(new File(testClass));
        }
        return result;
    }

}
