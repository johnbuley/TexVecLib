import java.util.HashMap;
import java.util.Map;

/* Returned as field of TfIdfMatrix, providing an index to the matrix field. */
public class DocumentIndex {

    private Map<String,Integer> docIndexByFilename;
    private Map<Integer,String> docFilenameByIndex;

    public DocumentIndex(Map<String,Integer> docIndex) {

        this.docIndexByFilename = docIndex;

        /* Copy inversion of docIndexByFilename to docFilenameByIndex.
           Values of docIndexByFilename are distinct. */
        this.docFilenameByIndex = new HashMap<>();
        this.docIndexByFilename.entrySet().forEach(entry ->
                                                    this.docFilenameByIndex.put(entry.getValue(),entry.getKey()));

    }

    /* Given a filename, return index */
    public int get(String filename) {

        return this.docIndexByFilename.get(filename);

    }

    /* Given an index, return filename */
    public String get(int index) {

        return this.docFilenameByIndex.get(index);

    }

}
