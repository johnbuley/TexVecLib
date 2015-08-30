
public class IntArrayAsBytes {

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

        for (int i = 0; i < bytesPerElement; i++)
            result += (this.array[index*bytesPerElement+i] & 0xFF) << (((bytesPerElement-1)*8) - 8*i);

        return result;

    }

    public void set(int index, int value) {

        for (int i = 0; i < bytesPerElement; i++)
            this.array[index*bytesPerElement+i] = (byte) (value >> ((bytesPerElement-1)*8 - 8*i));

    }

    public void increment(int index) {

        this.set(index,this.get(index)+1);

    }

    public void resize(int newLength) {

        byte[] newArray = new byte[newLength*bytesPerElement];

        System.arraycopy(this.array,0,newArray,0,this.length*bytesPerElement);

        this.array = newArray;

        this.length = newLength;


    }

}
