import java.util.Iterator;
import java.util.NoSuchElementException;

public class DocIterator implements Iterator<TokenArrayElement> {

    private final IntArrayAsBytes tokenIdsArray;
    private final IntArrayAsBytes tokenCountsArray;
    private final int length;
    private int currentIndex;
    TokenArrayElement element;

    public DocIterator(IntArrayAsBytes tokenIdsArray, IntArrayAsBytes tokenCountsArray, int length) {

        this.tokenIdsArray = tokenIdsArray;
        this.tokenCountsArray = tokenCountsArray;
        this.length  = length;
        currentIndex = 0;

        /* element is the object returned by the iterator, created once. */
        element = new TokenArrayElement();

    }

    @Override
    public TokenArrayElement next() {

        if (this.hasNext()) {

            /* Set fields of returned object. */
            element.tokenId = tokenIdsArray.get(currentIndex);
            element.tokenCount = tokenCountsArray.get(currentIndex);

            currentIndex++;

            return element;

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
