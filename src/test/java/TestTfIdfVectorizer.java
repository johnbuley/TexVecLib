import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class TestTfIdfVectorizer {

    @Test
    public void testFolderOfTextFilesIsLoaded() {
        List<String> expectedResult = Arrays.asList(
                                        "The brown cow sits in the grass.",
                                        "The blue cow sits in the grass.",
                                        "The red cow sits in the field.");
        TfIdfVectorizer vec = new TfIdfVectorizer();
        List<String> producedResult = vec.processInputFolder("./src/test/resources/testInputFolder/");
        assertEquals(expectedResult,producedResult);
    }
}