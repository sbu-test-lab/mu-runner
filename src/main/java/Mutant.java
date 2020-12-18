import java.io.File;

/**
 * a mutant is a java file with the following name
 *  <pre>package.name.className_type_number.java</pre>
 */
public class Mutant {
    File file;
    String type;
    String number;
    String pathAndFileName;
    private String filename;
    //killed, not-verify, live, compile-error
    private String status="not-verify";
    private int numberOfTestRun;
    private int numberOfTestFails;

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
        String onlyName=nameWithoutNumber.substring(0,nameWithoutNumber.lastIndexOf('_'));
        this.pathAndFileName=onlyName.replaceAll("\\.","/")+".java";
        if(this.pathAndFileName.contains("/"))
            this.filename=this.pathAndFileName.substring(this.pathAndFileName.lastIndexOf('/')+1);
        else
            this.filename=this.pathAndFileName;
    }

    public String getFileName(){
        return file.getName();
    }

    public String getSimpleName(){
        return this.filename+ " - "+this.type+" - "+this.number;
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

    public String getPathAndFileName() {
        return pathAndFileName;
    }

    public String getFilename() {
        return filename;
    }

    @Override
    public String toString() {
        return "Mutant{" +
                "file=" + file +
                ", type='" + type + '\'' +
                ", number='" + number + '\'' +
                ", pathAndFileName='" + pathAndFileName + '\'' +
                ", filename='" + filename + '\'' +
                '}';
    }

    public void setStatus(String s) {
        this.status=s;
    }

    public String getStatus() {
        return this.status;
    }

    public void setNumberOfTestRun(int i) {
        this.numberOfTestRun=i;
    }

    public void incNumberOfTestRun(int i) {
        this.numberOfTestRun=this.numberOfTestRun+i;
    }

    public int getNumberOfTestRun() {
        return this.numberOfTestRun;
    }

    public void setNumberOfTestFails(int i) {
        this.numberOfTestFails=i;
    }

    public void incNumberOfTestFails(int i) {
        this.numberOfTestFails=this.numberOfTestRun+i;
    }

    public int getNumberOfTestFails() {
        return this.numberOfTestFails;
    }
}
