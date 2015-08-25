import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    private int ioBufferSize = 1024; /* bytes */
    private int defaultDocSize = 1024; /* expected number of distinct words in a document */
    private int defaultCorpusSize = 2048; /* expected number of distinct words among all documents */
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
    public void fit(Map<String,SparseDoc> docs, int minDf, float maxDfRatio) {

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
    public TfIdfMatrix fitTransform(Map<String,SparseDoc> docs, int minDf, float maxDfRatio) {

        this.fit(docs,minDf,maxDfRatio);
        return transform(docs);

    }

    /* This is an overloaded method for handling folder inputs */
    public void fit(String inputFolder, int minDf, float maxDfRatio) {

        /* Call 'base' method now that input is formatted */
        this.fit(this.processInputFolder(inputFolder), minDf, maxDfRatio);

    }

    /* This is an overloaded method for handling folder inputs */
    public TfIdfMatrix transform(String inputFolder) {

        return this.transform(this.processInputFolder(inputFolder));

    }

    /* This is an overloaded method for handling folder inputs */
    public TfIdfMatrix fitTransform(String inputFolder, int minDf, float maxDfRatio) {

        Map<String,SparseDoc> input = this.processInputFolder(inputFolder);

        this.fit(input,minDf,maxDfRatio);
        return transform(input);

    }


/* --------------------------
   Ingestion
 * -------------------------- */

    /* Reads a file through a FileChannel, returns a sparse representation of the document */
    public SparseDoc sparsifyDoc(Path filePath) throws IOException {

        byte[] concatBuf = new byte[maxStringLength];

        Map<String, Integer> doc = new HashMap<>(defaultDocSize);

        ByteBuffer buf = ByteBuffer.allocate(ioBufferSize);

        FileChannel fileChannel = (new FileInputStream(filePath.toString())).getChannel();

        int bytesRead;
        IntegerPtr bufPtr = new IntegerPtr(0);
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
                   If so, write the current contents of the buffer as a word and reset. */
                if (bufPtr.value == maxStringLength-1) {
                    addWordToDoc(doc,concatBuf,bufPtr);
                }
            }

            buf.clear();
        }

        return new SparseDoc(filePath.getFileName().toString(),doc);

    }

    /* Finds .txt files in a given folder and collects them into a list of strings */
    private Map<String,SparseDoc> processInputFolder(String folderName) {

        Path inputPath = Paths.get(folderName);

        ExecutorService executorService = Executors.newFixedThreadPool(numThreads);

        List<Future<SparseDoc>> tasks = new ArrayList<>();

        if (Files.exists(inputPath)) {
            try {
                DirectoryStream<Path> stream = Files.newDirectoryStream(inputPath, "*.txt");

                /*  Iterate through files in input folder, transform each to string,
                *   and add each to list of document strings. */
                for (Path filePath : stream) {
                    try {
                        tasks.add(executorService.submit(() -> sparsifyDoc(filePath)));
                    }
                    catch (Exception e) { System.err.println("Error reading file: " + filePath.toString()); }
                }
            }
            catch (IOException e) { System.err.println("Error reading input folder" + e.toString()); }
        }
        else { System.err.println("Not a valid path."); }


        /* Allocates for the number of documents */
        Map<String,SparseDoc> docs = new HashMap<>(tasks.size());

        /* Iterates through the list of futures.  As a future is completed, the result
           is added to the global map and the task is removed from the list. */
        while(tasks.size() > 0) {

            Iterator<Future<SparseDoc>> iterator = tasks.iterator();
            while (iterator.hasNext()) {
                Future<SparseDoc> task = iterator.next();
                try {
                    if (task.isDone()) {
                        docs.put(task.get().filename, task.get());
                        iterator.remove();
                    }
                }
                catch (Exception e) { System.err.println("Future exception"); }
            }

        }

        safeExecutorServiceShutdown(executorService);

        return docs;
    }

    /* This behavior was extracted from sparsifyDoc for readability under the assumption/hope
       that the compiler will inline it */
    private static void addWordToDoc(Map<String,Integer> doc, byte[] concatBuf, IntegerPtr bufPtr) {

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
                                                               int minDf, float maxDfRatio) {

        ConcurrentHashMap<String,Integer> allWordCounts = getGlobalWordCount(docs);

        int docCount = docs.size();
        /* Calculate max-allowable document frequency based on maxDfRatio and number of docs */
        int maxDf = (int)Math.floor(maxDfRatio*docCount);


        /* Filter the result of the previous section according to max- and min-allowable
           document frequency, then collect into a map where the value is the idf. */
        Map<String, Double> idfValues =
                allWordCounts.entrySet().stream()
                                        .filter(e -> (e.getValue() >= minDf && e.getValue() <= maxDf))
                                        .collect(Collectors.toMap(Map.Entry::getKey,
                                                e -> Math.log((float) docCount / e.getValue())));


        ConcurrentHashMap<String, Double> idfWordHash = new ConcurrentHashMap<>(defaultCorpusSize);
        int i = 0;
        /* Copy elements of idfValues to a ConcurrentHashMap, along with a
           unique id that will be used to index the final tf-idf matrix. */
        for(Map.Entry e : idfValues.entrySet()) {
            //idfWordHash.put((String)e.getKey(),new IdfWord(1,(double)e.getValue()));
            idfWordHash.put((String)e.getKey(),(double)e.getValue());
        }

        return idfWordHash;
    }

    /* Calculate document frequency
       df(word) = |{docs d | d contains word}| */
    private ConcurrentHashMap<String,Integer> getGlobalWordCount(Map<String,SparseDoc> docs) {

        ConcurrentHashMap<String,Integer> allWordCounts =
                new ConcurrentHashMap<>(defaultCorpusSize,(float).75,numThreads);

        List<Future<?>> tasks = new ArrayList<>(docs.size());

        ExecutorService executorService = Executors.newFixedThreadPool(numThreads);

        docs.entrySet().forEach(entry ->
                tasks.add(executorService.submit(() ->
                        updateGlobalWordCount(allWordCounts, entry.getValue()))));

        waitForTasksToComplete(tasks);

        safeExecutorServiceShutdown(executorService);

        return allWordCounts;

    }

    /* Method called by executor threads to calculate document frequency in parallel. */
    private void updateGlobalWordCount(ConcurrentHashMap<String,Integer> globalWordCounts,
                                       SparseDoc doc) {

        Integer currentCount;

        for(HashMap.Entry entry : doc.docHash.entrySet()) {

            /* By assigning either the desired value or null to a variable and then checking
               if it's null, I only need to touch the shared hash twice to add/update a value */
            currentCount = globalWordCounts.get(entry.getKey());
            if (currentCount != null) {
                globalWordCounts.put((String)entry.getKey(),currentCount+1);
            }
            else {
                globalWordCounts.put((String)entry.getKey(),1);
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

           Elsewhere, I have parallelized tasks of this size, however it relies on
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