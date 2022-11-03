package gitlet;

import java.io.Serializable;
import java.util.List;
import java.util.LinkedList;
import java.util.Date;
import java.util.Map;
import java.util.HashMap;

/** Class that represents commits in gitlet.
 *  @author Frank Warren
 */
public class Commit implements Serializable {
    /** Commit constructor creates a commit with message MESSAGE from PARENT. */
    Commit(String message, String parent) {
        if (message.isBlank()) {
            throw new GitletException("Please enter a commit message.");
        }
        _message = message;
        _parent = parent;
        Date date;
        if (parent == null) {
            date = new Date(0);
            _blobs = new HashMap<>(0);
            _ancestors = new LinkedList<>();
        } else {
            _ancestors =
                    Utils.readObject(
                            Main.COMMITS, parent, Commit.class).getAncestors();
            _ancestors.addFirst(parent);
            date = new Date();
            _blobs =
                Utils.readObject(Main.COMMITS, parent, Commit.class).getBlobs();
            List<String> addFiles = Utils.plainFilenamesIn(Main.ADD);
            List<String> removeFiles = Utils.plainFilenamesIn(Main.REMOVE);
            if (addFiles.size() == 0 && removeFiles.size() == 0) {
                throw new GitletException("No changes added to the commit.");
            }
            for (String file : addFiles) {
                Blob blob = new Blob(file);
                blob.save();
                _blobs.put(file, blob.toString());
                Utils.join(Main.ADD, file).delete();
            }
            for (String file : removeFiles) {
                _blobs.remove(file);
                Utils.join(Main.REMOVE, file).delete();
            }
        }
        _timestamp = String.format("%1$ta %1$tb %1$td "
                        + "%1$tH:%1$tM:%1$tS %1$tY %1$tz", date);
        _id = Utils.sha1("commit ", Utils.serialize(this));
    }

    /** Constructor for initial commit. */
    Commit() {
        this("initial commit", null);
    }

    /** Saves this commit in /commits. */
    void save() {
        Utils.writeObject(Main.COMMITS, _id, this);
    }

    /** Returns _message. */
    String getMessage() {
        return _message;
    }

    /** Returns _timestamp. */
    String getTimestamp() {
        return _timestamp;
    }

    /** Returns _parent. */
    String getParent() {
        return _parent;
    }

    /** Returns _secondParent. */
    String getSecondParent() {
        return _secondParent;
    }

    /** Returns _blobs. */
    Map<String, String> getBlobs() {
        return _blobs;
    }

    /** Returns _ancestors. */
    LinkedList<String> getAncestors() {
        return _ancestors;
    }

    @Override
    public String toString() {
        return _id;
    }

    /** Commit metadata. */
    private String _id, _message, _timestamp, _parent, _secondParent;

    /** Mapping of SHA-1 filepath KEYS to blob SHA-1 ID. */
    private Map<String, String> _blobs;

    /** This commit's ancestors. */
    private LinkedList<String> _ancestors;
}
