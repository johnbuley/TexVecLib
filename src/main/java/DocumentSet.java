import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class DocumentSet implements Iterable {

    public enum DocumentSetType {
        FIT, TRANSFORM
    }

    public Map<String,Integer> docIndex;
    public List<SparseDoc> docs;
    public Corpus corpus;

    private int maxStringLength = 64;
    private int ioBufferSize = 2048;
    private int defaultDocSize = 1024;

    public DocumentSet(DocumentSetType type) {

        this.docIndex = new HashMap<>();
        this.docs = new ArrayList<>();
        this.corpus = new Corpus();

        setCorpusOptions(this.corpus,type);

    }

    public DocumentSet(DocumentSetType type, Corpus corpus) {

        this.docIndex = new HashMap<>();
        this.docs = new ArrayList<>();
        this.corpus = corpus;

        setCorpusOptions(this.corpus,type);

    }

    public Corpus getCorpus() { return this.corpus; }

/* -----------------------------
   Public Methods
   -----------------------------  */

    public void addDoc(String docName, Path filePath) throws Exception {

        this.addSparseDoc(docName,this.sparsifyDoc(filePath.getFileName().toString(),filePath));

    }

    public void addFolder(String inputFolder) throws Exception {

        this.processInputFolder(Paths.get(inputFolder));

    }

    @Override
    public Iterator<SparseDoc> iterator() {

        return docs.iterator();

    }


/* -----------------------------
   Private Methods
   -----------------------------  */

/*    private void updateWordCounts(SparseDoc doc, Corpus corpus) {

        for(int[] result: ((SparseDoc)doc)) {

            corpus.updateDocOccurrence(result[0]);

        }

    }*/

    private void setCorpusOptions(Corpus corpus, DocumentSetType type) {

        if (type == DocumentSetType.FIT) {
            corpus.setLock(false);
            corpus.setCalcIdf(true);
        }
        else if (type == DocumentSetType.TRANSFORM) {
            corpus.setLock(true);
            corpus.setCalcIdf(false);
        }

    }

    /* Finds .txt files in a given folder and collects them into a list of strings */
    private void processInputFolder(Path inputPath) {

        if (Files.exists(inputPath)) {
            try {
                DirectoryStream<Path> stream = Files.newDirectoryStream(inputPath, "*.txt");
                /*  Iterate through files in input folder, transform each to string,
                *   and add each to list of document strings. */
                for (Path filePath : stream) {
                    try {
                        SparseDoc doc = this.sparsifyDoc(filePath.getFileName().toString(), filePath);
                        //this.updateWordCounts(doc,corpus);
                        this.corpus.addDoc(doc);
                        this.addSparseDoc(filePath.getFileName().toString(), doc);
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
        }
    }

    /* Adds a SparseDocHash to collection, returns assigned index */
    private int addSparseDoc(String docName, SparseDoc doc) {

        docs.add(doc);
        docIndex.put(docName,docs.size()-1);

        /* Return index assigned to document */
        return docs.size()-1;

    }

    private SparseDoc sparsifyDoc(String docName, Path filePath) throws Exception {

        SparseDoc doc = SparseDoc.getSparseDoc(docName,8);

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

        return doc;

    }

    private void addWordToDoc(SparseDoc doc, byte[] concatBuf, IntegerPtr bufPtr) {

        String word = new String(concatBuf, 0, bufPtr.value);
        bufPtr.value = 0;

        int tokenId = this.corpus.getId(word);
        if (tokenId != -1) {
            doc.addToken(tokenId);
        }

    }



}
