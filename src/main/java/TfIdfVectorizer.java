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

    boolean initialized;
    Map<String,Integer> tfHash;
    Map<String,Integer> idfHash;

    public TfIdfVectorizer() {
        initialized = false;
        tfHash = new ConcurrentHashMap<String,Integer>();
        idfHash = new ConcurrentHashMap<String,Integer>();
    }

    private List<String> processInputFolder(String folderName) {

        List<String> listOfDocuments = new ArrayList<String>();
        Path inputPath = Paths.get(folderName);

        if (Files.exists(inputPath)) {
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

        List<List<String>> listOfSplitDocs = new ArrayList<List<String>>();

        for (String doc : listOfDocs) {

            /*  Remove non-alphanumeric and non-whitespace characters,
                replace two or more spaces with one space,
                and cast to lower case.*/

            doc = doc.replaceAll("[^a-zA-Z\\s]", " ").replaceAll("\\s+", " ").toLowerCase();
            listOfSplitDocs.add(Arrays.asList(doc.split(" ")));

        }

        return listOfSplitDocs;
    }

    private ConcurrentHashMap<String,idfWord> getNewIdfWordHash(List<List<String>> listOfDocuments, int minDf, float maxDfRatio) {

        List<HashSet<String>> listOfDocDistinctWordSets = new ArrayList<HashSet<String>>();

        listOfDocuments.forEach(d -> listOfDocDistinctWordSets.add(new HashSet<String>(d)));


        /* Use a flatmap to find the number of documents in which each word occurs
           at least once.
         */
        Map<String,Integer> allWordCounts =
                listOfDocDistinctWordSets.stream()
                    .flatMap(Collection::stream)
                    .collect(Collectors.groupingBy(Function.identity(), Collectors.summingInt(e -> 1)));


        int docCount = listOfDocuments.size();

        /* Calculate max-allowable document frequency based on maxDfRatio and number of docs */
        int maxDf = (int)Math.floor(maxDfRatio*docCount);


        /* Filter the result of the previous line according to max- and min-allowable
           document frequency, then collect into a map where the value is the idf.
         */
        Map<String, Double> wordCounts =
                allWordCounts.entrySet().stream()
                        .filter(e -> (e.getValue() >= minDf && e.getValue() <= maxDf))
                        .collect(Collectors.toMap(e -> e.getKey(), e -> Math.log((float) docCount / e.getValue())));


        ConcurrentHashMap<String, idfWord> idfWordHash = new ConcurrentHashMap<>();
        int i = 0;
        /* Copy elements of wordCounts to a ConcurrentHashMap, along with a
           unique id that will be used to index the final tf-idf matrix.
         */
        for(Map.Entry e : wordCounts.entrySet()) {
            idfWordHash.put((String)e.getKey(),new idfWord(i++,(double)e.getValue()));
        }

        return idfWordHash;
    }
}
