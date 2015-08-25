import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TfIdfMatrix {

    public final double[][] matrix;
    public final Map<String,Integer> wordIndexByWord;
    public final Map<Integer,String> wordByWordIndex;
    public final Map<String,Integer> docIndexByFilename;
    public final Map<Integer,String> docFilenameByIndex;

    public TfIdfMatrix(double[][] matrix, Map<String,Integer> words, Map<String,Integer> docIndex) {

        this.matrix = matrix;
        this.wordIndexByWord = words;
        this.docIndexByFilename = docIndex;

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

    /* Given a word, get index */
    public int indexOf(String word) {
        return wordIndexByWord.get(word);
    }

    /* Given an index, get word */
    public String wordAt(int i) {
        return wordByWordIndex.get(i);
    }

    /* Given a filename, return index */
    public int getDocIndex(String filename) { return this.docIndexByFilename.get(filename); }

    /* Given an index, return filename */
    public String getDocAt(int index) { return this.docFilenameByIndex.get(index); }
}
