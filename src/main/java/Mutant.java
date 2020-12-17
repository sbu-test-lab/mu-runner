import java.io.File;

/**
 * a mutant is a java file with the following name
 *  <pre>package.name.className_type_number.java</pre>
 */
public class Mutant {
    File file;
    String type;
    String number;

    public Mutant(File file) {
        this.file = file;
        extractTypeAndNumber();
    }

    private void extractTypeAndNumber() {
        String name=file.getName();
        String nameWithoutExtension=name.substring(0,name.lastIndexOf('.'));
        this.number=nameWithoutExtension.substring(nameWithoutExtension.lastIndexOf('_')+1);
        String nameWithoutNumber=nameWithoutExtension.substring(0,name.lastIndexOf('_'));
        this.type=nameWithoutNumber.substring(nameWithoutNumber.lastIndexOf('_')+1);
    }

    public String getFileName(){
        return file.getName();
    }

    public String getSimpleName(){
        return this.type+'-'+this.number;
    }

    public File getFile() {
        return file;
    }

    public String getType() {
        return type;
    }

    public String getNumber() {
        return number;
    }

}
