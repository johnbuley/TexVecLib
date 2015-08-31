import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class TfIdfVectorizer {

/* The fields of TfIdfVectorizer have been distributed to other classes, so at
   this point the class could be made static. */

/* --------------------------
   Defaults
 * -------------------------- */

    private static int defaultCorpusSize = 8192; /* expected number of distinct words among all documents */
    private int numThreads = Math.max(Runtime.getRuntime().availableProcessors() - 1, 1);


/* --------------------------
   Get/Set
 * -------------------------- */

    public int getDefaultCorpusSize() { return defaultCorpusSize; }
    public void setDefaultCorpusSize(int value) { defaultCorpusSize = value; }

    public int getNumThreads() { return numThreads; }
    public void setNumThreads(int value) { numThreads = value; }


/* --------------------------
   Constructor
 * -------------------------- */

    public TfIdfVectorizer() {

    }


/* --------------------------
   Public Methods
 * -------------------------- */

    /* Base method for training a tf-idf model */
    public Corpus fit(DocumentSet docSet, int minDf, double maxDfRatio) {

        Corpus corpus = docSet.getCorpus();

        corpus.filterValidTokens(minDf, maxDfRatio);
        corpus.calcIdf();
        corpus.setLock(true);

        return corpus;

    }

    /* Base method for generating a tf-idf matrix */
    public TfIdfMatrix transform(DocumentSet docSet, Corpus corpus) {

        if (!corpus.isIdfCalculated()) {
            corpus.calcIdf();
        }

        return this.getTfIdfMatrix(docSet, corpus);

    }

    /* Base method for fitting and transforming in one step */
    public TfIdfMatrix fitTransform(DocumentSet docSet, int minDf, double maxDfRatio) {

        return transform(docSet, this.fit(docSet, minDf, maxDfRatio));

    }

    /* This is an overloaded method for handling folder inputs */
    public Corpus fit(String inputFolder, int minDf, double maxDfRatio) {

        DocumentSet docSet = new DocumentSet();

        docSet.addFolder(inputFolder);

        /* Call 'base' method now that input is formatted */
        return this.fit(docSet, minDf, maxDfRatio);

    }

    /* This is an overloaded method for handling folder inputs */
    public TfIdfMatrix transform(String inputFolder, Corpus corpus) {

        DocumentSet docSet =
                new DocumentSet(DocumentSet.DocumentSetType.TRANSFORM, corpus);

        docSet.addFolder(inputFolder);

        /* Call 'base' method now that input is formatted */
        return this.transform(docSet, corpus);

    }

    /* This is an overloaded method for handling folder inputs */
    public TfIdfMatrix fitTransform(String inputFolder, int minDf, double maxDfRatio) {

        DocumentSet docSet = new DocumentSet();

        docSet.addFolder(inputFolder);

        /* Call 'base' method now that input is formatted */
        return transform(docSet,this.fit(docSet,minDf,maxDfRatio));

    }


/* --------------------------
   Transform
 * -------------------------- */

    /* Top-level method for returning a TfIdfMatrix object */
    private TfIdfMatrix getTfIdfMatrix(DocumentSet docSet,
                                       Corpus corpus) {

        /* This method requires two passes over the documents.  This first pass
           determines the subset of the global vocabulary that is present in
           the documents.  In this way, the tf-idf matrix need only be as large
           as numDocs x |subset of vocabulary|. */
        ConcurrentHashMap<Integer,Integer> presentTokenIndex =
                    getPresentTokenIndex(docSet, corpus);


        /* Assign row indices to each doc key */
        Map<String,Integer> docIndex = new HashMap<>(docSet.numDocs());

        int i = 0;

        for (SparseDoc doc : docSet)
            docIndex.put(doc.docName,i++);



        /* Initialize result matrix */
        int numDocs = docSet.numDocs();

        double[][] resultMatrix = new double[numDocs][presentTokenIndex.size()];


        /* This second pass calculates the tf-idf for each word, and writes
           it to the result matrix. */
        asyncCalcTfIdfAndWrite(docSet, docIndex, corpus, presentTokenIndex, resultMatrix);

        normalizeTfIdfMatrixRows(resultMatrix);


        /* Create an index by strings, for use by the user */
        Map<String,Integer> presentTokenStringIndex = new HashMap<>(presentTokenIndex.size());

        presentTokenIndex.entrySet().forEach(entry ->
                presentTokenStringIndex.put(corpus.getString(entry.getKey()), entry.getValue()));


        return new TfIdfMatrix(resultMatrix,presentTokenStringIndex,docIndex);
    }

    /* Returns a hashmap that is an index of the 'valid' words, which is the intersection of the
       fit and transform corpora. */
    private ConcurrentHashMap<Integer,Integer> getPresentTokenIndex(DocumentSet docSet,
                                                                    Corpus corpus) {

        ConcurrentHashMap<Integer,Integer> presentWordsIndex =
                    new ConcurrentHashMap<>(defaultCorpusSize);

        /* Iterate through documents.  If a word in the doc set is a 'valid' word in
           the corpus, then add it to the set of valid words. */

        int i = 0;
        for (SparseDoc doc : docSet) {
            for (TokenArrayElement token : doc) {
                if (!presentWordsIndex.containsKey(token.tokenId)) {
                    if (corpus.isValidTokenId(token.tokenId)) {
                        presentWordsIndex.put(token.tokenId,i++);
                    }
                }
            }
        }

        return presentWordsIndex;

    }

    /* This method asynchronously calls getTfIdfEntries() and then writeTfIdfEntriesToMatrix() */
    private void asyncCalcTfIdfAndWrite(DocumentSet docSet,
                                        Map<String, Integer> docIndex,
                                        Corpus corpus,
                                        ConcurrentHashMap<Integer, Integer> presentWordsListIndex,
                                        double[][] resultMatrix) {


        ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
        /* Pass a callable (as a lambda) to an ExecutorService for each document.  This callable
           calculates the entries for a particular document and writes these to a 2-d array
           in one pass.  */
        List<Future<?>> tasks = new ArrayList<>(docSet.numDocs());

        docSet.forEach(doc ->
                tasks.add(executorService.submit(() ->
                    writeTfIdfEntriesToMatrix(doc,corpus,presentWordsListIndex,
                                              docIndex.get(doc.docName),resultMatrix))));

        waitForTasksToComplete(tasks);

        safeExecutorServiceShutdown(executorService);
    }

    /* Writes the sparse (hash) representation of the non-zero entries to a matrix */
    private void writeTfIdfEntriesToMatrix(SparseDoc doc, Corpus corpus,
                                           ConcurrentHashMap<Integer, Integer> presentWordsListIndex,
                                           int row, double[][] resultMatrix) {

        doc.forEach(token -> {
            if (presentWordsListIndex.containsKey(token.tokenId)) {
                resultMatrix[row][presentWordsListIndex.get(token.tokenId)] =
                            corpus.getIdf(token.tokenId)*token.tokenCount;
            }
        });


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
    private void normalizeTfIdfMatrixRows(double[][] matrix) {

        int m = matrix.length;
        int n = matrix[0].length;

        double rs;
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
