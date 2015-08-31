import org.junit.Test;

import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;

public class TestDocumentSet {

    @Test
    public void test_sparsifyDoc() {

        TfIdfVectorizer vec = new TfIdfVectorizer();

        int[] expectedCounts = { 2, 1, 1, 1, 1, 2 };

        DocumentSet docSet = new DocumentSet();

        docSet.addFile("docA.txt", "./src/test/resources/testInputFolder/docA.txt");

        SparseDoc doc = docSet.getDoc(0);

        int i = 0;
        for(TokenArrayElement token : doc) {
            assertEquals(i,token.tokenId);
            assertEquals(expectedCounts[i],token.tokenCount);
            i++;
        }
    }
}
