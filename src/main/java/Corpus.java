import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class Corpus {

/* ---------------------------
   Fields
   ---------------------------  */

    private ConcurrentHashMap<String,Integer> globalTokenIdsByString;
    private ConcurrentHashMap<Integer,String> globalStringsByTokenId;
    private Map<Integer,Integer> docOccurrencesByTokenId;
    private Map<Integer,Double> idfByTokenId;
    private Set<Integer> validTokenIds;
    private boolean locked;
    private boolean idfReady;
    private int nextTokenId;
    private int numDocs;


/* ---------------------------
   Defaults
   ---------------------------  */

    private int defaultCorpusSize = 8192; /* expected number of distinct words among all documents */


/* ---------------------------
   Constructor
   ---------------------------  */

    public Corpus() {

        this.locked = false;
        this.idfReady = false;
        this.globalTokenIdsByString = new ConcurrentHashMap<>(defaultCorpusSize);
        this.globalStringsByTokenId = new ConcurrentHashMap<>(defaultCorpusSize);
        this.docOccurrencesByTokenId = new HashMap<>(defaultCorpusSize);
        this.numDocs = 0;

    }

/* ---------------------------
   Get/Set
   ---------------------------  */

    public void setLock(boolean value) { this.locked = value; }
    public boolean getLock(boolean value) { return this.locked; }

    public void incNumDocs() { this.numDocs++; }
    public void setNumDocs(int value) { this.numDocs = value; }
    public int getNumDocs() { return this.numDocs; }

    public boolean isIdfCalculated() { return this.idfReady; }

    public double getIdf(int tokenId) { return idfByTokenId.get(tokenId); }


/* ---------------------------
   Public Methods
   ---------------------------  */

    /* Returns a unique id for a given token, or -1 if token is invalid. */
    public int getId(String word) {

        /* If unique id already exists for token, return it. */
        if (globalTokenIdsByString.containsKey(word)) {
            return globalTokenIdsByString.get(word);
        }
        /* If unique id does not exist for token, and corpus is unlocked,
           then generate a new id and return it; */
        else if (!this.locked) {
            this.globalTokenIdsByString.put(word,nextTokenId);
            this.globalStringsByTokenId.put(nextTokenId,word);
            return nextTokenId++;
        }
        /* If unique id is not present, and corpus is locked, token is invalid,
           so return -1. */
        else {
            return -1;
        }

    }

    public String getString(int tokenId) {

        return this.globalStringsByTokenId.get(tokenId);

    }

    /* Update docOccurrences and numDocs given a new document. */
    public void addDoc(SparseDoc doc) {

        for(TokenArrayElement token : doc) {
            if (docOccurrencesByTokenId.containsKey(token.tokenId)) {
                this.docOccurrencesByTokenId.put(token.tokenId, this.docOccurrencesByTokenId.get(token.tokenId) + 1);
            } else {
                this.docOccurrencesByTokenId.put(token.tokenId, 1);
            }
        }

        this.numDocs++;

        /* If a document has been added, then a previously calculated idf set is invalid. */
        this.idfReady = false;

    }

    /* Filter known tokens for those which fall in a doc frequency range, and
       modify validTokenIds to contain the filtered result. */
    public void filterValidTokens(int minDf, double maxDfRatio) {

        int maxDf = (int)Math.floor(maxDfRatio*this.numDocs);

        /* validTokenIds is instantiated only once, to reduce allocation cost on
           subsequent calls, the drawback being that it may be unnecessarily large
           if later calls only use a portion of the allocated space. */
        if(validTokenIds == null)
            validTokenIds = new HashSet<>(defaultCorpusSize);

        /* Modify validTokenIds, filtering for a new minDf and maxDf */
        docOccurrencesByTokenId.entrySet().forEach(entry -> {
                    if ((entry.getValue() >= minDf) & (entry.getValue() <= maxDf)) {
                        validTokenIds.add(entry.getKey());
                    }
                    else {
                        validTokenIds.remove(entry.getKey());
                    }});

    }

    public boolean isValidTokenId(int tokenId) {

        return validTokenIds.contains(tokenId);

    }

    /* Calculate idf for known documents and store in this.idfByTokenIdf */
    public void calcIdf() {

        if (validTokenIds == null)
            this.filterValidTokens(1, 1.);


        this.idfByTokenId =
                docOccurrencesByTokenId.entrySet().stream()
                                                  .collect(Collectors.toMap(Map.Entry::getKey,
                                                           e -> Math.log((float) this.numDocs / e.getValue())));

        this.idfReady = true;

    }

}
