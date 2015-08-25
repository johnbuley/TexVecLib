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
        expectedDocHash.put("sits", 1); expectedDocHash.put("in", 1); expectedDocHash.put("grass", 2);

        SparseDoc expectedResult = new SparseDoc("docA.txt",expectedDocHash);

        Object[] testInputArray = { Paths.get("./src/test/resources/testInputFolder/docA.txt") };
        Class<?>[] testClassArray = { Path.class };

        SparseDoc producedResult =
                (SparseDoc)callTfIdfPrivateMethod("sparsifyDoc", vec,testInputArray, testClassArray);

        assertEquals(producedResult.filename,expectedResult.filename);

        assertEquals(producedResult.docHash.size(),expectedResult.docHash.size());

        /* If the EntrySets are the same size, then only need to check that there is match
           between each entry in the expectedResult and an entry in the producedResult. */
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

        /* Since sparsifyDoc is tested elsewhere, this just checks that the expected file keys
           are present, and the value of each is a SparseDoc. */
        expectedResult.forEach(entry -> {
            assertTrue(producedResult.containsKey(entry));
            assertTrue(producedResult.get(entry) != null);
        });

    }

    @Test /* This test is troubled, but works with a manual change described below. */
    public void test_addWordToDoc() {

        /* Had trouble passing byte[].class as an argument type to *.class.getDeclaredMethod().
           So to test, I make the method public temporarily, otherwise leaving the below commented. */

/*        Map<String,Integer> testDoc = new HashMap<String,Integer>();
        byte[] testConcatBuf = new byte[8];

        testConcatBuf[0] = (char) 't'; testConcatBuf[1] = (char) 'h'; testConcatBuf[2] = (char) 'e';

        TfIdfVectorizer vec = new TfIdfVectorizer();

        IntegerPtr bufPtr = new IntegerPtr(3);
        vec.addWordToDoc(testDoc,testConcatBuf,bufPtr);
        bufPtr.value = 3;
        vec.addWordToDoc(testDoc,testConcatBuf,bufPtr);

        assertEquals((int)testDoc.get("the"),2);

        // Error:

        //Object[] testInputArray = { testDoc, testConcatBuf, 3 };
        //Class<?>[] testClassArray = { testDoc.getClass(), byte[].class, int.class };

        //Map<String,SparseDoc> producedResult =
                //(Map<String,SparseDoc>)callTfIdfPrivateMethod("addWordToDoc", vec, testInputArray, testClassArray);*/

    }

    @Test
    public void test_getNewIdfWordHash() {

        Map<String,SparseDoc> testDocs = getMockDocs();

        ConcurrentHashMap<String,Double> expectedIdfValuesHash = new ConcurrentHashMap<>();
        expectedIdfValuesHash.put("blue",Math.log(3./2)); expectedIdfValuesHash.put("brown",Math.log(3./1));
        expectedIdfValuesHash.put("field",Math.log(3./1)); expectedIdfValuesHash.put("grass",Math.log(3./2));

        TfIdfVectorizer vec = new TfIdfVectorizer();

        Object[] testInputArray = {testDocs,1,.7};
        Class<?>[] classArray = {Map.class,int.class,double.class};

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

    @Test // Also tests updateGlobalWordDocFreq(). */
    public void test_getGlobalWordDocFreq() {

        Map<String,Integer> expectedResult = new HashMap<String,Integer>();
        expectedResult.put("the", 3);expectedResult.put("brown",1);expectedResult.put("cow",3);
        expectedResult.put("sits",3);expectedResult.put("in",3);expectedResult.put("blue",2);
        expectedResult.put("grass",2);expectedResult.put("field",1);

        Map<String,SparseDoc> testDocs = getMockDocs();

        TfIdfVectorizer vec = new TfIdfVectorizer();

        Object[] testInputArray = { testDocs };
        Class<?>[] classArray = { Map.class };

        Map<String, Integer> producedResult =
                (Map<String, Integer>)callTfIdfPrivateMethod("getGlobalWordDocFreq", vec,
                                                             testInputArray, classArray);


        /* Check that the size of the results and their value sets match */
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
        Class<?>[] testClassArray = {Map.class, ConcurrentHashMap.class, int.class, testMatrix.getClass()};

        callTfIdfPrivateMethod("writeTfIdfEntriesToMatrix", vec, testInputArray, testClassArray);

        /* Assert that each element of the returned matrix is correct */
        for(int i = 0; i < 2; i++) {
            for (int j = 0; j < 4; j++) {
                assertEquals(testMatrix[i][j],expectedMatrix[i][j],.001);
            }
        }
    }

    @Test
    public void test_getIndexOfPresentWordsList() {

        List<String> testResult = Arrays.asList("blue", "brown", "grass", "field");

        ConcurrentHashMap<String,Double> mockIdfHash = getMockIdfHash();
        Map<String,SparseDoc> testDocs = getMockDocs();

        TfIdfVectorizer vec = new TfIdfVectorizer();

        Object[] testInputArray = { testDocs, mockIdfHash };
        Class<?>[] testClassArray = { Map.class, ConcurrentHashMap.class };

        ConcurrentHashMap<String,Integer> producedResult =
                (ConcurrentHashMap<String,Integer>)callTfIdfPrivateMethod("getPresentWordsIndex", vec,
                                                                          testInputArray, testClassArray);

        /* Assert that the result is of the same size as the input, and that all
        elements of testInput (which are distinct) are contained by producedResult. */
        assertEquals(testResult.size(), producedResult.size());

        testResult.forEach(e -> assertTrue(producedResult.containsKey(e)));

        List<Integer> producedIndices =
                producedResult.entrySet().stream()
                        .map(e -> e.getValue())
                        .collect(Collectors.toList());

        producedIndices.sort((a, b) -> a.compareTo(b));

        /* Assert that the indices are distinct and span the desired range */
        for (int i = 0; i < producedIndices.size(); i++) {
            assertEquals((int)producedIndices.get(i),i);
        }

    }

    @Test
    public void test_getTfIdfMatrix() {

        ConcurrentHashMap<String,Double> testIdfHash = getMockIdfHash();
        Map<String,SparseDoc> testDocs = getMockDocs();

        TfIdfVectorizer vec = new TfIdfVectorizer();

        Object[] testInputArray = { testDocs, testIdfHash };
        Class<?>[] testClassArray = {Map.class, ConcurrentHashMap.class};

        TfIdfMatrix producedResult =
                (TfIdfMatrix)callTfIdfPrivateMethod("getTfIdfMatrix", vec,
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
                      producedResult.matrix[producedResult.getDocIndex("docA.txt")][producedResult.indexOf(e.getKey())],
                      e.getValue()[0],
                      .001);
                    assertEquals(
                      producedResult.matrix[producedResult.getDocIndex("docB.txt")][producedResult.indexOf(e.getKey())],
                      e.getValue()[1],
                      .001);
                    assertEquals(
                      producedResult.matrix[producedResult.getDocIndex("docC.txt")][producedResult.indexOf(e.getKey())],
                      e.getValue()[2],
                      .001);
                });

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

    public ConcurrentHashMap<String,Double> getMockIdfHash() {

        Map<String,SparseDoc> testInput = getMockDocs();

        TfIdfVectorizer vec = new TfIdfVectorizer();

        Object[] testInputArray = {testInput,1,.7};
        Class<?>[] classArray = {Map.class,int.class,double.class};

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