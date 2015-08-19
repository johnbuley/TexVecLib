import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TfIdfVectorizer {

    public TfIdfVectorizer() {

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
                and cast to lower case.
             */
            doc = doc.replaceAll("[^a-zA-Z\\s]", " ").replaceAll("\\s+", " ").toLowerCase();

            listOfSplitDocs.add(Arrays.asList(doc.split(" ")));
        }

        return listOfSplitDocs;
    }
}
