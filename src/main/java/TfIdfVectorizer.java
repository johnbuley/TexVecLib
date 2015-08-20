import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TfIdfVectorizer {

    private boolean initialized;
    private ConcurrentHashMap<String,IdfWord> idfHash;

    public TfIdfVectorizer() {
        initialized = false;
        idfHash = new ConcurrentHashMap<>();
    }

    public void fit(List<List<String>> input, int minDf, float maxDfRatio) {

        this.idfHash = this.getNewIdfWordHash(input, minDf, maxDfRatio);
        this.initialized = true;

    }

    public TfIdfMatrix transform(List<List<String>> input) {

        if (initialized)
            return this.getTfIdfMatrix(input,this.idfHash);
        else {
            System.err.println("TfIdfVectorizer object not initialized with fit().");
            return null;
        }

    }

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

    private List<List<String>> splitDocuments(List<String> listOfDocs) {

        List<List<String>> listOfSplitDocs = new ArrayList<>();

        for (String doc : listOfDocs) {

            /*  Remove non-alphanumeric and non-whitespace characters,
                replace two or more spaces with one space,
                and cast to lower case.*/
            doc = doc.replaceAll("[^a-zA-Z\\s]", " ").replaceAll("\\s+", " ").toLowerCase();
            listOfSplitDocs.add(Arrays.asList(doc.split(" ")));

        }

        return listOfSplitDocs;
    }

    private ConcurrentHashMap<String, IdfWord> getNewIdfWordHash(List<List<String>> listOfDocuments, int minDf, float maxDfRatio) {

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


        /* Filter the result of the previous line according to max- and min-allowable
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

    private TfIdfMatrix getTfIdfMatrix(List<List<String>> listOfDocuments, ConcurrentHashMap<String, IdfWord> idfHash) {


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
        for (int i = 0; i < numDocs; i++) {

            Map<String,Double> tfIdfEntries = getTfIdfEntries(listOfDocuments.get(i),idfHash);

            writeTfIdfEntriesToMatrix(tfIdfEntries,presentWordsListIndex,i,resultMatrix);

        }

        return new TfIdfMatrix(resultMatrix,presentWordsList);
    }

    private void writeTfIdfEntriesToMatrix(Map<String,Double> tfIdfEntries,
                                           ConcurrentHashMap<String,Integer> presentWordsListIndex,
                                           int row,double[][] resultMatrix) {

        for(Map.Entry e : tfIdfEntries.entrySet()) {
            resultMatrix[row][presentWordsListIndex.get(e.getKey())] = (double)e.getValue();
        }

    }

    private Map<String,Double> getTfIdfEntries(List<String> document, ConcurrentHashMap<String,IdfWord> idfHash) {

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
}
