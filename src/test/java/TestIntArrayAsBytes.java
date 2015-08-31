import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TestIntArrayAsBytes {

    @Test
    public void test_IntArrayAsBytes() {

        IntArrayAsBytes testArray = new IntArrayAsBytes(4,3);

        int[] testValues = { 0, 255, 127, 10 };

        /* Separate for-loops are used to test whether values are overwritten on subsequent iterations */
        for (int i = 0; i < 4; i++)
            testArray.set(i,testValues[i]);

        for (int i = 0; i < 4; i++)
            assertEquals(testValues[i],testArray.get(i));

        for (int i = 0; i < 4; i++)
            testArray.increment(i);

        for (int i = 0; i < 4; i++)
            assertEquals(testValues[i]+1,testArray.get(i));

    }

}
