import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
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

        ConcurrentHashMap<String, IdfWord> producedResult =
                (ConcurrentHashMap<String, IdfWord>)callTfIdfPrivateMethod("getNewIdfWordHash", vec,
                                                                           testInputArray, classArray);

        /* Check that method constructed a hashmap with the correct keys */
        assertEquals(producedResult.keySet(),expectedIdfValuesHash.keySet());

        /* Check that the idf values match */
        expectedIdfValuesHash.keySet()
                .forEach(w ->
                        assertEquals(producedResult.get(w).idf,expectedIdfValuesHash.get(w), .001));

        /* Check that IdfWord indices are a valid set for indexing a matrix */
        List<Integer> producedIndices =
                producedResult.keySet()
                    .stream()
                    .map(w -> producedResult.get(w).index)
                    .sorted()
                    .collect(Collectors.toList());

        for(int i = 0; i < producedIndices.size(); i++)
            assertEquals(i,(int)producedIndices.get(i));
    }


    @Test
    public void TestGetTfIdfMatrix() {

        List<List<String>> testInput = Arrays.asList(
                Arrays.asList("the", "brown", "cow",
                        "sits","in","the","grass"
                ),
                Arrays.asList("the", "blue", "cow",
                        "sits","in","the","grass"
                ),
                Arrays.asList("the", "blue", "cow",
                        "sits","in","the","blue","field"
                )
        );

        TfIdfVectorizer vec = new TfIdfVectorizer();

        /* Get the IdfWordHash used to test the target method. */
        Object[] mockInputArray = {testInput,1,(float).7};
        Class<?>[] mockClassArray = {List.class,int.class,float.class};

        ConcurrentHashMap<String, IdfWord> mockIdfHash =
                (ConcurrentHashMap<String, IdfWord>)callTfIdfPrivateMethod("getNewIdfWordHash", vec,
                        mockInputArray, mockClassArray);

        /* Test the target method */
        Object[] testInputArray = {testInput,mockIdfHash};
        Class<?>[] testClassArray = {List.class,ConcurrentHashMap.class};

        TfIdfMatrix producedTfIdfMatrixObj =
                (TfIdfMatrix)callTfIdfPrivateMethod("getTfIdfMatrix", vec,testInputArray, testClassArray);

        HashMap<String,double[]> expectedResults = new HashMap<>();

        double[] array1 = {1.0986,0,0};
        expectedResults.put("brown",array1);
        double[] array2 = {0,.4055,.8109};
        expectedResults.put("blue",array2);
        double[] array3 = {.4055,.4055,0};
        expectedResults.put("grass",array3);
        double[] array4 = {0,0,1.0986};
        expectedResults.put("field", array4);

        expectedResults.entrySet().forEach(
                e -> {
                    assertEquals(
                            producedTfIdfMatrixObj.matrix[0][producedTfIdfMatrixObj.indexOf(e.getKey())],
                            e.getValue()[0],.001);
                    assertEquals(
                            producedTfIdfMatrixObj.matrix[1][producedTfIdfMatrixObj.indexOf(e.getKey())],
                            e.getValue()[1],.001);
                    assertEquals(
                            producedTfIdfMatrixObj.matrix[2][producedTfIdfMatrixObj.indexOf(e.getKey())],
                            e.getValue()[2],.001);
                });
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