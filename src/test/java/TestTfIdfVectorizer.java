import org.junit.Test;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class TestTfIdfVectorizer {

/* ---------------------------
   Tests
 * --------------------------- */

    @Test
    public void test_sparsifyDoc() {

        TfIdfVectorizer vec = new TfIdfVectorizer();

        Map<String,Integer> expectedDocHash = new HashMap<>();
        expectedDocHash.put("the",2); expectedDocHash.put("brown",1); expectedDocHash.put("cow",1);
        expectedDocHash.put("sits",1); expectedDocHash.put("in",1); expectedDocHash.put("grass",2);

        SparseDoc expectedResult = new SparseDoc("docA.txt",expectedDocHash);

        Object[] testInputArray = { Paths.get("./src/test/resources/testInputFolder/docA.txt") };
        Class<?>[] testClassArray = { Path.class };

        SparseDoc producedResult =
                (SparseDoc)callTfIdfPrivateMethod("sparsifyDoc", vec,testInputArray, testClassArray);

        assertEquals(producedResult.filename,expectedResult.filename);

        assertEquals(producedResult.docHash.size(),expectedResult.docHash.size());

        expectedResult.docHash.entrySet().forEach(entry ->
                assertEquals(entry.getValue(),
                        producedResult.docHash.get(entry.getKey())));

    }

    @Test
    public void test_processInputFolder() {

        TfIdfVectorizer vec = new TfIdfVectorizer();

        Set<String> expectedResult = new HashSet<>();
        expectedResult.add("docA.txt"); expectedResult.add("docB.txt"); expectedResult.add("docC.txt");

        Object[] testInputArray = { "./src/test/resources/testInputFolder/" };
        Class<?>[] testClassArray = { String.class };

        Map<String,SparseDoc> producedResult =
                (Map<String,SparseDoc>)callTfIdfPrivateMethod("processInputFolder", vec, testInputArray, testClassArray);

        assertEquals(expectedResult.size(),producedResult.size());

        expectedResult.forEach(entry -> {
            assertTrue(producedResult.containsKey((String) entry));
            assertTrue(producedResult.get((String) entry) instanceof SparseDoc);
        });

    }

    @Test
    public void test_addWordToDoc() {

        Map<String,Integer> testDoc = new HashMap<String,Integer>();
        byte[] testConcatBuf = new byte[8];

        testConcatBuf[0] = (char) 't'; testConcatBuf[1] = (char) 'h'; testConcatBuf[2] = (char) 'e';

        TfIdfVectorizer vec = new TfIdfVectorizer();

        /* Had trouble passing byte[].class as an argument type to *.class.getDeclaredMethod().
           So to test, I make the method public temporarily, otherwise leaving this commented. */

        //Object[] testInputArray = { testDoc, testConcatBuf, 3 };
        //Class<?>[] testClassArray = { testDoc.getClass(), byte[].class, int.class };

        //callTfIdfPrivateMethod("addWordToDoc", vec, testInputArray, testClassArray);

        //Map<String,SparseDoc> producedResult =
        //(Map<String,SparseDoc>)callTfIdfPrivateMethod("addWordToDoc", vec, testInputArray, testClassArray);

        //vec.addWordToDoc(testDoc,testConcatBuf,3);
        //vec.addWordToDoc(testDoc,testConcatBuf,3);

        //assertEquals((int)testDoc.get("the"),2);

    }

    @Test
    public void test_getNewIdfWordHash() {

        List<List<String>> testInput = getMockTokens();

        Map<String,SparseDoc> testDocs = getMockDocs();

        ConcurrentHashMap<String,Double> expectedIdfValuesHash = new ConcurrentHashMap<>();
        expectedIdfValuesHash.put("blue",Math.log(3./2)); expectedIdfValuesHash.put("brown",Math.log(3./1));
        expectedIdfValuesHash.put("field",Math.log(3./1)); expectedIdfValuesHash.put("grass",Math.log(3./2));

        TfIdfVectorizer vec = new TfIdfVectorizer();

        Object[] testInputArray = {testDocs,1,(float).7};
        Class<?>[] classArray = {Map.class,int.class,float.class};

        ConcurrentHashMap<String, Double> producedResult =
                (ConcurrentHashMap<String, Double>)callTfIdfPrivateMethod("getNewIdfWordHash", vec,
                        testInputArray, classArray);

        /* Check that method constructed a hashmap with the correct keys */
        assertEquals(producedResult.keySet(), expectedIdfValuesHash.keySet());

        /* Check that the idf values match */
        expectedIdfValuesHash.keySet()
                .forEach(w ->
                        assertEquals((double) producedResult.get(w), expectedIdfValuesHash.get(w), .001));

    }

    @Test // Also tests updateGlobalWordCount, an extracted method.
    public void test_getGlobalWordCount() {

        Map<String,Integer> expectedResult = new HashMap<String,Integer>();
        expectedResult.put("the", 3);expectedResult.put("brown",1);expectedResult.put("cow",3);
        expectedResult.put("sits",3);expectedResult.put("in",3);expectedResult.put("blue",2);
        expectedResult.put("grass",2);expectedResult.put("field",1);

        Map<String,SparseDoc> testDocs = getMockDocs();

        TfIdfVectorizer vec = new TfIdfVectorizer();

        Object[] testInputArray = { testDocs };
        Class<?>[] classArray = { Map.class };

        ConcurrentHashMap<String, Integer> producedResult =
                (ConcurrentHashMap<String, Integer>)callTfIdfPrivateMethod("getGlobalWordCount", vec,
                        testInputArray, classArray);

        assertEquals(expectedResult.size(),producedResult.size());

        expectedResult.entrySet().forEach(entry ->
                assertEquals(entry.getValue(),
                        producedResult.get(entry.getKey())));

    }

    @Test
    public void test_getTfIdfEntries() {

        Map<String,SparseDoc> testDocs = getMockDocs();
        ConcurrentHashMap<String,Double> testIdfHash = getMockIdfHash();

        Map<String,Double> expectedResult = new HashMap<>();
        expectedResult.put("brown",1.0986); expectedResult.put("grass",.8109);

        TfIdfVectorizer vec = new TfIdfVectorizer();

        Object[] testInputArray = {testDocs.get("docA.txt").docHash,testIdfHash};
        Class<?>[] testClassArray = {Map.class,ConcurrentHashMap.class};

        Map<String,Double> producedResult =
                (Map<String,Double>)callTfIdfPrivateMethod("getTfIdfEntries", vec,testInputArray, testClassArray);

        /* Check that the size of the results and their value sets match */
        assertEquals(producedResult.size(), expectedResult.size());
        expectedResult.entrySet().forEach(e -> assertEquals((double) e.getValue(),
                producedResult.get(e.getKey()),
                .001));

    }

    @Test
    public void test_writeTdIdfEntriesToMatrix() {

        TfIdfVectorizer vec = new TfIdfVectorizer();

        double[][] testMatrix = { {0, 0, 0, 0}, {0, 0, 0, 0} };
        double[][] expectedMatrix = { {0, 1., 0, 2.}, {0, 0, 0, 0} };

        Map<String,Double> testTfIdfEntries = new HashMap<>();
        testTfIdfEntries.put("blue",1.); testTfIdfEntries.put("brown",2.);

        ConcurrentHashMap<String,Integer> testPresentWordsIndex = new ConcurrentHashMap<>();
        testPresentWordsIndex.put("cow",0); testPresentWordsIndex.put("blue",1);
        testPresentWordsIndex.put("brown",3);

        Object[] testInputArray = {testTfIdfEntries,testPresentWordsIndex,0,testMatrix};
        Class<?>[] testClassArray = {Map.class,ConcurrentHashMap.class, int.class, testMatrix.getClass()};

        callTfIdfPrivateMethod("writeTfIdfEntriesToMatrix", vec, testInputArray, testClassArray);

        /* Assert that each element of the returned matrix is correct */
        for(int i = 0; i < 2; i++) {
            for (int j = 0; j < 4; j++) {
                assertEquals(testMatrix[i][j],expectedMatrix[i][j],.001);
            }
        }
    }

    @Test
    public void test_getPresentWordsList() {

        Map<String,SparseDoc> testDocs = getMockDocs();

        List<String> expectedResult = Arrays.asList("blue", "brown", "grass", "field");

        TfIdfVectorizer vec = new TfIdfVectorizer();

        ConcurrentHashMap<String,Double> mockIdfHash = getMockIdfHash();

        Object[] testInputArray = { testDocs,mockIdfHash };
        Class<?>[] testClassArray = { Map.class,ConcurrentHashMap.class };

        List<String> producedResult = (List<String>)callTfIdfPrivateMethod("getPresentWordsList", vec,
                testInputArray, testClassArray);


        /* Assert that the results are of the same size, and that all elements
        of expectedResult (which are distinct) are contained by producedResult. */
        assertEquals(expectedResult.size(), producedResult.size());

        expectedResult.forEach(e -> assertTrue(producedResult.contains(e)));

    }

    @Test
    public void test_getIndexOfPresentWordsList() {

        List<String> testInput = Arrays.asList("blue", "brown", "grass", "field");

        TfIdfVectorizer vec = new TfIdfVectorizer();

        Object[] testInputArray = { testInput };
        Class<?>[] testClassArray = {List.class};

        ConcurrentHashMap<String,Integer> producedResult =
                (ConcurrentHashMap<String,Integer>)callTfIdfPrivateMethod("getPresentWordsIndex", vec,
                                                                          testInputArray, testClassArray);

        /* Assert that the result is of the same size as the input, and that all
        elements of testInput (which are distinct) are contained by producedResult. */
        assertEquals(testInput.size(), producedResult.size());

        testInput.forEach(e -> assertTrue(producedResult.containsKey(e)));

        List<Integer> producedIndices =
                producedResult.entrySet().stream()
                        .map(e -> e.getValue())
                        .collect(Collectors.toList());

        producedIndices.sort((a, b) -> a.compareTo(b));

        /* Assert that the indices are distinct and span the desired range */
        for(int i : producedIndices) {
            assertEquals((int)producedIndices.get(i),i++);
        }
    }

    @Test
    public void test_getTfIdfMatrix() {

        ConcurrentHashMap<String,Double> testIdfHash = getMockIdfHash();
        Map<String,SparseDoc> testDocs = getMockDocs();

        Map<String,Map<String,Integer>> expectedResult = new HashMap<>();

        TfIdfVectorizer vec = new TfIdfVectorizer();

        Object[] testInputArray = { testDocs, testIdfHash };
        Class<?>[] testClassArray = {Map.class, ConcurrentHashMap.class};

        TfIdfMatrix producedResult =
                (TfIdfMatrix)callTfIdfPrivateMethod("getTfIdfMatrix", vec,
                        testInputArray, testClassArray);

    }

/* ---------------------------
   Utility Methods
 * --------------------------- */

    /* I wrote this to allow me to use reflection to call a private method with an arbitrary number of parameters */
    private Object callTfIdfPrivateMethod(String methodName, TfIdfVectorizer vec, Object[] input, Class<?>[] inputType) {

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

    public List<List<String>> getMockTokens() {

        return Arrays.asList(
                Arrays.asList("the", "brown", "cow", "sits","in","the","grass"),
                Arrays.asList("the", "blue", "cow", "sits","in","the","grass"),
                Arrays.asList("the", "blue", "cow", "sits","in","the","field"));

    }

    public ConcurrentHashMap<String,Double> getMockIdfHash() {

        Map<String,SparseDoc> testInput = getMockDocs();

        TfIdfVectorizer vec = new TfIdfVectorizer();

        Object[] testInputArray = {testInput,1,(float).7};
        Class<?>[] classArray = {Map.class,int.class,float.class};

        return (ConcurrentHashMap<String, Double>)callTfIdfPrivateMethod("getNewIdfWordHash", vec,
                                                                         testInputArray, classArray);

    }

    private Map<String,SparseDoc> getMockDocs() {

        TfIdfVectorizer vec = new TfIdfVectorizer();

        Object[] testInputArray = { "./src/test/resources/testInputFolder/" };
        Class<?>[] testClassArray = { String.class };

        return (Map<String,SparseDoc>)callTfIdfPrivateMethod("processInputFolder", vec,
                                                             testInputArray, testClassArray);
    }
}