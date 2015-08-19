import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class TestTfIdfVectorizer {

    @Test
    public void testFolderOfTextFilesIsLoadedIntoList() {
        List<String> expectedResult = Arrays.asList(
                                        "The brown cow sits in the grass.",
                                        "The blue cow sits in the grass.",
                                        "The red cow sits in the field."
                                      );

        TfIdfVectorizer vec = new TfIdfVectorizer();

        List<String> producedResult = null;

        /* Use reflection to change access of processInputFolder */
        try {
            Method testMethod = TfIdfVectorizer.class.getDeclaredMethod("processInputFolder",String.class);
            testMethod.setAccessible(true);
            producedResult = (List<String>)testMethod.invoke(vec, "./src/test/resources/testInputFolder/");
        }
        catch (Exception e) {
            System.out.println(e.toString());
        }

        assertEquals(expectedResult,producedResult);
    }


    @Test
    public void testListOfDocumentsIsSplit() {
        List<List<String>> expectedResult = Arrays.asList(
                                                Arrays.asList("the", "brown", "cow",
                                                        "sits","in","the","grass"
                                                ),
                                                Arrays.asList("the", "blue", "cow",
                                                        "sits","in","the","grass"
                                                ),
                                                Arrays.asList("the", "red", "cow",
                                                        "sits","in","the","field"
                                                )
                                            );

        List<String> testInput = Arrays.asList(
                                    "The brown cow sits in the grass.",
                                    "The blue cow sits in the grass.",
                                    "The red cow sits in the field."
                                 );

        TfIdfVectorizer vec = new TfIdfVectorizer();
        List<List<String>> producedResult = null;

        try {
            Method testMethod = TfIdfVectorizer.class.getDeclaredMethod("splitDocuments", List.class);
            testMethod.setAccessible(true);
            producedResult = (List<List<String>>)testMethod.invoke(vec, testInput);
        }
        catch (Exception e) {
            System.out.println(e.toString());
        }

        assertEquals(expectedResult,producedResult);
    }

}