import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class DocumentSet implements Iterable<SparseDoc> {

/* -----------------------------
   Enumerator
   -----------------------------  */

    public enum DocumentSetType {
        FIT, TRANSFORM
    }


/* -----------------------------
   Fields
   -----------------------------  */

    private final Map<String,Integer> docIndex;
    private final List<SparseDoc> docs;
    private final CorpusMetadata corpusMetadata;
    private int numDocs;


/* -----------------------------
   Defaults
   -----------------------------  */

    private int maxStringLength = 64; /* chars */
    private int ioBufferSize = 4096; /* bytes */
    private int defaultDocSize = 4096; /* expected number of distinct words in a document */


/* -----------------------------
   Constructors
   -----------------------------  */

    public DocumentSet() {

        this.docIndex = new HashMap<>();
        this.docs = new ArrayList<>();
        this.corpusMetadata = new CorpusMetadata();
        this.numDocs = 0;

        /* FIT is the default type for a new DocumentSet */
        setCorpusOptions(this.corpusMetadata,DocumentSetType.FIT);

    }

    public DocumentSet(DocumentSetType type, CorpusMetadata corpusMetadata) {

        this.docIndex = new HashMap<>();
        this.docs = new ArrayList<>();
        this.corpusMetadata = corpusMetadata;
        this.numDocs = 0;

        setCorpusOptions(this.corpusMetadata,type);

    }


/* -----------------------------
   Get/Set
   -----------------------------  */

    public int getMaxStringLength() { return maxStringLength; }
    public void setMaxStringLength(int value) { maxStringLength = value; }

    public int getIoBufferSize() { return ioBufferSize; }
    public void setIoBufferSize(int value) { ioBufferSize = value; }

    public int getDefaultDocSize() { return defaultDocSize; }
    public void setDefaultDocSize(int value) { defaultDocSize = value; }

    public CorpusMetadata getCorpusMetadata() { return this.corpusMetadata; }

    public int numDocs() { return this.numDocs; }

    public SparseDoc getDoc(int index) {

        if (index < this.docs.size())
            return this.docs.get(index);
        else
            return null;

    }


/* -----------------------------
   Public Methods
   -----------------------------  */

    /* Overloaded public method for adding one file */
    public void addFile(String docName, String pathString) {

        this.addFile(docName,Paths.get(pathString));

    }

    /* 'base' method for adding one file */
    public void addFile(String docName, Path filePath) {

        try {
            SparseDoc doc = this.sparsifyDoc(filePath.getFileName().toString(), filePath);
            this.corpusMetadata.addDoc(doc);
            doc.compress();
            this.addSparseDoc(docName, this.sparsifyDoc(docName, filePath));
        }
        catch (Exception e) { System.err.println(e.toString()); }
    }

    /* Public method for adding a folder of documents */
    public void addFolder(String inputFolder) {

        try {
            this.processInputFolder(Paths.get(inputFolder));
        }
        catch (Exception e) { System.err.println(e.toString()); }

    }

    @Override
    public Iterator<SparseDoc> iterator() {

        return docs.iterator();

    }


/* -----------------------------
   Private Methods
   -----------------------------  */

    /* I see two alternative ways to control the interaction between a DocumentSet
       and its CorpusMetadata.  Inheritance doesn't necessarily address the issue, because
       the interaction can change over the life of the object.  I could expect
       the consumer of the library to set these parameters manually, but that's
       not very clean and prone to error.  So DocumentSetType allows for the definition
       of parameter templates that can be used to control locking, compression, and
       other behavior. */
    private void setCorpusOptions(CorpusMetadata corpusMetadata, DocumentSetType type) {

        if (type == DocumentSetType.FIT) {
            corpusMetadata.setLock(false);
        }
        else if (type == DocumentSetType.TRANSFORM) {
            corpusMetadata.setLock(true);
        }

    }

    /* Finds .txt files in a given folder and adds them to the DocumentSet */
    private void processInputFolder(Path inputPath) {

        if (Files.exists(inputPath)) {
            try {
                DirectoryStream<Path> stream = Files.newDirectoryStream(inputPath, "*.txt");
                /*  Iterate through files in input folder, transform each to string,
                *   and add each to list of document strings. */
                for (Path filePath : stream) {
                    try {
                        this.addFile(filePath.getFileName().toString(), filePath);
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

    /* Adds a SparseDoc to collection, returns assigned index */
    private int addSparseDoc(String docName, SparseDoc doc) {

        docs.add(doc);
        docIndex.put(docName, docs.size() - 1);
        this.numDocs++;

        /* Return index assigned to document */
        return docs.size()-1;

    }

    /* Builds a SparseDoc object from a raw document */
    private SparseDoc sparsifyDoc(String docName, Path filePath) throws Exception {

        /* At the moment, this is hard-coded to instantiate a 3-byte-element
           SparseDoc.  The ideal implementation is to start with 2-byte-element arrays,
           then when I reach 256^2 unique global tokens ids, restart that document with
           3-byte-element arrays, and then use 3-byte-element from then on.  That
           allows for maximum compression with only a small performance cost. */
        SparseDoc doc = new SparseDoc(docName,defaultDocSize,3);

        ByteBuffer buf = ByteBuffer.allocate(ioBufferSize);
        byte[] concatBuf = new byte[maxStringLength];

        /* There seems to be a debate over the performance of synchronous NIO vs IO.  I worked
           with NIO because I was experimenting with parallelizing ingestion. */
        FileChannel fileChannel = (new FileInputStream(filePath.toString())).getChannel();

        int bytesRead;
        /* The integer pointer is only necessary to allow the buffer pointer to
           be reset within addTokenToDoc.  If this behavior were done in-line, then
           an int would be used. */
        IntegerPtr bufPtr = new IntegerPtr(0);
        /* A performance boost could be gained from generating a string
           directly from the FileChannel buffer, rather than a separate one,
           but that would be lossy in cases where a token spanned two separate
           FileChannel reads.  In this case, a token is only lost if it
           exceeds the size of the concatenation buffer.
           A small improvement I thought of was the following: if I'm processing
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
                        addTokenToDoc(doc, concatBuf, bufPtr);
                    }
                }
                /* Check that string concat buffer cannot overflow on next iteration.
                   If so, write the current contents of the buffer as a token and reset. */
                if (bufPtr.value == maxStringLength-1) {
                    addTokenToDoc(doc, concatBuf, bufPtr);
                }
            }

            buf.clear();
        }

        return doc;

    }

    /* Adds token to SparseDoc and resets the concat buffer */
    private void addTokenToDoc(SparseDoc doc, byte[] concatBuf, IntegerPtr bufPtr) {

        String word = new String(concatBuf, 0, bufPtr.value);
        bufPtr.value = 0;

        int tokenId = this.corpusMetadata.getId(word);
        if (tokenId != -1) {
            doc.addToken(tokenId);
        }
    }

    /* Used by sparsifyDoc() and addTokenToDoc() */
    private class IntegerPtr {

        public int value;

        public IntegerPtr(int value) {
            this.value = value;
        }

    }

}
