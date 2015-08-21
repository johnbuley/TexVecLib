import Jama.*;

import javax.imageio.IIOException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UserTest {

    public static void main(String[] args) {

        /* This is a quick test from the perspective of the user, loading 4 works of literature
           and calling fitTransform.  Using the Jama library, the user can produce a text
           cosine similarity matrix from the result by multiplying it by its transpose
           (this works because TfIdfVectorizer returns a matrix where each row is a unit vector).
         */

        TfIdfVectorizer vec = new TfIdfVectorizer();

        List<String> documents = new ArrayList<>();
        try {
            documents.add(new String(Files.readAllBytes(Paths.get("./src/test/resources/twainhomer/huckfinn.txt"))));
            documents.add(new String(Files.readAllBytes(Paths.get("./src/test/resources/twainhomer/sawyer.txt"))));
            documents.add(new String(Files.readAllBytes(Paths.get("./src/test/resources/twainhomer/iliad.txt"))));
            documents.add(new String(Files.readAllBytes(Paths.get("./src/test/resources/twainhomer/odyssey.txt"))));
        }
        catch (IOException e) {
            System.err.println(e.toString());
        }

        TfIdfMatrix result = vec.fitTransform(documents,2,(float).8);

        Matrix matrix = new Matrix(result.matrix);

        Matrix similarity = matrix.times(matrix.transpose());

        List<String> names = Arrays.asList("Huck Finn","Sawyer   ","Iliad    ","Odyssey  ");

        DecimalFormat f = new DecimalFormat("#0.00");

        System.out.println();
        System.out.println("Similarity Score  [0,1]");
        System.out.println("          Huck Finn   Sawyer   Iliad   Odyssey");
        for(int i = 0; i < 4; i++) {
            System.out.printf(names.get(i));
            for (int j = 0; j < 4; j++) {
                System.out.printf("     " + f.format(similarity.get(i, j)));
            }
            System.out.println(System.getProperty("line.separator"));
        }
    }

}
