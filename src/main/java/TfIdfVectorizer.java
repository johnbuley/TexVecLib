import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TfIdfVectorizer {

    private boolean initialized;
    private ConcurrentHashMap<String,IdfWord> idfHash;

/* --------------------------
   Constructor
 * -------------------------- */

    public TfIdfVectorizer() {
        initialized = false;
        idfHash = new ConcurrentHashMap<>();
    }

/* --------------------------
   Public Methods
 * -------------------------- */

    /* Base method for fitting the model */
    public void fit(List<String> input, int minDf, float maxDfRatio) {

        this.idfHash = this.getNewIdfWordHash(this.splitDocuments(input), minDf, maxDfRatio);
        this.initialized = true;

    }

    /* Base method for generating a tf-idf matrix */
    public TfIdfMatrix transform(List<String> input) {

        if (initialized)
            return this.getTfIdfMatrix(this.splitDocuments(input),this.idfHash);
        else {
            System.err.println("TfIdfVectorizer object not initialized with fit().");
            return null;
        }

    }

    /* Base method for fitting and transforming in one step */
    public TfIdfMatrix fitTransform(List<String> input, int minDf, float maxDfRatio) {

        this.fit(input,minDf,maxDfRatio);
        return transform(input);

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

        /* Call 'base' method now that input is formatted */
        return fitTransform(this.processInputFolder(inputFolder), minDf, maxDfRatio);

    }

/* --------------------------
   Private Methods
 * -------------------------- */

/* Input Processing

    /* Finds .txt files in a given folder and collects them into a list of strings */
    private List<String> processInputFolder(String folderName) {

        List<String> listOfDocuments = new ArrayList<>();
        Path inputPath = Paths.get(folderName);

        if (Files.exists(inputPath)) {
            /* This block is a clear use case for a closure, but there is no way
               to forward the IOException.
             */
            try {
                DirectoryStream<Path> stream = Files.newDirectoryStream(inputPath, "*.txt");

                /*  Iterate through files in input folder, transform each to string,
                *   and add each to list of document strings. */
                for (Path filePath : stream) {
                    try {
                        listOfDocuments.add(new String(Files.readAllBytes(filePath)));
                    }
                    catch (Exception e) {
                        System.out.println("Error reading file: " + filePath.toString());
                    }
                }
            }
            catch (IOException e) {
                System.out.println("Error reading input folder" + e.toString());
            }
        }

        return listOfDocuments;
    }

    /* Tokenizes the documents and collects each into a list of strings */
    private List<List<String>> splitDocuments(List<String> listOfDocs) {

        /*  Remove non-alphanumeric and non-whitespace characters,
            replace two or more spaces with one space,
            and cast to lower case.*/
        return listOfDocs.stream()
                         .map(d -> d.replaceAll("[^a-zA-Z\\s]", " ")
                                 .replaceAll("\\s+", " ")
                                 .toLowerCase())
                         .map(d -> Arrays.asList(d.split(" ")))
                         .collect(Collectors.toList());

    }

 /* Idf Calculation
 /* --- getNewIdfWordHash() should be extracted, there's a lot going on in there.

    /* Calculates the idf for each word present in the corpus and stores in a thread-safe hashmap.
       This constitutes the training step. The ConcurrentHashMap supports non-locking reads,
       which is essential for asyncCalcTfIdfAndWrite()  */
    /* idf(w) = log(|documents| * |documents in which w appears|) */
    private ConcurrentHashMap<String, IdfWord> getNewIdfWordHash(List<List<String>> listOfDocuments,
                                                                 int minDf, float maxDfRatio) {

        List<HashSet<String>> listOfDocDistinctWordSets = new ArrayList<>();
        listOfDocuments.forEach(d -> listOfDocDistinctWordSets.add(new HashSet<>(d)));


        /* Use a flatmap to find the number of documents in which each word occurs
           at least once.
         */
        Map<String,Integer> allWordCounts =
                listOfDocDistinctWordSets.stream()
                                         .flatMap(Collection::stream)
                                         .collect(Collectors.groupingBy(Function.identity(),
                                                 Collectors.summingInt(e -> 1)));


        int docCount = listOfDocuments.size();
        /* Calculate max-allowable document frequency based on maxDfRatio and number of docs */
        int maxDf = (int)Math.floor(maxDfRatio*docCount);


        /* Filter the result of the previous section according to max- and min-allowable
           document frequency, then collect into a map where the value is the idf.
         */
        Map<String, Double> wordCounts =
                allWordCounts.entrySet().stream()
                                        .filter(e -> (e.getValue() >= minDf && e.getValue() <= maxDf))
                                        .collect(Collectors.toMap(Map.Entry::getKey,
                                                                  e -> Math.log((float) docCount / e.getValue())));


        ConcurrentHashMap<String, IdfWord> idfWordHash = new ConcurrentHashMap<>();
        int i = 0;
        /* Copy elements of wordCounts to a ConcurrentHashMap, along with a
           unique id that will be used to index the final tf-idf matrix.
         */
        for(Map.Entry e : wordCounts.entrySet()) {
            idfWordHash.put((String)e.getKey(),new IdfWord(i++,(double)e.getValue()));
        }

        return idfWordHash;
    }


/* Tf-idf Calculation

    /* This is the workhorse of the transformation step, returning the tf-idf matrix based
       on the corpus. */
    private TfIdfMatrix getTfIdfMatrix(List<List<String>> listOfDocuments,
                                       ConcurrentHashMap<String, IdfWord> idfHash) {


        /* This method requires two passes over the documents.  This first pass
           determines the subset of the global vocabulary that is present in
           the documents.  In this way, the tf-idf matrix need only be as large
           as numDocs x |subset of vocabulary|.
         */
        List<String> presentWordsList = getPresentWordsList(listOfDocuments, idfHash);

        ConcurrentHashMap<String,Integer> presentWordsListIndex = getPresentWordsIndex(presentWordsList);


        int numDocs = listOfDocuments.size();
        double[][] resultMatrix = new double[numDocs][presentWordsList.size()];


        /* This second pass calculates the tf-idf for each word, and writes
           it to the result matrix.
        */
        asyncCalcTfIdfAndWrite(listOfDocuments,idfHash,presentWordsListIndex,resultMatrix);

        normalizeTfIdfMatrixRowWise(resultMatrix);

        return new TfIdfMatrix(resultMatrix,presentWordsList);
    }



    /* This method asynchronously calls getTfIdfEntries() and then writeTfIdfEntriesToMatrix() */
    private void asyncCalcTfIdfAndWrite(List<List<String>> listOfDocuments, ConcurrentHashMap<String,IdfWord> idfHash,
                                        ConcurrentHashMap<String,Integer> presentWordsListIndex, double[][] resultMatrix) {


        /* This method uses an ExecutorService to produce a collection of
           futures, and then a stream on those futures to determine if the
           tasks are completed.
        */
        ExecutorService executorService = Executors.newFixedThreadPool(4);

        /* This is an overly-convoluted way of generating a list of integers */
        List<Integer> numDocRange =
                IntStream.iterate(0, n -> n + 1)
                         .limit(listOfDocuments.size())
                         .boxed()
                         .collect(Collectors.toList());

        /* Passes a callable (as a lambda) to an ExecutorService for each document.  This callable
           calculates the non-zero entries for a particular document, and then writes these to a
           2-d array.  */
        List<Future<?>> tasks = numDocRange.stream()
                                           .map(i ->
                                                   executorService.submit(() ->
                                                           writeTfIdfEntriesToMatrix(
                                                                   getTfIdfEntries(listOfDocuments.get(i), idfHash),
                                                                   presentWordsListIndex, i,
                                                                   resultMatrix)))
                                           .collect(Collectors.toList());

        waitForTasksToComplete(tasks);

        executorService.shutdownNow();
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



    /* Returns a sparse (hash) representation of the non-zero entries in the
       tf-idf matrix for one document.
       tf-idf(word w in doc d) = |w occurrences in d| * idf(w) */
    private Map<String,Double> getTfIdfEntries(List<String> document,
                                               ConcurrentHashMap<String,IdfWord> idfHash) {

            Map<String,Integer> wordCounts =
                    document.stream()
                            .collect(Collectors.groupingBy(Function.identity(), Collectors.summingInt(e -> 1)));

            /* Filter for words present in global vocab, calculate tf-idf, and collect */
            return wordCounts.entrySet().stream()
                                        .filter(e -> idfHash.containsKey(e.getKey()))
                                        .map(e ->
                                                new Object() {
                                                    String key = e.getKey();
                                                    Double value = e.getValue() * idfHash.get(e.getKey()).idf;
                                                })
                                        .collect(Collectors.toMap(e -> e.key, e -> e.value));

    }

    /* Writes the sparse (hash) representation of the non-zero entries to a matrix */
    private void writeTfIdfEntriesToMatrix(Map<String,Double> tfIdfEntries,
                                           ConcurrentHashMap<String,Integer> presentWordsListIndex,
                                           int row,double[][] resultMatrix) {

        for(Map.Entry e : tfIdfEntries.entrySet()) {
            resultMatrix[row][presentWordsListIndex.get(e.getKey())] = (double)e.getValue();
        }

    }

    private List<String> getPresentWordsList(List<List<String>> listOfDocuments, ConcurrentHashMap<String,IdfWord> idfHash) {

        return listOfDocuments.stream()
                              .flatMap(Collection::stream)
                              .filter(e -> idfHash.containsKey(e))
                              .distinct()
                              .collect(Collectors.toList());

    }

    private ConcurrentHashMap<String,Integer> getPresentWordsIndex(List<String> presentWordsList) {

        ConcurrentHashMap<String,Integer> presentWordsListIndex = new ConcurrentHashMap<>();

        int c = 0;
        for(String word : presentWordsList) {
            presentWordsListIndex.put(word,c++);
        }

        return presentWordsListIndex;

    }



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
            System.out.println("Row sum: " + rs);
        }

    }

}
