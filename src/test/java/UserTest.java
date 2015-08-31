import Jama.*;

import java.text.DecimalFormat;

public class UserTest {

    public static void main(String[] args) {

        /* This is a quick test from the perspective of the user, loading 4 works of literature
           and calling fitTransform.  Using the Jama library, the user can produce a text
           cosine similarity matrix from the result by multiplying it by its transpose
           (this works because TfIdfVectorizer returns a matrix where each row is a unit vector).
         */

        TfIdfVectorizer vec = new TfIdfVectorizer();

/* -----------------------------
   One-Step Example
   -----------------------------  */

        TfIdfMatrix result = vec.fitTransform("./src/test/resources/twainhomer/", 2, .8);

        Matrix matrix = new Matrix(result.matrix);

        /* Get similarity matrix */
        Matrix similarity = matrix.times(matrix.transpose());

        System.out.println();
        System.out.println("one-step example:");
        System.out.println();
        printResult(result, similarity);


/* -----------------------------
   Two-Step Example
   -----------------------------  */

        DocumentSet fitDocSet = new DocumentSet();

        fitDocSet.addFile("odyssey", "./src/test/resources/twainhomer/odyssey.txt");
        fitDocSet.addFile("sawyer","./src/test/resources/twainhomer/sawyer.txt");

        Corpus corpus = vec.fit(fitDocSet,1,1.0);

        DocumentSet transformDocSet = new DocumentSet(DocumentSet.DocumentSetType.TRANSFORM,corpus);

        transformDocSet.addFile("iliad", "./src/test/resources/twainhomer/iliad.txt");
        transformDocSet.addFile("huckfinn", "./src/test/resources/twainhomer/huckfinn.txt");

        result = vec.transform(transformDocSet,corpus);

        matrix = new Matrix(result.matrix);

        similarity = matrix.times(matrix.transpose());

        System.out.println();
        System.out.println("two-step example:");
        System.out.println("different parameters are used, so different result is expected");
        System.out.println();
        printResult(result,similarity);

    }

    private static void printResult(TfIdfMatrix result, Matrix similarity) {

        /* Print output. */
        DecimalFormat f = new DecimalFormat("#0.00");

        System.out.println();
        System.out.println("Similarity Score  [0,1]");
        System.out.printf("                  ");
        for(int i = 0; i < result.numDocs(); i++) {
            System.out.printf(result.getDocAt(i) + "     ");
        }
        System.out.println();
        for(int i = 0; i < result.numDocs(); i++) {
            System.out.printf(result.getDocAt(i));
            for (int j = 0; j < result.numDocs(); j++) {
                System.out.printf("           " + f.format(similarity.get(i, j)));
            }
            System.out.println(System.getProperty("line.separator"));
        }

    }

}
