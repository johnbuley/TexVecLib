import java.util.Iterator;
import java.util.NoSuchElementException;

public class DocIterator implements Iterator {

    private final IntArrayAsBytes tokenIdsArray;
    private final IntArrayAsBytes tokenCountsArray;
    private final int length;
    private int currentIndex;
    int[] item;

    public DocIterator(IntArrayAsBytes tokenIdsArray, IntArrayAsBytes tokenCountsArray, int length) {

        this.tokenIdsArray = tokenIdsArray;
        this.tokenCountsArray = tokenCountsArray;
        this.length  = length;
        currentIndex = 0;
        item = new int[2];

    }

    @Override
    public int[] next() {

        if (this.hasNext()) {

            item[0] = tokenIdsArray.get(currentIndex);
            item[1] = tokenCountsArray.get(currentIndex);
            currentIndex++;

            return item;

        }
        else {

            throw new NoSuchElementException("Index " + currentIndex + " exceeds length of doc array");

        }

    }

    @Override
    public boolean hasNext() {

        return (currentIndex < this.length);

    }

}
