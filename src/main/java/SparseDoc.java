import java.util.HashMap;
import java.util.Map;

public class SparseDoc implements Iterable<TokenArrayElement>{

    /* SparseDoc is a container for two IntArrayAsBytes, one storing token ids,
       the other storing token counts.  It returns an iterator that joins the
       elements of the two arrays in one TokenArrayElement.  At creation, it
       relies on a hashed index to the arrays, which is disposed when compress()
       is called, after which the doc becomes immutable.  */

/* ---------------------------
   Constructor
   ---------------------------  */

    public String docName;
    private Map<Integer,Integer> arrayIndex;
    private int nextArrayIndex;
    private IntArrayAsBytes tokenIdsArray;
    private IntArrayAsBytes tokenCountsArray;
    private int length;
    private int maxLength;
    private boolean locked;


/* ---------------------------
   Constructor
   ---------------------------  */

    public SparseDoc(String docName, int startLength, int bytesPerToken) {

        this.docName = docName;
        this.arrayIndex = new HashMap<>();
        this.tokenIdsArray = new IntArrayAsBytes(startLength,bytesPerToken);
        this.tokenCountsArray = new IntArrayAsBytes(startLength,bytesPerToken);
        this.length = 0;
        this.maxLength = startLength;
        this.nextArrayIndex = 0;
        this.locked = false;

    }


/* ---------------------------
   Get/Set
   ---------------------------  */

    public int length() { return this.length; }

    public int getTokenIdAt(int index) { return this.tokenIdsArray.get(index); }

    public int getTokenCountAt(int index) { return this.tokenCountsArray.get(index); }


/* ---------------------------
   Byte array management
   ---------------------------  */

    public void addToken(int globalTokenId) {

        /* A sparse doc is locked after compression and becomes effectively immutable */
        if (!this.locked) {
            if (arrayIndex.containsKey(globalTokenId)) {

            /* Increment count for given token */
                this.tokenCountsArray.increment(arrayIndex.get(globalTokenId));

            } else {

            /* Check if new token will overrun the array boundary. */
                if (this.length == maxLength) {

                /* Double size of arrays */
                    this.tokenIdsArray.resize(this.length * 2);
                    this.tokenCountsArray.resize(this.length * 2);
                    this.maxLength = this.length * 2;

                }

                int newIndex = getNextArrayIndex();
                this.arrayIndex.put(globalTokenId, newIndex);

                this.tokenIdsArray.set(newIndex, globalTokenId);
                this.tokenCountsArray.set(arrayIndex.get(globalTokenId), 1);
                this.length++;

            }
        }
        else {
            System.err.println("Document " + this.docName + " locked.  Token not added.");
        }
    }

    /* Get next unused array index */
    private int getNextArrayIndex() {

        return this.nextArrayIndex++;

    }

    /* Compress arrays to minimum-needed size, dispose arrayIndex */
    public void compress() {

        this.arrayIndex = null;
        this.tokenIdsArray.resize(this.length);
        this.tokenCountsArray.resize(this.length);

        this.locked = true;

    }

    /* Return an iterator that joins the values of tokenIdsArray and tokenCountsArray */
    public DocIterator iterator() {

        return new DocIterator(this.tokenIdsArray,this.tokenCountsArray,this.length());

    }

}
