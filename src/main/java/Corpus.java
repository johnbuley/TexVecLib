import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class Corpus {

    private Map<String,Integer> globalTokenIdsByString;
    private Map<Integer,Integer> docOccurrencesByTokenId;
    private Map<Integer,Double> idfByTokenId;
    private Set<Integer> validTokenIds;
    private boolean locked;
    private boolean calcIdf;
    private int nextTokenId;
    private int numDocs;

    public Corpus() {

        this.locked = false;
        this.calcIdf = false;
        this.globalTokenIdsByString = new HashMap<>();
        this.docOccurrencesByTokenId = new HashMap<>();
        this.numDocs = 0;

    }

    public void setLock(boolean value) { this.locked = value; }
    public boolean getLock(boolean value) { return this.locked; }

    public void setCalcIdf(boolean value) { this.calcIdf = value; }
    public boolean getCalcIdf(boolean value) { return this.calcIdf; }

    public void incNumDocs() { this.numDocs++; }
    public void setNumDocs(int value) { this.numDocs = value; }
    public int getNumDocs() { return this.numDocs; }



    public int getId(String word) {

        if (globalTokenIdsByString.containsKey(word)) {
            return globalTokenIdsByString.get(word);
        }
        else if (!this.locked) {
            this.globalTokenIdsByString.put(word,nextTokenId);
            return nextTokenId++;
        }
        else {
            return -1;
        }

    }

    public void addDoc(SparseDoc doc) {

        for(int[] word : doc) {
            if (docOccurrencesByTokenId.containsKey(word[0])) {
                this.docOccurrencesByTokenId.put(word[0], this.docOccurrencesByTokenId.get(word[0]) + 1);
            } else {
                this.docOccurrencesByTokenId.put(word[0], 1);
            }
        }

        this.numDocs++;

    }

    public void filterValidWords(int minDf, double maxDfRatio) {

        int maxDf = (int)Math.floor(maxDfRatio*this.numDocs);

        if(validTokenIds == null) {
            validTokenIds = new HashSet<>();
        }

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

    public void calcIdf() {

        idfByTokenId =
                docOccurrencesByTokenId.entrySet().stream()
                                                  .collect(Collectors.toMap(Map.Entry::getKey,
                                                          e -> Math.log((float) this.numDocs / e.getValue())));

    }

}
