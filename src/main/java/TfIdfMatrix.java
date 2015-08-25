import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TfIdfMatrix {

    public final double[][] matrix;
    public final List<String> words;
    public final Map<String,Integer> docIndexByFilename;
    public final Map<Integer,String> docFilenameByIndex;

    public TfIdfMatrix(double[][] matrix, List<String> words, Map<String,Integer> docIndex) {

        this.matrix = matrix;
        this.words = words;
        this.docIndexByFilename = docIndex;

        /* Copy inversion of docIndexByFilename to docFilenameByIndex.
           Values of docIndexByFilename are distinct. */
        this.docFilenameByIndex = new HashMap<>();
        ///////////////////////REMOVE
        if (this.docIndexByFilename != null)
        this.docIndexByFilename.entrySet().forEach(entry ->
                this.docFilenameByIndex.put(entry.getValue(),entry.getKey()));

    }

    /* Given a word, get index */
    public int indexOf(String word) {
        return words.indexOf(word);
    }

    /* Given an index, get word */
    public String wordAt(int i) {
        return words.get(i);
    }

    /* Given a filename, return index */
    public int getDocIndex(String filename) { return this.docIndexByFilename.get(filename); }

    /* Given an index, return filename */
    public String getDocAt(int index) { return this.docFilenameByIndex.get(index); }
}
