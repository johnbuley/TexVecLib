import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

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

        List<String> producedResult = (List<String>)callTfIdfPrivateMethod("processInputFolder",String.class,vec,"./src/test/resources/testInputFolder/");

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

        List<List<String>> producedResult = (List<List<String>>)callTfIdfPrivateMethod("splitDocuments",List.class,vec,testInput);

        assertEquals(expectedResult,producedResult);
    }

    public Object callTfIdfPrivateMethod(String methodName, Class<?> inputType, TfIdfVectorizer vec, Object input) {

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