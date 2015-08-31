import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TfIdfMatrix {

    public final double[][] matrix;
    private final Map<String,Integer> wordIndexByWord;
    private final Map<Integer,String> wordByWordIndex;
    private final Map<String,Integer> docIndexByFilename;
    private final Map<Integer,String> docFilenameByIndex;
    private final int numDocs;

    public TfIdfMatrix(double[][] matrix, Map<String,Integer> words, Map<String,Integer> docIndex) {

        this.matrix = matrix;
        this.wordIndexByWord = words;
        this.docIndexByFilename = docIndex;
        this.numDocs = docIndex.size();

        /* Copy inversion of docIndexByFilename to docFilenameByIndex.
           Values of docIndexByFilename are distinct. */
        this.docFilenameByIndex = new HashMap<>();
        this.docIndexByFilename.entrySet().forEach(entry ->
                this.docFilenameByIndex.put(entry.getValue(),entry.getKey()));

        /* Copy inversion of docIndexByFilename to docFilenameByIndex.
           Values of docIndexByFilename are distinct. */
        this.wordByWordIndex = new HashMap<>();
        this.wordIndexByWord.entrySet().forEach(entry ->
                this.wordByWordIndex.put(entry.getValue(),entry.getKey()));
    }

    /* Given a token, get index */
    public int indexOfToken(String token) {
        return wordIndexByWord.get(token);
    }

    /* Given an index, get word */
    public String tokenAt(int i) {
        return wordByWordIndex.get(i);
    }

    /* Given a filename, return index */
    public int getDocIndex(String filename) { return this.docIndexByFilename.get(filename); }

    /* Given an index, return filename */
    public String getDocAt(int index) { return this.docFilenameByIndex.get(index); }

    public int numDocs() { return this.numDocs; }
}
