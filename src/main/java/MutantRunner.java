import com.google.common.net.UrlEscapers;
import dnl.utils.text.table.TextTable;
import org.apache.commons.io.FileUtils;
import org.junit.internal.TextListener;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

import static org.apache.commons.io.FileUtils.listFiles;

public class MutantRunner {
    public static File mutantDir = new File("mutants");
    public static File tempSrcDir = new File("temp-src");
    public static File tempClassesDir = new File("temp-classes");
    private static File projectSrcDir = new File("project-src");
    private static File projectTestClassesDir = new File("project-test-classes");
    private static File jarsDependencyDir = new File("jars");

    static List<Mutant> mutants = new ArrayList<Mutant>();
    static List<File> testClasses = new ArrayList<File>();
    static Collection<File> jarsDependency = new ArrayList<File>();

    public static void main(String[] args) throws IOException {

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
            if (!tempSrcDir.exists())
                tempSrcDir.mkdir();
            if (!tempClassesDir.exists())
                tempClassesDir.mkdir();
            FileUtils.cleanDirectory(tempSrcDir);
            FileUtils.cleanDirectory(tempClassesDir);

            //copy source files to temp folder
            FileUtils.copyDirectory(projectSrcDir, tempSrcDir);

            //replacement of mutant in temp-src directory
            FileUtils.copyFile(mutant.getFile(), new File(tempSrcDir + "/" + mutant.getPathAndFileName()));


            //compile temp-src folder to generate temp-classes
            List<String> javacArgs = new ArrayList<String>();
            javacArgs.add("-d");
            javacArgs.add(tempClassesDir.getAbsolutePath());

            //set java class path
            /*args.add("-cp");
            args.add(classpath);*/

            //load src file in temp-src for compilation
            Collection<File> allSrc = listFiles(tempSrcDir, new String[]{"java"}, true);

            for (File srcFile : allSrc) {
                javacArgs.add(srcFile.getAbsolutePath());
            }
            int JavacExitCode = ToolProvider.getSystemJavaCompiler().run(System.in, System.out, System.err,
                    javacArgs.toArray(new String[]{}));
            if (JavacExitCode != 0) {
                System.out.println("Unable to compile the given source code. See System.err for details.");
                mutant.setStatus("compile-error");
                continue;
            }
            System.out.println("compilation is successfully for mutant: " + mutant.getSimpleName());

            //load test class for running test

            try {
                // Prepare URLs
                URL classesUrl = tempClassesDir.toURI().toURL();
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

        TextTable textTable = new TextTable(new String[]{"Mutant", "Status ", " #Tests ", " #Fails "}, mutantsData);
        textTable.printTable();

        //clean and delete temp folder
        //clean temp folder
        FileUtils.deleteDirectory(tempSrcDir);
        FileUtils.deleteDirectory(tempClassesDir);

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
            if (testClass.endsWith(".class"))
                testClass = testClass + ".class";
            //testClass = testClass.replaceAll("\\.", "/");
            result.add(new File(testClass));
        }
        return result;
    }
}
