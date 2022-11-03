package gitlet;

import java.io.Serializable;

/** Blobs represent files.
 * @author Frank Warren
 */
public class Blob implements Serializable {
    /** Blob constructor creates a blob that represents FILE. */
    public Blob(String file) {
        _contents = Utils.readContents(Utils.join(Main.CWD, file));
        _file = file;
        _id = Utils.sha1("blob ", Utils.serialize(this));
    }

    /** Saves this blob to .gitlet/blobs. */
    void save() {
        Utils.writeObject(Main.BLOBS, _id, this);
    }

    /** Returns _contents. */
    byte[] getContents() {
        return _contents;
    }

    /** Returns the name of the file that this blob represents. */
    public String toString() {
        return _id;
    }

    /** The serialized contents of the file this blob represents. */
    private byte[] _contents;
    /** Blob metadata. */
    private String _id, _file;
}
