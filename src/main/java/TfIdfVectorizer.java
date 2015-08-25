import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class TfIdfVectorizer {

/* --------------------------
   Fields
 * -------------------------- */

    private boolean initialized;
    private ConcurrentHashMap<String,Double> idfHash;


/* --------------------------
   Defaults
 * -------------------------- */

    private int maxStringLength = 64; /* chars */
    private int ioBufferSize = 4096; /* bytes */
    private int defaultDocSize = 4096; /* expected number of distinct words in a document */
    private int defaultCorpusSize = 8192; /* expected number of distinct words among all documents */
    private int numThreads = Math.max(Runtime.getRuntime().availableProcessors() - 1, 1);


/* --------------------------
   Get/Set
 * -------------------------- */

    public int getMaxStringLength() { return maxStringLength; }
    public void setMaxStringLength(int value) { maxStringLength = value; }

    public int getIoBufferSize() { return ioBufferSize; }
    public void setIoBufferSize(int value) { ioBufferSize = value; }

    public int getDefaultDocSize() { return defaultDocSize; }
    public void setDefaultDocSize(int value) { defaultDocSize = value; }

    public int getDefaultCorpusSize() { return defaultCorpusSize; }
    public void setDefaultCorpusSize(int value) { defaultCorpusSize = value; }

    public int getNumThreads() { return numThreads; }
    public void setNumThreads(int value) { numThreads = value; }


/* --------------------------
   Constructor
 * -------------------------- */

    public TfIdfVectorizer() {

        initialized = false;

    }


/* --------------------------
   Public Methods
 * -------------------------- */

    /* Base method for training a tf-idf model */
    public void fit(Map<String,SparseDoc> docs, int minDf, double maxDfRatio) {

        this.idfHash = this.getNewIdfWordHash(docs, minDf, maxDfRatio);
        this.initialized = true;

    }

    /* Base method for generating a tf-idf matrix */
    public TfIdfMatrix transform(Map<String,SparseDoc> docs) {

        if (initialized)
            return this.getTfIdfMatrix(docs, this.idfHash);
        else {
            System.err.println("TfIdfVectorizer object must be initialized with fit().");
            return null;
        }

    }

    /* Base method for fitting and transforming in one step */
    public TfIdfMatrix fitTransform(Map<String,SparseDoc> docs, int minDf, double maxDfRatio) {

        this.fit(docs,minDf,maxDfRatio);
        return transform(docs);

    }

    /* This is an overloaded method for handling folder inputs */
    public void fit(String inputFolder, int minDf, double maxDfRatio) {

        /* Call 'base' method now that input is formatted */
        this.fit(this.processInputFolder(inputFolder), minDf, maxDfRatio);

    }

    /* This is an overloaded method for handling folder inputs */
    public TfIdfMatrix transform(String inputFolder) {

        return this.transform(this.processInputFolder(inputFolder));

    }

    /* This is an overloaded method for handling folder inputs */
    public TfIdfMatrix fitTransform(String inputFolder, int minDf, double maxDfRatio) {

        Map<String,SparseDoc> input = this.processInputFolder(inputFolder);

        this.fit(input,minDf,maxDfRatio);
        return transform(input);

    }


/* --------------------------
   Ingestion
 * -------------------------- */

    /* Finds .txt files in a given folder and collects them into a list of strings */
    private Map<String,SparseDoc> processInputFolder(String folderName) {

        Path inputPath = Paths.get(folderName);

        List<SparseDoc> results = new ArrayList<>();

        if (Files.exists(inputPath)) {
            try {
                DirectoryStream<Path> stream = Files.newDirectoryStream(inputPath, "*.txt");
                /*  Iterate through files in input folder, transform each to string,
                *   and add each to list of document strings. */
                for (Path filePath : stream) {
                    try {
                        results.add(sparsifyDoc(filePath));
                    }
                    catch (Exception e) {
                        System.err.println("Error reading file: " + filePath.toString() +
                                           " " + e.toString());
                    }
                }
            }
            catch (IOException e) { System.err.println("Error reading input folder" + e.toString()); }
        }
        else {
            System.err.println("Not a valid path.");
            return null;
        }

        /* Allocates for the number of documents */
        Map<String,SparseDoc> docs = new HashMap<>(results.size());

        results.forEach(doc -> docs.put(doc.filename, doc));

        return docs;
    }

    /* Reads a file through a FileChannel, returns a sparse representation of the document */
    public SparseDoc sparsifyDoc(Path filePath) throws IOException {

        Map<String, Integer> doc = new HashMap<>(defaultDocSize);

        ByteBuffer buf = ByteBuffer.allocate(ioBufferSize);
        byte[] concatBuf = new byte[maxStringLength];

        /* There seems to be a debate over the performance of synchronous NIO vs IO.  I worked
           with NIO because I was experimenting with parallelizing ingestion. */
        FileChannel fileChannel = (new FileInputStream(filePath.toString())).getChannel();

        int bytesRead;
        /* The integer pointer is only necessary to allow the buffer pointer to
           be reset within addWordToDoc.  If this behavior were done in-line, then
           an int would be used. */
        IntegerPtr bufPtr = new IntegerPtr(0);
        /* A performance boost could be gained from generating a string
           directly from the FileChannel buffer, rather than a separate one,
           but that would be lossy in cases where a token spanned two separate
           FileChannel reads.  In this case, a token is only lost if it
           exceeds the size of the concatenation buffer.
           A small improvement I thought of late was the following: if I'm processing
           in place, without copying, and the last n bytes in the read buffer are part
           of an unfinished token, then on the next read only ioBufferSize-n bytes,
           preserving the first part of the token at the end of the read buffer, which
           can be joined with the second part at the beginning of the read buffer.
           Only in this case would I copy to a separate concat buffer. */
        while ((bytesRead = fileChannel.read(buf)) != -1) {

            for (int i = 0; i < bytesRead; i++) {

                byte c = buf.get(i);
                if /* lower-case letter */ ((c >= 97) & (c <= 122)) {
                    concatBuf[bufPtr.value++] = c;
                }
                else if /* upper-case letter */ ((c >= 65) & (c <= 90)) {
                    concatBuf[bufPtr.value++] = (byte) (c + 32);
                }
                else /* then split token */ {
                    if (bufPtr.value > 0) {
                        addWordToDoc(doc,concatBuf,bufPtr);
                    }
                }
                /* Check that string concat buffer cannot overflow on next iteration.
                   If so, write the current contents of the buffer as a token and reset. */
                if (bufPtr.value == maxStringLength-1) {
                    addWordToDoc(doc,concatBuf,bufPtr);
                }
            }

            buf.clear();
        }

        return new SparseDoc(filePath.getFileName().toString(),doc);

    }

    /* This behavior was extracted from sparsifyDoc for readability under the assumption/hope
   that the compiler will inline it */
    private void addWordToDoc(Map<String,Integer> doc, byte[] concatBuf, IntegerPtr bufPtr) {

        String word = new String(concatBuf, 0, bufPtr.value);
        bufPtr.value = 0;

        Integer currentCount = doc.get(word);
        if (currentCount != null) {
            doc.put(word, currentCount + 1);
        } else {
            doc.put(word, 1);
        }

    }


/* --------------------------
   Fit
 * -------------------------- */

    /* Top-level method for returning a collection of <word,idf> pairs */
    private ConcurrentHashMap<String,Double> getNewIdfWordHash(Map<String, SparseDoc> docs,
                                                               int minDf, double maxDfRatio) {

        Map<String,Integer> wordDocFreqs = getGlobalWordDocFreq(docs);

        int docCount = docs.size();
        /* Calculate max-allowable document frequency based on maxDfRatio and number of docs */
        int maxDf = (int)Math.floor(maxDfRatio*docCount);


        /* Filter the result of the previous section according to max- and min-allowable
           document frequency, then collect into a map where the value is the idf. */
        Map<String, Double> idfValues =
                wordDocFreqs.entrySet().stream()
                                        .filter(e -> (e.getValue() >= minDf && e.getValue() <= maxDf))
                                        .collect(Collectors.toMap(Map.Entry::getKey,
                                                e -> Math.log((float) docCount / e.getValue())));


        ConcurrentHashMap<String, Double> idfWordHash = new ConcurrentHashMap<>(defaultCorpusSize);
        /* Copy elements of idfValues to a ConcurrentHashMap, keyed by the filename. */
        for(Map.Entry e : idfValues.entrySet()) {
            idfWordHash.put((String)e.getKey(),(double)e.getValue());
        }

        return idfWordHash;
    }

    /* Calculate document frequency
       df(word) = |{docs d | d contains word}| */
    private Map<String,Integer> getGlobalWordDocFreq(Map<String, SparseDoc> docs) {

        /* This section was parallelized, but I caught an error caused by the omission of
         * a synchronization point.  So, as described in Discussion 2, this should be joined
         * with the ingestion process, because the synchronization will overwhelm the
         * performance boost, but for now this is a fix. */

        Map<String,Integer>globalWordDocFreqs =
                new HashMap<>(defaultCorpusSize);

        docs.entrySet().forEach(entry -> updateGlobalWordDocFreqs(globalWordDocFreqs, entry.getValue()));

        return globalWordDocFreqs;

    }

    /* Method called by executor threads to calculate document frequency in parallel.
       Method no longer needed when this functionality is joined with ingestion.  */
    private void updateGlobalWordDocFreqs(Map<String, Integer> globalWordDocFreqs,
                                          SparseDoc doc) {

        Integer currentCount;

        for(HashMap.Entry entry : doc.docHash.entrySet()) {

            /* Below comment only relevant when this method was called by multiple threads. */
            /* By assigning either the desired value or null to a variable and then checking
               if it's null, I only need to touch the shared hash twice to add/update a value */
            currentCount = globalWordDocFreqs.get(entry.getKey());
            if (currentCount != null) {
                globalWordDocFreqs.put((String) entry.getKey(), currentCount + 1);
            }
            else {
                globalWordDocFreqs.put((String) entry.getKey(), 1);
            }

        }

    }


/* --------------------------
   Transform
 * -------------------------- */

    /* Top-level method for returning a TfIdfMatrix object */
    private TfIdfMatrix getTfIdfMatrix(Map<String, SparseDoc> docs,
                                       ConcurrentHashMap<String, Double> idfHash) {


        /* This method requires two passes over the documents.  This first pass
           determines the subset of the global vocabulary that is present in
           the documents.  In this way, the tf-idf matrix need only be as large
           as numDocs x |subset of vocabulary|. */
        ConcurrentHashMap<String,Integer> presentWordsIndex = getPresentWordsIndex(docs, idfHash);


        /* Assign row indices to each doc key */
        Map<String,Integer> docIndex = new HashMap<>(docs.size());
        int i = 0;

        for (SparseDoc doc : docs.values()) {
            docIndex.put(doc.filename,i++);
        }


        /* Initialize result matrix */
        int numDocs = docs.size();

        double[][] resultMatrix = new double[numDocs][presentWordsIndex.size()];


        /* This second pass calculates the tf-idf for each word, and writes
           it to the result matrix. */
        asyncCalcTfIdfAndWrite(docs, docIndex, idfHash, presentWordsIndex, resultMatrix);

        normalizeTfIdfMatrixRowWise(resultMatrix);


        return new TfIdfMatrix(resultMatrix,presentWordsIndex,docIndex);
    }

    /* Returns a hashmap that is an index of the 'valid' words, which is the intersection of the
       fit and transform corpora. */
    private ConcurrentHashMap<String,Integer> getPresentWordsIndex(Map<String, SparseDoc> docs,
                                                                   ConcurrentHashMap<String, Double> idfHash) {

        ConcurrentHashMap<String,Integer> presentWordsIndex = new ConcurrentHashMap<>(defaultCorpusSize);

        /* Iterate through documents.  If a word in the doc set has a corresponding
           precalculated idf, then add it to the set of valid words.

           I would try to parallelize this computation, but it relies on
           a shared integer for providing a unique index, and so would be hobbled
           by synchronization. */
        int i = 0;
        for(SparseDoc doc : docs.values()) {
            for(String word : doc.docHash.keySet()) {
                if (!presentWordsIndex.containsKey(word)) {
                    if (idfHash.containsKey(word)) {
                        presentWordsIndex.put(word,i++);
                    }
                }
            }
        }

        return presentWordsIndex;

    }

    /* This method asynchronously calls getTfIdfEntries() and then writeTfIdfEntriesToMatrix() */
    private void asyncCalcTfIdfAndWrite(Map<String, SparseDoc> docs,
                                        Map<String, Integer> docIndex,
                                        ConcurrentHashMap<String, Double> idfHash,
                                        ConcurrentHashMap<String, Integer> presentWordsListIndex,
                                        double[][] resultMatrix) {


        ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
        /* Passes a callable (as a lambda) to an ExecutorService for each document.  This callable
           calculates the non-zero entries for a particular document, and then writes these to a
           2-d array.  */
        List<Future<?>> tasks =
                docs.entrySet().stream()
                               .map(entry ->
                                       executorService.submit(() ->
                                               writeTfIdfEntriesToMatrix(
                                                       getTfIdfEntries(entry.getValue().docHash,
                                                                       idfHash),
                                                       presentWordsListIndex,
                                                       docIndex.get(entry.getKey()),
                                                       resultMatrix)))
                               .collect(Collectors.toList());

        waitForTasksToComplete(tasks);

        safeExecutorServiceShutdown(executorService);
    }

    /* Returns a sparse (hash) representation of the non-zero entries in the
       tf-idf matrix for one document.
       tf-idf(word w in doc d) = |w occurrences in d| * idf(w) */
    private Map<String,Double> getTfIdfEntries(Map<String, Integer> doc,
                                               ConcurrentHashMap<String, Double> idfHash) {


        /* Filter for words present in global vocab, calculate tf-idf, and collect */
        return doc.entrySet().stream()
                             .filter(e -> idfHash.containsKey(e.getKey()))
                             .map(e ->
                                    new Object() {
                                        String key = e.getKey();
                                        Double value = e.getValue() * idfHash.get(e.getKey());
                                    })
                             .collect(Collectors.toMap(e -> e.key, e -> e.value));

    }

    /* Writes the sparse (hash) representation of the non-zero entries to a matrix */
    private void writeTfIdfEntriesToMatrix(Map<String, Double> tfIdfEntries,
                                           ConcurrentHashMap<String, Integer> presentWordsListIndex,
                                           int row, double[][] resultMatrix) {

        for(Map.Entry e : tfIdfEntries.entrySet()) {
            resultMatrix[row][presentWordsListIndex.get(e.getKey())] = (double)e.getValue();
        }

    }


/* --------------------------
   Utility
 * -------------------------- */

    private boolean safeExecutorServiceShutdown(ExecutorService executorService) {

        executorService.shutdownNow();

        boolean successfulShutdown = false;
        try {
            successfulShutdown = executorService.awaitTermination(5, TimeUnit.SECONDS);
        }
        catch (Exception e) { System.err.println(e.toString()); }

        if (!successfulShutdown) {
            System.err.println("Error terminating ExecutorService");
            return false;
        }
        else {
            return true;
        }

    }

    /* Called by asyncCalcTfIdfAndWrite to check the list of Futures for completion */
    private void waitForTasksToComplete(List<Future<?>> tasks) {

        try {
            int checkSum = 0;
            while (checkSum != tasks.size()) {
                checkSum = (int)tasks.stream()
                        .map(task -> task.isDone())
                        .filter(b -> b) /* The elements were mapped to a boolean */
                        .count();
                Thread.sleep(5);
            }
        }
        catch (Exception e) {
            System.out.println("Timeout");
        }

    }

    /* Transform each row of a matrix to a unit vector */
    private void normalizeTfIdfMatrixRowWise(double[][] matrix) {

        int m = matrix.length;
        int n = matrix[0].length;

        double rs = 0;
        for(int i = 0; i < m; i++) {
            rs = 0;
            for (int j = 0; j < n; j++) {
                rs += Math.pow(matrix[i][j],2);
            }
            rs = Math.sqrt(rs);
            for (int j = 0; j < n; j++) {
                matrix[i][j] /= rs;
            }
        }
    }
}