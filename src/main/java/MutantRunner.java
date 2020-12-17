import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.*;

import static org.apache.commons.io.FileUtils.listFiles;

public class MutantRunner {
    public static String mutantDir="mutants";
    static List<Mutant> mutants=new ArrayList<Mutant>();

    public static void main(String[] args) {

        Collection<File> allMutants = listFiles(new File(mutantDir), new String[]{"java"},true);
        for (File f:allMutants){
            System.out.println(f);
            Mutant m=new Mutant(f);
            mutants.add(m);
        }


    }
}
