import java.util.Map;

public class SparseDocHash {

    public final String filename;
    public final Map<String,Integer> docHash;

    public SparseDocHash(String filename, Map<String, Integer> docHash) {

        this.filename = filename;
        this.docHash = docHash;

    }

}
