import java.util.Map;

public class SparseDoc {

    public final String filename;
    public final Map<String,Integer> docHash;

    public SparseDoc(String filename, Map<String,Integer> docHash) {

        this.filename = filename;
        this.docHash = docHash;

    }

}
