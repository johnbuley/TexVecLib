import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class Utilities {

        /* I wrote this to allow me to use reflection to call a private method with an arbitrary number of parameters */
    public static Object callTfIdfPrivateMethod(String methodName, Object targetObj, Object[] input, Class<?>[] inputType) {

        Object result = null;

        try {
            Method testMethod = targetObj.getClass().getDeclaredMethod(methodName, inputType);
            testMethod.setAccessible(true);
            result = testMethod.invoke(targetObj, input);
        }
        catch (Exception e) {
            System.out.println(e.toString());
        }

        return result;

    }

    public static DocumentSet getMockDocs() {

        DocumentSet docSet = new DocumentSet();

        docSet.addFolder("./src/test/resources/testInputFolder/");

        return docSet;

    }

}
