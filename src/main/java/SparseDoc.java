import java.util.HashMap;
import java.util.Map;

class SparseDoc implements Iterable<int[]>{

    public String docName;
    private Map<Integer,Integer> arrayIndex;
    private int nextTokenId;
    private IntArrayAsBytes tokenIdsArray;
    private IntArrayAsBytes tokenCountsArray;
    private int length;
    private int maxLength;

    public static SparseDoc getSparseDoc(String docName, int startLength) {

        return new SparseDoc(docName, startLength);

    }

    public SparseDoc(String docName, int startLength) {

        this.docName = docName;
        this.arrayIndex = new HashMap<>();
        this.tokenIdsArray = new IntArrayAsBytes(startLength,3);
        this.tokenCountsArray = new IntArrayAsBytes(startLength,3);
        this.length = 0;
        this.maxLength = startLength;
        this.nextTokenId = 0;

    }


/* ---------------------------
   Get/Set
   ---------------------------  */

    public int length() { return this.length; }


/* ---------------------------
   TokenId-scale operations
   ---------------------------  */

    private int getNextTokenId() {

        return this.nextTokenId++;

    }

    public void addToken(int globalTokenId) {

        if (arrayIndex.containsKey(globalTokenId)) {

            //this.tokenCountsArray.set(arrayIndex.get(globalTokenId),this.tokenCountsArray.get(arrayIndex.get(globalTokenId))+1);
            this.tokenCountsArray.increment(arrayIndex.get(globalTokenId));

        }
        else {

            if (this.length == maxLength) {

                this.tokenIdsArray.resize(this.length * 2);
                this.tokenCountsArray.resize(this.length * 2);
                this.maxLength = this.length * 2;

            }

            int newId = getNextTokenId();
            this.arrayIndex.put(globalTokenId,newId);

            this.tokenIdsArray.set(newId, globalTokenId);
            this.tokenCountsArray.set(arrayIndex.get(globalTokenId),1);
            this.length++;

        }

    }

    /* Compress arrays to minimum-needed size, dispose arrayIndex */
    public void compress() {

        this.arrayIndex = null;
        this.tokenIdsArray.resize(this.length);
        this.tokenCountsArray.resize(this.length);

    }

    public DocIterator iterator() {

        return new DocIterator(this.tokenIdsArray,this.tokenCountsArray,this.length());

    }

}
