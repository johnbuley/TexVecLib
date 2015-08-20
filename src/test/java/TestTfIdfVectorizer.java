import org.junit.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class TestTfIdfVectorizer {

    @Test
    public void testFolderOfTextFilesIsLoadedIntoList() {
        List<String> expectedResult = Arrays.asList(
                                        "The9 brown cow . sits   in the grass.",
                                        "The blue cow sits in the grass.",
                                        "The blue !!  cow sits in the field."
                                      );

        TfIdfVectorizer vec = new TfIdfVectorizer();

        Object[] testInputArray = {"./src/test/resources/testInputFolder/"};
        Class<?>[] classArray = {String.class};

        List<String> producedResult =
                (List<String>)callTfIdfPrivateMethod("processInputFolder", vec, testInputArray, classArray);

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
                                                Arrays.asList("the", "blue", "cow",
                                                        "sits","in","the","field"
                                                )
                                            );


        List<String> testInput = Arrays.asList(
                                    "The9 brown cow . sits   in the grass.",
                                    "The blue cow sits in the grass.",
                                    "The blue !! cow sits in the field."
                                 );

        TfIdfVectorizer vec = new TfIdfVectorizer();

        Object[] testInputArray = {testInput};
        Class<?>[] classArray = {List.class};

        List<List<String>> producedResult =
                (List<List<String>>)callTfIdfPrivateMethod("splitDocuments", vec, testInputArray, classArray);

        assertEquals(expectedResult,producedResult);
    }


    @Test
    public void TestGetNewIdfWordHash() {

        List<List<String>> testInput = Arrays.asList(
                Arrays.asList("the", "brown", "cow",
                        "sits","in","the","grass"
                ),
                Arrays.asList("the", "blue", "cow",
                        "sits","in","the","grass"
                ),
                Arrays.asList("the", "blue", "cow",
                        "sits","in","the","field"
                )
        );

        ConcurrentHashMap<String,Double> expectedIdfValuesHash = new ConcurrentHashMap<>();
        expectedIdfValuesHash.put("blue",Math.log(3./2));
        expectedIdfValuesHash.put("brown",Math.log(3./1));
        expectedIdfValuesHash.put("field",Math.log(3./1));
        expectedIdfValuesHash.put("grass",Math.log(3./2));

        TfIdfVectorizer vec = new TfIdfVectorizer();

        Object[] testInputArray = {testInput,1,(float).7};
        Class<?>[] classArray = {List.class,int.class,float.class};

        ConcurrentHashMap<String,idfWord> producedResult =
                (ConcurrentHashMap<String,idfWord>)callTfIdfPrivateMethod("getNewIdfWordHash", vec,
                                                                           testInputArray, classArray);

        /* Check that method constructed a hashmap with the correct keys */
        assertEquals(producedResult.keySet(),expectedIdfValuesHash.keySet());

        /* Check that the idf values match */
        expectedIdfValuesHash.keySet()
                .forEach(w ->
                        assertEquals(
                                (double)producedResult.get(w).idf,
                                (double)expectedIdfValuesHash.get(w), .001));

        /* Check that idfWord indices are a valid set for indexing a matrix */
        List<Integer> producedIndices =
                producedResult.keySet()
                    .stream()
                    .map(w -> producedResult.get(w).index)
                    .sorted()
                    .collect(Collectors.toList());

        for(int i = 0; i < producedIndices.size(); i++)
            assertEquals(i,(int)producedIndices.get(i));
    }

    public Object callTfIdfPrivateMethod(String methodName, TfIdfVectorizer vec, Object[] input, Class<?>[] inputType) {

        Object result = null;

        try {
            Method testMethod = TfIdfVectorizer.class.getDeclaredMethod(methodName, inputType);
            testMethod.setAccessible(true);
            result = testMethod.invoke(vec, input);
        }
        catch (Exception e) {
            System.out.println(e.toString());
        }

        return result;

    }

}