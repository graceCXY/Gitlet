package gitlet;
import java.io.File;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Date;


/** Data structure for storing commits for Gitlet,
 * the tiny stupid version-control system.
 *  @author Grace Chen
 */
public class Commit implements Serializable {

    /** Constructors for Commit objects.
     * @param logMessage the input log message.
     * @param parent1 the primary parent. */
    public Commit(String logMessage, String parent1) {
        _logMessage = logMessage;
        _timestamp = getFormattedTime(getCurrentTime());
        _parentReference = parent1;
        _parentReference2 = null;
        _blobs = new HashMap<>();
        copyBlobs(parent1);
    }

    /** Constructor in terms of a merge.
     * @param logMessage the input log message.
     * @param parent1 the first parent.
     * @param parent2 the second parent. */
    public Commit(String logMessage, String parent1, String parent2) {
        _logMessage = logMessage;
        _timestamp = getFormattedTime(getCurrentTime());
        _parentReference = parent1;
        _parentReference2 = parent2;
        _blobs = new HashMap<>();
        copyBlobs(parent1);
    }

    /** Default constructor for Commit objects.
     * Used for the init command. */
    public Commit() {
        _logMessage = "initial commit";
        _blobs = null;
        _timestamp = getFormattedTime(unixEpoch);
        _parentReference = null;
        _parentReference2 = null;
    }

    /** used by the constructor.
     * by default copies files from parent commit into current.
     * @param parent the parent commit we want to get files from. */
    private void copyBlobs(String parent) {
        File parentFile = new File(".gitlet/commits/" + parent);
        if (parentFile.exists()) {
            Commit parentCommit = Utils.readObject(parentFile, Commit.class);
            HashMap<String, String> from = parentCommit.getBlobs();
            if (from != null) {
                for (String blobName : from.keySet()) {
                    _blobs.put(blobName, from.get(blobName));
                }
            }
        } else {
            System.out.println("Parent doesn't exists.");
        }
    }

    /** accessor method for the log message of current commit.
     * @return the log message of current commit. */
    public String getLogMessage() {
        return _logMessage;
    }

    /** accessor method for the timestamp of current commit.
     * @return the timestamp of current commit. */
    public String getTimeStamp() {
        return _timestamp;
    }

    /** accessor for the parent reference of current commit.
     * @return the primary parent of current commit. */
    public String getParent() {
        return _parentReference;
    }

    /** accessor for the second parent reference of current commit.
     * @return the second parent of current commit. */
    public String getParent2() {
        return _parentReference2;
    }

    /** accessor for blobs that this commit tracks.
     * @return the files tracked by current commit. */
    public HashMap<String, String> getBlobs() {
        return _blobs;
    }

    /** add a blob to the blobs that this commit tracks to.
     * @param blobName the file name we want to add.
     * @param blobID the file content we want to add. */
    public void addBlob(String blobName, String blobID) {
        _blobs.put(blobName, blobID);
    }

    /** remove the this file from the tracking list.
     * @param blobName the file we want to remove. */
    public void removeBlob(String blobName) {
        _blobs.remove(blobName);
    }

    /** replace the content of a blob with another.
     * @param blobName the file name input.
     * @param blobID the content we want to replace. */
    public void replaceBlobContent(String blobName, String blobID) {
        _blobs.replace(blobName, blobID);
    }

    /** check to see if the current commit contains a certain file.
     * @param blobName the file name input.
     * @return whether the file is tracked by the current commit. */
    public boolean containsBlob(String blobName) {
        return _blobs != null && _blobs.containsKey(blobName);
    }

    /** find the blobID with the current filename in _blobs.
     * @param blobName the file name we want to find.
     * @return the blob ID associated with that name in the current commit. */
    public String findBlob(String blobName) {
        if (!containsBlob(blobName)) {
            return "";
        }
        return _blobs.get(blobName);
    }

    /** put Date in correct format for time log.
     * @param d the input date we want to format.
     * @return the formatted String of time. */
    public String getFormattedTime(Date d) {
        SimpleDateFormat formatter =
                new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z");
        return formatter.format(d);
    }

    /** get the current time in current time zone.
     * @return the current time. */
    private Date getCurrentTime() {
        return new Date();
    }

    /** accessor method for unixEpoch.
     * @return the initial time. */
    private Date getUnixEpoch() {
        return unixEpoch;
    }

    /** log Message of a commit. */
    private String _logMessage;

    /** timestamp of a commit. */
    private String _timestamp;

    /** mapping of fileNames. */
    private HashMap<String, String> _blobs;

    /** the parent reference of current commit object. */
    private String _parentReference;

    /** the second parent reference in case of a merge. */
    private String _parentReference2;

    /** the beginning of time for computers. */
    private Date unixEpoch = new Date(0);
}
