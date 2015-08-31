
public class IntArrayAsBytes {

    /* This class provides storage of an n-element integer array as an n*bytesPerElement
       byte array.  The Consumer can set and get using the abstract n-element index. */

    private byte[] array;
    private int length;
    private final int bytesPerElement;

    public IntArrayAsBytes(int length, int bytesPerElement) {

        array = new byte[length*bytesPerElement];
        this.length = length;
        this.bytesPerElement = bytesPerElement;

    }

    public int get(int index) {

        int result = 0;

        /* Use a bit mask because Java stores bytes as signed bytes. */
        for (int i = 0; i < bytesPerElement; i++)
            result += (this.array[index*bytesPerElement+i] & 0xFF) << (((bytesPerElement-1)*8) - 8*i);

        return result;

    }

    public void set(int index, int value) {

        for (int i = 0; i < bytesPerElement; i++)
            this.array[index*bytesPerElement + i] = (byte) (value >> 8*(bytesPerElement-1-i));

    }

    public void increment(int index) {

        this.set(index,this.get(index)+1);

    }

    /* Creates a new byte array, copies the contents of the old array to the new, and
       replaces the old array reference with the new one. */
    public void resize(int newLength) {

        byte[] newArray = new byte[newLength*bytesPerElement];

        if(this.length < newLength)
            System.arraycopy(this.array,0,newArray,0,this.length*bytesPerElement);
        else
            System.arraycopy(this.array,0,newArray,0,newLength*bytesPerElement);

        this.array = newArray;

        this.length = newLength;


    }

}
