import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    public void testGetNewIdfWordHash() {

        List<List<String>> testInput = getMockTokens();

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
        assertEquals(producedResult.keySet(), expectedIdfValuesHash.keySet());

        /* Check that the idf values match */
        expectedIdfValuesHash.keySet()
                .forEach(w ->
                        assertEquals(producedResult.get(w).idf, expectedIdfValuesHash.get(w), .001));

        /* Check that IdfWord indices are a valid set for indexing a matrix */
        List<Integer> producedIndices =
                producedResult.keySet().stream()
                                       .map(w -> producedResult.get(w).index)
                                       .sorted()
                                       .collect(Collectors.toList());

        for(int i = 0; i < producedIndices.size(); i++)
            assertEquals(i,(int)producedIndices.get(i));
    }



    @Test
    public void testGetTfIdfMatrix() {

        List<List<String>> testInput = Arrays.asList(
                Arrays.asList("the", "brown", "cow",
                        "sits", "in", "the", "grass"
                ),
                Arrays.asList("the", "blue", "cow",
                        "sits", "in", "the", "grass"
                ),
                Arrays.asList("the", "blue", "cow",
                        "sits", "in", "the", "blue", "field"
                )
        );

        ConcurrentHashMap<String, IdfWord> mockIdfHash = getMockIdfHash();

        TfIdfVectorizer vec = new TfIdfVectorizer();

        /* Test the target method */
        Object[] testInputArray = {testInput,mockIdfHash};
        Class<?>[] testClassArray = {List.class,ConcurrentHashMap.class};

        TfIdfMatrix producedTfIdfMatrixObj =
                (TfIdfMatrix)callTfIdfPrivateMethod("getTfIdfMatrix", vec,testInputArray, testClassArray);

        HashMap<String,double[]> expectedResults = new HashMap<>();

        double[] array1 = {1.0986,0,0};
        expectedResults.put("brown",array1);
        double[] array2 = {0,.4055,.8109};
        expectedResults.put("blue", array2);
        double[] array3 = {.4055,.4055,0};
        expectedResults.put("grass", array3);
        double[] array4 = {0,0,1.0986};
        expectedResults.put("field", array4);

        expectedResults.entrySet().forEach(
                e -> {
                    assertEquals(
                            producedTfIdfMatrixObj.matrix[0][producedTfIdfMatrixObj.indexOf(e.getKey())],
                            e.getValue()[0], .001);
                    assertEquals(
                            producedTfIdfMatrixObj.matrix[1][producedTfIdfMatrixObj.indexOf(e.getKey())],
                            e.getValue()[1], .001);
                    assertEquals(
                            producedTfIdfMatrixObj.matrix[2][producedTfIdfMatrixObj.indexOf(e.getKey())],
                            e.getValue()[2], .001);
                });
    }



    @Test
    public void TestGetPresentWordsList() {

        List<List<String>> testInput = getMockTokens();

        List<String> expectedResult = Arrays.asList("blue", "brown", "grass", "field");

        TfIdfVectorizer vec = new TfIdfVectorizer();

        ConcurrentHashMap<String,IdfWord> mockIdfHash = getMockIdfHash();

        Object[] testInputArray = {testInput,mockIdfHash};
        Class<?>[] testClassArray = {List.class,ConcurrentHashMap.class};

        List<String> producedResult = (List<String>)callTfIdfPrivateMethod("getPresentWordsList", vec,
                                                                           testInputArray, testClassArray);


        /* Assert that the results are of the same size, and that all elements
           of expectedResult (which are distinct) are contained by producedResult.
         */
        assertEquals(expectedResult.size(), producedResult.size());

        expectedResult.forEach(e -> assertTrue(producedResult.contains(e)));

    }



    @Test
    public void TestGetIndexOfPresentWordsList() {

        List<String> testInput = Arrays.asList("blue", "brown", "grass", "field");

        TfIdfVectorizer vec = new TfIdfVectorizer();

        Object[] testInputArray = { testInput };
        Class<?>[] testClassArray = {List.class};

        ConcurrentHashMap<String,Integer> producedResult =
                (ConcurrentHashMap<String,Integer>)callTfIdfPrivateMethod("getPresentWordsIndex", vec,
                                                                          testInputArray, testClassArray);

        /* Assert that the result is of the same size as the input, and that all
           elements of testInput (which are distinct) are contained by producedResult.
         */
        assertEquals(testInput.size(), producedResult.size());

        testInput.forEach(e -> assertTrue(producedResult.containsKey(e)));

        List<Integer> producedIndices =
                producedResult.entrySet().stream()
                                        .map(e -> e.getValue())
                                        .collect(Collectors.toList());

        producedIndices.sort((a, b) -> a.compareTo(b));

        for(int i : producedIndices) {
            assertEquals((int)producedIndices.get(i),i++);
        }
    }



    @Test
    public void TestGetTfIdfEntries() {

        List<List<String>> testListOfDocuments = getMockTokens();
        ConcurrentHashMap<String,IdfWord> testIdfHash = getMockIdfHash();

        Map<String,Double> expectedResult = new HashMap<>();
        expectedResult.put("brown",1.0986);
        expectedResult.put("grass",.4055);

        TfIdfVectorizer vec = new TfIdfVectorizer();

        Object[] testInputArray = {testListOfDocuments.get(0),testIdfHash};
        Class<?>[] testClassArray = {List.class,ConcurrentHashMap.class};

        Map<String,Double> producedResult =
                (Map<String,Double>)callTfIdfPrivateMethod("getTfIdfEntries", vec,testInputArray, testClassArray);

        /* Check that the size of the results and their value sets match */
        assertEquals(producedResult.size(), expectedResult.size());
        expectedResult.entrySet().forEach(e -> assertEquals((double) e.getValue(),
                                                            producedResult.get(e.getKey()),
                                                            .001));

    }

    @Test
    public void TestWriteTdIdfEntriesToMatrix() {

        TfIdfVectorizer vec = new TfIdfVectorizer();

        double[][] testMatrix = { {0, 0, 0, 0},
                                  {0, 0, 0, 0} };
        double[][] expectedMatrix = { {0, 1., 0, 2.},
                                      {0, 0, 0, 0} };

        Map<String,Double> testTfIdfEntries = new HashMap<>();
        testTfIdfEntries.put("blue",1.);
        testTfIdfEntries.put("brown",2.);

        ConcurrentHashMap<String,Integer> testPresentWordsIndex = new ConcurrentHashMap<>();
        testPresentWordsIndex.put("cow",0);
        testPresentWordsIndex.put("blue",1);
        testPresentWordsIndex.put("brown",3);

        Object[] testInputArray = {testTfIdfEntries,testPresentWordsIndex,0,testMatrix};
        Class<?>[] testClassArray = {Map.class,ConcurrentHashMap.class, int.class, testMatrix.getClass()};

        callTfIdfPrivateMethod("writeTfIdfEntriesToMatrix", vec,testInputArray, testClassArray);

        for(int i = 0; i < 2; i++) {
            for (int j = 0; j < 4; j++) {
                assertEquals(testMatrix[i][j],expectedMatrix[i][j],.001);
            }
        }
    }

    public List<List<String>> getMockTokens() {

        return Arrays.asList(
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

    }

    public ConcurrentHashMap<String,IdfWord> getMockIdfHash() {

        List<List<String>> testInput = getMockTokens();

        TfIdfVectorizer vec = new TfIdfVectorizer();

        Object[] testInputArray = {testInput,1,(float).7};
        Class<?>[] classArray = {List.class,int.class,float.class};

        return (ConcurrentHashMap<String, IdfWord>)callTfIdfPrivateMethod("getNewIdfWordHash", vec,
                                                                          testInputArray, classArray);

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