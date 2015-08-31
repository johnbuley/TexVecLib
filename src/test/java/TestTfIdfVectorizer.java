import Jama.Matrix;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class TestTfIdfVectorizer {

    @Test
    public void test_fitTransform() {

        TfIdfVectorizer vec = new TfIdfVectorizer();

        TfIdfMatrix result = vec.fitTransform("./src/test/resources/twainhomer/", 2, .8);

        Matrix matrix = new Matrix(result.matrix);

        Matrix similarity = matrix.times(matrix.transpose());

        assertEquals(similarity.get(result.getDocIndex("huckfinn.txt"), result.getDocIndex("sawyer.txt")),.4987,.01);
        assertEquals(similarity.get(result.getDocIndex("iliad.txt"), result.getDocIndex("odyssey.txt")), .2986, .01);

    }

    @Test
    public void test_getTfIdfMatrix() {

        DocumentSet docSet = Utilities.getMockDocs();
        Corpus testCorpus = docSet.getCorpus();

        testCorpus.filterValidTokens(1, .7);
        testCorpus.calcIdf();

        TfIdfVectorizer vec = new TfIdfVectorizer();

        Object[] testInputArray = { docSet, testCorpus };
        Class<?>[] testClassArray = {DocumentSet.class, Corpus.class};

        TfIdfMatrix producedResult =
                (TfIdfMatrix) Utilities.callTfIdfPrivateMethod("getTfIdfMatrix", vec,
                                                                testInputArray, testClassArray);

        HashMap<String,double[]> expectedResult = new HashMap<>();

        double[] array1 = {.8047,0,0};
        expectedResult.put("brown", array1);
        double[] array2 = {0,.7071,.5937};
        expectedResult.put("blue", array2);
        double[] array3 = {.5937,.7071,0};
        expectedResult.put("grass", array3);
        double[] array4 = {0,0,.8046};
        expectedResult.put("field", array4);

        /* Assert that tf-idf values are equal */
        expectedResult.entrySet().forEach(
                e -> {
                    assertEquals(
                            producedResult.matrix[producedResult.getDocIndex("docA.txt")]
                                                 [producedResult.indexOfToken(e.getKey())],
                            e.getValue()[0],
                            .001);
                    assertEquals(
                            producedResult.matrix[producedResult.getDocIndex("docB.txt")]
                                                 [producedResult.indexOfToken(e.getKey())],
                            e.getValue()[1],
                            .001);
                    assertEquals(
                            producedResult.matrix[producedResult.getDocIndex("docC.txt")]
                                                 [producedResult.indexOfToken(e.getKey())],
                            e.getValue()[2],
                            .001);
                });

    }

}