package gitlet;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;

/** The staging area
 * Gitlet, the tiny stupid version-control system.
 *  @author Grace Chen
 */
public class StagingArea implements Serializable {

    /** constructor. */
    public StagingArea() {
        stagingAdd = new HashMap<>();
        stagingRm = new HashSet<>();
    }

    /** accessor method for staging add.
     * @return stagingAdd hashMap.*/
    public HashMap<String, String> getToAdd() {
        return stagingAdd;
    }

    /** accessor method for staging RM.
     * @return stagingRM hashSet.
     */
    public HashSet<String> getToRm() {
        return stagingRm;
    }

    /** add to staging add.
     * @param filename file name.
     * @param blobID file content.
     */
    public void stageForAddition(String filename, String blobID) {
        if (stagingAdd.containsKey(filename)) {
            stagingAdd.replace(filename, blobID);
        } else {
            stagingAdd.put(filename, blobID);
        }
    }

    /** remove from staging add.
     * @param filename file name.
     */
    public void removeFromStagingAdd(String filename) {
        stagingAdd.remove(filename);
    }

    /** add to staging remove.
     * @param filename file name.
     */
    public void stageForRemoval(String filename) {
        stagingRm.add(filename);
    }

    /** remove from staging add.
     * @param name file name.
     */
    public void removeFromStagingRm(String name) {
        stagingRm.remove(name);
    }

    /** check to see if staging area is empty.
     * @return true if both staging add and staging remove is empty.
     */
    public boolean isEmpty() {
        return stagingRm.isEmpty() && stagingAdd.isEmpty();
    }

    /** clear staging area; after commit. */
    public void clearAfterCommit() {
        stagingAdd.clear();
        stagingRm.clear();
    }

    /** check to see if a file is staged for removal.
     * @param filename the file we want to check for.
     * @return whether stagingRm contains input. */
    public boolean stagingAddContains(String filename) {
        return stagingAdd.containsKey(filename);
    }

    /** check to see if a file is staged for addition.
     * @param filename the file we want to check for.
     * @return whether stagingAdd contains input. */
    public boolean stagingRMContains(String filename) {
        return stagingRm.contains(filename);
    }
    /** staging for adding. */
    private HashMap<String, String> stagingAdd;

    /** staging for removing. */
    private HashSet<String> stagingRm;

}
