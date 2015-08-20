import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TfIdfMatrix {

    public final double[][] matrix;
    public final List<String> words;

    public TfIdfMatrix(double[][] matrix, List<String> words) {
        this.matrix = matrix;
        this.words = words;
    }

    public int indexOf(String word) {
        return words.indexOf(word);
    }

    public String wordAt(int i) {
        return words.get(i);
    }
}
