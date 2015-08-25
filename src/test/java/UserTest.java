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

        long startTime = System.nanoTime();

        TfIdfMatrix result = vec.fitTransform("./src/test/resources/twainhomer/", 2, .8);

        long endTime = System.nanoTime();

        Matrix matrix = new Matrix(result.matrix);

        /* Get similarity matrix */
        Matrix similarity = matrix.times(matrix.transpose());


        /* Print output. */
        DecimalFormat f = new DecimalFormat("#0.00");

        System.out.println("fitTransform time : " + f.format((double)(endTime - startTime)/1000000.) + " ms");
        System.out.println();
        System.out.println("Similarity Score  [0,1]");
        System.out.printf("                  ");
        for(int i = 0; i < 4; i++) {
            System.out.printf(result.getDocAt(i) + "     ");
        }
        System.out.println();
        for(int i = 0; i < 4; i++) {
            System.out.printf(result.getDocAt(i));
            for (int j = 0; j < 4; j++) {
                System.out.printf("           " + f.format(similarity.get(i, j)));
            }
            System.out.println(System.getProperty("line.separator"));
        }
    }

}
