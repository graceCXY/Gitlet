package gitlet;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.io.File;

/** The system that process commands made to
 * Gitlet, the tiny stupid version-control system.
 *  @author Grace Chen
 */
public class Gitlet {

    /** the default gitlet class constructor. */
    public Gitlet() {
        staging = new StagingArea();
        saveStaging();
        _tree = new CommitTree();
        branch = _tree.getBranchName();
        saveBranch(_tree);
        saveCurrBranch();
        Commit initialCommit = new Commit();
        byte[] initArray = Utils.serialize(initialCommit);
        String initialID = Utils.sha1(initArray);
        initID = initialID;
        saveCommit(initialCommit);
        updateHead(initialCommit, initialID);
        _tree.setHeadID(initialID);
        saveBranch(_tree);
        saveCurrBranch();
        Gitlet.gitletDir.mkdir();
        Gitlet.stagingDir.mkdir();
        Gitlet.blobDir.mkdir();
        Gitlet.commitDir.mkdir();
        Gitlet.branchDir.mkdir();
        File initialized = new File(".gitlet/initialized.txt");
        try {
            initialized.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** set up all the files to ensure persistence. */
    public static void setupPersistence() {
        Gitlet.gitletDir.mkdir();
        Gitlet.stagingDir.mkdir();
        Gitlet.blobDir.mkdir();
        Gitlet.commitDir.mkdir();
        Gitlet.branchDir.mkdir();
        Gitlet.remoteDir.mkdir();
        File initialized = new File(".gitlet/initialized.txt");
        if (initialized.exists()) {
            File stagingFile = new File(".gitlet/staging/stagingArea.txt");
            File currBranchFile = new File(".gitlet/staging/branch");
            headFile = new File(".gitlet/head.txt");
            staging = Utils.readObject(stagingFile, StagingArea.class);
            branch = Utils.readObject(currBranchFile, String.class);
            _tree = getBranch(branch);
            headID = Utils.readObject(headFile, String.class);
            head = getCommit(headID);
        }

    }

    /** adds a blob file and stage it for addition.
     * @param name the name of the file we want to save.*/
    public static void addBlob(String name) {
        File toAdd = new File(name);
        if (toAdd.exists()) {
            String updatedContent = Utils.readContentsAsString(toAdd);
            Blob currBlob = new Blob(updatedContent);
            String blobID = Utils.sha1(Utils.serialize(currBlob));
            if (staging.stagingRMContains(name)) {
                staging.removeFromStagingRm(name);
                saveStaging();
                return;
            }
            if (head.getBlobs() != null && head.findBlob(name).equals(blobID)) {
                if (staging.getToAdd().containsKey(name)) {
                    staging.removeFromStagingAdd(name);
                }
                saveStaging();
                return;
            }
            staging.stageForAddition(name, blobID);
            saveBlob(currBlob);
            saveStaging();
        } else {
            System.out.println("File does not exist.");
        }
    }

    /** remove file from current head and the CWD.
     * @param filename the file we want to remove. */
    public static void removeFile(String filename) {
        if (staging.stagingAddContains(filename)) {
            staging.removeFromStagingAdd(filename);
            saveStaging();
        } else if (head.containsBlob(filename)) {
            staging.stageForRemoval(filename);
            File toDelete = new File(filename);
            Utils.restrictedDelete(toDelete);
            saveStaging();
        } else {
            System.out.println("No reason to remove the file.");
        }
    }

    /** add the commit to the gitlet structure and the current commit tree.
     * @param newCommit the commit we want to add.*/
    public static void addCommit(Commit newCommit) {
        if (staging.isEmpty()) {
            System.out.println("No changes added to the commit.");
        } else {
            HashMap<String, String> currBlobs = newCommit.getBlobs();
            for (String filename: staging.getToRm()) {
                newCommit.removeBlob(filename);
            }
            HashMap<String, String> toAdd = staging.getToAdd();
            for (String file: toAdd.keySet()) {
                if (currBlobs.keySet().contains(file)) {
                    newCommit.replaceBlobContent(file, toAdd.get(file));
                } else {
                    newCommit.addBlob(file, toAdd.get(file));
                }
            }
            saveCommit(newCommit);
            staging.clearAfterCommit();
            saveStaging();
        }
    }

    /** make a new branch.
     * @param branchName the branch we want to add to the system.*/
    public static void addBranch(String branchName) {
        if (getBranch(branchName) != null) {
            System.out.println("A branch with that name already exists.");
        }
        CommitTree newBranch = new CommitTree(branchName, headID);
        saveBranch(newBranch);
    }

    /** remove the pointer to the branch. Deals with special cases.
     * @param branchName the branch we want to remove.*/
    public static void rmBranch(String branchName) {
        if (branch.equals(branchName)) {
            System.out.println("Cannot remove the current branch.");
        } else {
            File toDelete = new File(".gitlet/branches/" + branchName);
            if (toDelete.exists()) {
                toDelete.delete();
            } else {
                System.out.println("A branch with that name does not exist.");
            }
        }
    }

    /** save the commit object into a file in the commit directory.
     * @param c is the input commit we want to save. */
    public static void saveCommit(Commit c) {
        byte[] commitArray = Utils.serialize(c);
        String commitID = Utils.sha1(commitArray);
        File commitFile = new File(".gitlet/commits/" + commitID);
        try {
            commitFile.createNewFile();
            Utils.writeObject(commitFile, c);
            updateHead(c, commitID);
            _tree.setHeadID(commitID);
            saveBranch(_tree);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** save the branch input branch into a file in the branch directory.
     * @param currbranch is the input branch we want to save. */
    public static void saveBranch(CommitTree currbranch) {
        File commitFile = new File(".gitlet/branches/"
                + currbranch.getBranchName());
        try {
            commitFile.createNewFile();
            Utils.writeObject(commitFile, currbranch);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** save the staging area into a file in the staging directory. */
    public static void saveStaging() {
        File stagingFile = new File(".gitlet/staging/stagingArea.txt");
        try {
            stagingFile.createNewFile();
            Utils.writeObject(stagingFile, staging);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** save the current head into a head file. */
    public static void saveHead() {
        try {
            headFile.createNewFile();
            Utils.writeObject(headFile, headID);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** update the head after a commit.
     * @param c the new head commit.
     * @param commitID the new commit ID. */
    private static void updateHead(Commit c, String commitID) {
        head = c;
        headID = commitID;
        saveHead();
    }

    /** save the current branch we are on in to a file. */
    public static void saveCurrBranch() {
        File currBranchFile = new File(".gitlet/staging/branch");
        try {
            currBranchFile.createNewFile();
            Utils.writeObject(currBranchFile, branch);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** update the current branch we are on.
     * @param branchName the new branch we are now on.*/
    public static void updateBranch(String branchName) {
        branch = branchName;
        _tree = getBranch(branch);
        saveCurrBranch();
    }

    /** print log on this head commit. */
    public static void log() {
        Commit curr = head;
        String currID = headID;
        while (curr != null) {
            logFormat(curr, currID);
            currID = curr.getParent();
            curr = getCommit(currID);
        }
    }

    /** print global log. */
    public static void globalLog() {
        List<String> globalCommits =
                new ArrayList<>(Utils.plainFilenamesIn(commitDir));
        for (int i = 0; i < globalCommits.size(); i += 1) {
            String commitID = globalCommits.get(i);
            Commit currCommit = getCommit(commitID);
            logFormat(currCommit, commitID);
        }
    }

    /** print messages in log format.
     * @param commitID the commit id.
     * @param c the commit. */
    public static void logFormat(Commit c, String commitID) {
        System.out.println("===");
        System.out.println("commit " + commitID);
        if (c.getParent2() != null) {
            System.out.println("Merge: " + c.getParent().substring(0, 7)
                    + " " + c.getParent2().substring(0, 7));
        }
        System.out.println("Date: " + c.getTimeStamp());
        System.out.println(c.getLogMessage());
        System.out.println();
    }

    /** Find a blob object from the blob file with the following blobID.
     * @param blobID the blob ID
     * @return the blob object that the id refers to.*/
    public static Blob getBlob(String blobID) {
        File blobFile = new File(".gitlet/blobs/" + blobID + ".txt");
        Blob result = null;
        if (blobFile.exists()) {
            result = Utils.readObject(blobFile, Blob.class);
        }
        return result;
    }

    /** save the blob object into a file in the blob directory.
     * @param b is the input commit we want to save. */
    public static void saveBlob(Blob b) {
        byte[] blobArray = Utils.serialize(b);
        String blobID = Utils.sha1(blobArray);
        File commitFile = new File(".gitlet/blobs/" + blobID + ".txt");
        try {
            commitFile.createNewFile();
            Utils.writeObject(commitFile, b);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Find a commit object from commit files with the following commitID.
     * @param commitID the commit ID.
     * @return the commit object this ID refers to. */
    public static Commit getCommit(String commitID) {
        File commitFile = null;
        Commit result = null;
        if (commitID != null && commitID.length() == 8) {
            List<String> commits =
                    new ArrayList<>(Utils.plainFilenamesIn(commitDir));
            for (String cID: commits) {
                if (cID.substring(0, 8).equals(commitID)) {
                    commitFile = new File(".gitlet/commits/" + cID);
                    break;
                }
            }
        } else {
            commitFile = new File(".gitlet/commits/" + commitID);
        }
        if (commitFile != null && commitFile.exists()) {
            result = Utils.readObject(commitFile, Commit.class);
        }
        return result;
    }

    /** find commitTree object in branch file with input branch name.
     * @param branchName the branch name
     * @return the object that the branch refers to.*/
    public static CommitTree getBranch(String branchName) {
        File branchFile = new File(".gitlet/branches/" + branchName);
        CommitTree result = null;
        if (branchFile.exists()) {
            result = Utils.readObject(branchFile, CommitTree.class);
        }
        return result;
    }

    /** process the find command.
     * @param message the message we want to look for. */
    public static void findMessage(String message) {
        boolean foundIt = false;
        List<String> globalCommits = Utils.plainFilenamesIn(commitDir);
        for (int i = 0; i < globalCommits.size(); i += 1) {
            String commitID = globalCommits.get(i);
            Commit currCommit = getCommit(commitID);
            if (currCommit.getLogMessage().equals(message)) {
                System.out.println(commitID);
                foundIt = true;
            }
        }
        if (!foundIt) {
            System.out.println("Found no commit with that message.");
        }
    }

    /** get current status on branches, staging, and untracked files. */
    public static void status() {
        System.out.println("=== Branches ===");
        List<String> branches =
                new ArrayList<>(Utils.plainFilenamesIn(branchDir));
        for (int i = 0; i < branches.size(); i += 1) {
            String currBranch = branches.get(i);
            if (currBranch.equals(branch)) {
                System.out.println("*" + currBranch);
            } else {
                System.out.println(currBranch);
            }
        }
        System.out.println();
        System.out.println("=== Staged Files ===");
        if (staging != null && staging.getToAdd() != null) {
            for (String filename : staging.getToAdd().keySet()) {
                System.out.println(filename);
            }
        }
        System.out.println();
        System.out.println("=== Removed Files ===");
        if (staging != null && staging.getToRm() != null) {
            for (String filename : staging.getToRm()) {
                System.out.println(filename);
            }
        }
        System.out.println();
        System.out.println("=== Modifications Not Staged For Commit ===");
        HashMap<String, String> trackedByPrev = head.getBlobs();
        if (trackedByPrev != null) {
            for (String file : trackedByPrev.keySet()) {
                File curr = new File(file);
                if (curr.exists()) {
                    String currContent = Utils.readContentsAsString(curr);
                    Blob currBlob = new Blob(currContent);
                    String blobID = Utils.sha1(Utils.serialize(currBlob));
                    if (!staging.getToAdd().containsKey(file)
                            && !staging.getToRm().contains(file)
                            && head.getBlobs() != null
                            && !head.findBlob(file).equals(blobID)) {
                        System.out.println(file + " (modified)");
                    }
                }
                if (!staging.getToRm().contains(file) && !curr.exists()) {
                    System.out.println(file + " (deleted)");
                }

            }
        }
        System.out.println();
        System.out.println("=== Untracked Files ===");
        List<String> untracked = getUntrackedFiles();
        for (String file: untracked) {
            System.out.println(file);
        }
        System.out.println();
    }

    /** checkout file based on the commit.
     * @param commitID the commit ID that we want to search in.
     * @param fileName the specific file we want to checkout. */
    public static void checkoutFile(String commitID, String fileName) {
        Commit toFind;
        if (commitID.equals("head")) {
            toFind = head;
        } else {
            toFind = getCommit(commitID);
            if (toFind == null) {
                System.out.println("No commit with that id exists.");
                return;
            }
        }
        if (toFind.containsBlob(fileName)) {
            File newFile = new File(fileName);
            String blobID = toFind.findBlob(fileName);
            Blob toOverWrite = getBlob(blobID);
            if (newFile.exists()) {
                Utils.writeContents(newFile, toOverWrite.getContent());
            } else {
                try {
                    newFile.createNewFile();
                    Utils.writeContents(newFile, toOverWrite.getContent());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            System.out.println("File does not exist in that commit.");
        }
    }

    /** checkout a specific branch by finding the branch,
     * update branch, and checkout all files.
     * @param branchName the name of the branch we want to checkout. */
    public static void checkoutBranch(String branchName) {
        branchName = branchName.replace("/", "-");
        List<String> untracked = getUntrackedFiles();
        if (!untracked.isEmpty()) {
            System.out.println("There is an untracked file in the way; "
                    + "delete it, or add and commit it first.");
            return;
        }
        if (branch.equals(branchName)) {
            System.out.println("No need to checkout the current branch.");
            return;
        }
        CommitTree desiredBranch = getBranch(branchName);
        if (desiredBranch == null) {
            System.out.println("No such branch exists.");
        } else {
            String newHeadID = desiredBranch.getHeadID();
            Commit newHead = getCommit(newHeadID);
            HashMap<String, String> blobs = newHead.getBlobs();
            if (blobs != null) {
                for (String file : blobs.keySet()) {
                    checkoutFile(newHeadID, file);
                }
            }
            if (head.getBlobs() != null) {
                for (String file: head.getBlobs().keySet()) {
                    if (!newHead.containsBlob(file)) {
                        Utils.restrictedDelete(file);
                    }
                }
            }
            updateHead(newHead, newHeadID);
            updateBranch(branchName);
            saveCurrBranch();
            staging.clearAfterCommit();
            saveStaging();
        }
    }

    /** reset to previous commit by checking out all files in that commit
     * and update the head.
     * @param commitID the commit we want reset to.
     */
    public static void reset(String commitID) {
        List<String> untracked = getUntrackedFiles();
        if (!untracked.isEmpty()) {
            System.out.println("There is an untracked file in the way; "
                    + "delete it, or add and commit it first. ");
            return;
        }
        Commit toCheckout = getCommit(commitID);
        if (toCheckout == null) {
            System.out.println("No commit with that id exists.");
        } else {
            HashMap<String, String> blobs = toCheckout.getBlobs();
            for (String file: blobs.keySet()) {
                checkoutFile(commitID, file);
            }
            for (String file: getFilesInCWD()) {
                if (!blobs.containsKey(file)) {
                    File toDelete = new File(file);
                    Utils.restrictedDelete(toDelete);
                }
            }
            updateHead(toCheckout, commitID);
            _tree.setHeadID(commitID);
            saveBranch(_tree);
            saveCurrBranch();
            staging.clearAfterCommit();
            saveStaging();
        }
    }
    /** merge errors.
     * @param otherBranchName the branch name.
     * @return whether there is an error. */
    public static boolean mergeErrors1(String otherBranchName) {
        List<String> untracked = getUntrackedFiles();
        if (!untracked.isEmpty()) {
            System.out.println(untracked.toArray());
            System.out.println("There is an untracked file in the way; "
                    + "delete it, or add and commit it first.");
            return true;
        }
        if (!staging.isEmpty()) {
            System.out.println("You have uncommitted changes.");
            return true;
        }
        if (getBranch(otherBranchName) == null) {
            System.out.println("A branch with that name does not exist.");
            return true;
        }
        if (otherBranchName.equals(branch)) {
            System.out.println("Cannot merge a branch with itself.");
            return true;
        }
        return false;
    }
    /** merge errors.
     * @param otherBranchName the branch name.
     * @param otherBranch the other branch.
     * @param otherHeadID the other head id.
     * @return whether there is an error. */
    public static boolean mergeErrors2(String otherBranchName,
                                      CommitTree otherBranch,
                                      String otherHeadID) {
        if (otherBranch.getSplitpoint() != null
                && _tree.containsCommit(otherHeadID)) {
            System.out.println("Given branch is an ancestor "
                    + "of the current branch.");
            return true;
        }
        if (_tree.getSplitpoint() != null
                && otherBranch.containsCommit(headID)) {
            System.out.println("Current branch fast-forwarded.");
            checkoutBranch(otherBranchName);
            return true;
        }
        return false;
    }
    /** helper function of merge.
     * @param otherHead the other head.
     * @param otherHeadID the other head ID.
     * @param otherBranch the other branch.
     * @return splitPoint commit. */
    public static Commit findSplitPoint(Commit otherHead, String otherHeadID,
                                        CommitTree otherBranch) {
        String currHeadParent1 = otherHead.getParent();
        String currHeadParent2 = otherHead.getParent2();
        String splitPointID = otherBranch.getSplitpoint();
        Commit other = otherHead;
        String otherID = otherHeadID;
        while (other != null) {
            if (otherID.equals(currHeadParent1)) {
                splitPointID = currHeadParent1;
                break;
            }
            if (otherID.equals(currHeadParent2)) {
                splitPointID = currHeadParent2;
                break;
            }
            otherID = other.getParent();
            other = getCommit(otherID);
        }
        return getCommit(splitPointID);
    }
    /** merge helper, deal with files present in otherHead.
     * @param currHeadFiles files in current head.
     * @param splitPointFiles files in split point.
     * @param otherHeadFiles files in other head.
     * @param otherHead other head.
     * @param otherHeadID other head ID.
     * @param splitPoint split point commit.
     * @return whether there is a merge conflict. */
    public static boolean processOtherHeadFiles(Commit otherHead,
                                                String otherHeadID,
                                                Commit splitPoint,
                                                HashMap<String, String>
                                                        otherHeadFiles,
                                             HashMap<String, String>
                                                        splitPointFiles,
                                             HashMap<String, String>
                                                        currHeadFiles) {
        boolean conflictedMerge = false;
        for (String file : otherHeadFiles.keySet()) {
            if (splitPointFiles == null) {
                if (currHeadFiles.containsKey(file)) {
                    mergeConflict(head, file, otherHead, file);
                    conflictedMerge = true;
                } else {
                    checkoutFile(otherHeadID, file);
                    staging.stageForAddition(file,
                            otherHead.findBlob(file));
                    saveStaging();
                }
            } else if (splitPointFiles != null
                    && !splitPointFiles.containsKey(file)) {
                if (currHeadFiles.containsKey(file)) {
                    mergeConflict(head, file, otherHead, file);
                    conflictedMerge = true;
                } else {
                    checkoutFile(otherHeadID, file);
                    staging.stageForAddition(file,
                            otherHead.findBlob(file));
                    saveStaging();
                }
            } else {
                if (!currHeadFiles.containsKey(file)) {
                    if (!otherHead.findBlob(file)
                            .equals(splitPoint.findBlob(file))) {
                        checkoutFile(otherHeadID, file);
                        staging.stageForAddition(file,
                                otherHead.findBlob(file));
                        saveStaging();
                    } else {
                        if (getFilesInCWD().contains(file)) {
                            removeFile(file);
                        }
                        staging.stageForRemoval(file);
                        saveStaging();
                    }
                }
            }
        }
        return conflictedMerge;
    }

    /** merge helper, deal with files present in split point.
     * @param sPointFiles files in split point.
     * @param otherHead other head.
     * @param otherHeadID other head ID.
     * @param splitPoint split point commit.
     * @return whether there is a merge conflict. */
    public static boolean processSPFiles(Commit otherHead, String otherHeadID,
                                         Commit splitPoint,
                                         HashMap<String, String> sPointFiles) {
        boolean conflictedMerge = false;
        for (String file : sPointFiles.keySet()) {
            if (head.containsBlob(file)) {
                boolean changedInCurr = !head.findBlob(file)
                                .equals(splitPoint.findBlob(file));
                if (otherHead.containsBlob(file)) {
                    boolean changedInOther = !otherHead.findBlob(file)
                                    .equals(splitPoint.findBlob(file));
                    if (changedInOther) {
                        if (changedInCurr) {
                            if (!head.findBlob(file)
                                    .equals(otherHead.findBlob(file))) {
                                mergeConflict(head, file, otherHead, file);
                                conflictedMerge = true;
                            }
                        } else {
                            checkoutFile(otherHeadID, file);
                            staging.stageForAddition(file,
                                    otherHead.findBlob(file));
                        }
                    } else if (changedInCurr && head.findBlob(file)
                                    .equals(otherHead.findBlob(file))) {
                        mergeConflict(head, file, otherHead, file);
                        conflictedMerge = true;

                    }
                } else {
                    if (changedInCurr) {
                        mergeConflict(head, file, otherHead, "empty");
                        conflictedMerge = true;
                    } else {
                        if (getFilesInCWD().contains(file)) {
                            removeFile(file);
                        }
                        staging.stageForRemoval(file);
                    }
                }
            } else {
                if (otherHead.containsBlob(file)) {
                    if (!otherHead.findBlob(file)
                            .equals(splitPoint.findBlob(file))) {
                        checkoutFile(otherHeadID, file);
                        staging.stageForAddition(file,
                                otherHead.findBlob(file));
                        mergeConflict(head, "empty", otherHead, file);
                        conflictedMerge = true;
                    } else {
                        if (!staging.getToAdd().containsKey(file)) {
                            if (getFilesInCWD().contains(file)) {
                                removeFile(file);
                            }
                            staging.stageForRemoval(file);
                        }
                    }
                }
            }
        }
        return conflictedMerge;
    }

    /** process merge command.
     * @param otherBranchName the other branch
     *      we want to merge the current with. */
    public static void merge(String otherBranchName) {
        if (mergeErrors1(otherBranchName)) {
            return;
        }
        boolean conflictedMerge = false;
        CommitTree otherBranch = getBranch(otherBranchName);
        String otherHeadID = otherBranch.getHeadID();
        Commit otherHead = getCommit(otherHeadID);
        Commit splitPoint = findSplitPoint(otherHead, otherHeadID, otherBranch);
        if (mergeErrors2(otherBranchName, otherBranch, otherHeadID)) {
            return;
        }
        HashMap<String, String> otherHeadFiles = otherHead.getBlobs();
        HashMap<String, String> splitPointFiles = null;
        if (splitPoint != null) {
            splitPointFiles = splitPoint.getBlobs();
        }
        HashMap<String, String> currHeadFiles = head.getBlobs();
        if (otherHeadFiles != null) {
            conflictedMerge = processOtherHeadFiles(otherHead, otherHeadID,
                    splitPoint, otherHeadFiles, splitPointFiles, currHeadFiles);
        }
        if (splitPointFiles != null) {
            conflictedMerge = (processSPFiles(otherHead, otherHeadID,
                    splitPoint, splitPointFiles))
                    || conflictedMerge;
            saveStaging();
        }
        otherBranchName = otherBranchName.replace("-", "/");
        String message = "Merged " + otherBranchName + " into " + branch + ".";
        Commit merge = new Commit(message, headID, otherHeadID);
        addCommit(merge);
        byte[] mergeArray = Utils.serialize(merge);
        String mergeID = Utils.sha1(mergeArray);
        updateHead(merge, mergeID);
        saveHead();
        if (conflictedMerge) {
            System.out.println("Encountered a merge conflict.");
        }
    }

    /** deals with merge conflict by combining 2 different files.
     * @param c1 current commit file, whose content we want first.
     * @param file1 the file ID in current commit.
     * @param c2 the other commit.
     * @param file2 the other file ID in other commit.
     */
    public static void mergeConflict(Commit c1, String file1,
                                     Commit c2, String file2) {
        String header = "<<<<<<< HEAD\n";
        String content1 = "";
        String content2 = "";
        if (!file1.equals("empty")) {
            String blobID = c1.findBlob(file1);
            File b1 = new File(".gitlet/blobs/" + blobID + ".txt");
            Blob blob1 = Utils.readObject(b1, Blob.class);
            content1 += blob1.getContent();
        }
        String middle = "=======\n";
        if (!file2.equals("empty")) {
            String blobID = c2.findBlob(file2);
            File b2 = new File(".gitlet/blobs/" + blobID + ".txt");
            Blob blob1 = Utils.readObject(b2, Blob.class);
            content2 += blob1.getContent();
        }
        String footer = ">>>>>>>\n";
        String overall = header + content1 + middle + content2 + footer;
        File toReplace = new File(file1);
        Utils.writeContents(toReplace, overall);
        addBlob(file1);
    }

    /** return a list of untracked files in the current working directory. */
    public static List<String> getUntrackedFiles() {
        List<String> result = new ArrayList<>();
        List<String> from = getFilesInCWD();
        for (String file: from) {
            if (!head.containsBlob(file) && !staging.stagingAddContains(file)
                    && !staging.stagingRMContains(file)) {
                result.add(file);
            }
        }
        return result;
    }

    /** return a list of files in the working directory. */
    public static List<String> getFilesInCWD() {
        return new ArrayList<>(Utils.plainFilenamesIn("."));
    }

    /** accessor method for head ID.
     * @return the head ID. */
    public static String getHeadID() {
        return headID;
    }

    /** add the pointer to a remote path.
     * @param remoteName the name of the new remote.
     * @param path the remote path of the new remote.*/
    public static void addRemote(String remoteName, String path) {
        File remote = new File(".gitlet/remotes/" + remoteName);
        if (remote.exists()) {
            System.out.println("A remote with that name already exists.");
        } else {
            try {
                remote.createNewFile();
                Utils.writeObject(remote, path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /** remove a pointer to a remote path.
     * @param remoteName the remote pointer we want to remove. */
    public static void rmRemote(String remoteName) {
        File remote = new File(".gitlet/remotes/" + remoteName);
        if (remote.exists()) {
            remote.delete();
        } else {
            System.out.println("A remote with that name does not exist.");
        }
    }

    /** process the push command.
     * @param remoteName the name of the remote name.
     * @param remoteBranchName the name of branch in the remote directory. */
    public static void push(String remoteName, String remoteBranchName) {
        File remote = new File(".gitlet/remotes/" + remoteName);
        String remotePath = Utils.readObject(remote, String.class);
        File remoteFile = new File(remotePath);
        if (!remoteFile.exists()) {
            System.out.println("Remote directory not found.");
            return;
        }
        File remoteHeadFile = new File(remotePath + "/head.txt");
        String remoteHeadID = Utils.readObject(remoteHeadFile, String.class);
        if (!_tree.containsCommit(remoteHeadID)) {
            System.out.println("Please pull down remote changes "
                    + "before pushing.");
            return;
        }
        File remoteBranchFile = new File(remotePath
                + "/branches/" + remoteBranchName);
        CommitTree remoteBranch = null;
        if (!remoteBranchFile.exists()) {
            try {
                remoteBranchFile.createNewFile();
                remoteBranch = new CommitTree(remoteBranchName, initID);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            remoteBranch = Utils.readObject(remoteBranchFile, CommitTree.class);
        }
        List<String> allCommits = Utils.plainFilenamesIn(commitDir);
        for (String commitID: allCommits) {
            File currRemote = new File(remotePath + "/commits/" + commitID);
            File currLocal = new File(".gitlet/commits/" + commitID);
            if (!currRemote.exists()) {
                try {
                    currRemote.createNewFile();
                    Commit c = Utils.readObject(currLocal, Commit.class);
                    Utils.writeObject(currRemote, c);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        List<String> allBlobs = Utils.plainFilenamesIn(blobDir);
        for (String blobID: allBlobs) {
            File currRemote = new File(remotePath + "/blobs/" + blobID);
            File currLocal = new File(".gitlet/blobs/" + blobID);
            if (!currRemote.exists()) {
                try {
                    currRemote.createNewFile();
                    Blob currContent = Utils.readObject(currLocal, Blob.class);
                    Utils.writeObject(currRemote, currContent);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        Utils.writeObject(remoteHeadFile, headID);
        remoteBranch.setHeadID(headID);
        Utils.writeObject(remoteBranchFile, remoteBranch);
    }

    /** process the fetch command.
     * @param remoteName the name of the remote name.
     * @param remoteBranchName the name of branch in the remote directory. */
    public static void fetch(String remoteName, String remoteBranchName) {
        File remote = new File(".gitlet/remotes/" + remoteName);
        String remotePath = Utils.readObject(remote, String.class);
        File remoteFile = new File(remotePath);
        if (!remoteFile.exists()) {
            System.out.println("Remote directory not found.");
            return;
        }
        File remoteBFile = new File(remotePath
                + "/branches/" + remoteBranchName);
        if (!remoteBFile.exists()) {
            System.out.println("That remote does not have that branch.");
            return;
        }
        CommitTree remoteB = Utils.readObject(remoteBFile, CommitTree.class);
        List<String> allC = Utils.plainFilenamesIn(remotePath + "/commits/");
        for (String commitID: allC) {
            File currLocal = new File(".gitlet/commits/" + commitID);
            File currRemote = new File(remotePath + "/commits/" + commitID);
            if (!currLocal.exists()) {
                try {
                    currLocal.createNewFile();
                    Commit c = Utils.readObject(currRemote, Commit.class);
                    Utils.writeObject(currLocal, c);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        List<String> allBlobs = Utils.plainFilenamesIn(remotePath + "/blobs/");
        for (String blobID: allBlobs) {
            File currRemote = new File(remotePath + "/blobs/" + blobID);
            File currLocal = new File(".gitlet/blobs/" + blobID);
            if (!currLocal.exists()) {
                try {
                    currRemote.createNewFile();
                    Blob currContent = Utils.readObject(currRemote, Blob.class);
                    Utils.writeObject(currLocal, currContent);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        File localCopyFile = new File(".gitlet/branches/" + remoteName
                + "-" + remoteBranchName);
        if (!localCopyFile.exists()) {
            try {
                localCopyFile.createNewFile();
                Utils.writeObject(localCopyFile, remoteB);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
//        String remoteheadID = remoteB.getHeadID();
//        updateHead(getCommit(remoteheadID), remoteheadID);
//        _tree.setHeadID(remoteheadID);
//        saveCurrBranch();
    }

    /** process the pull command.
     * @param remoteName the name of the remote name.
     * @param remoteBranchName the name of branch in the remote directory. */
    public static void pull(String remoteName, String remoteBranchName) {
        fetch(remoteName, remoteBranchName);
        merge(remoteName + "-" + remoteBranchName);
    }


    /** the .gitlet directory. */
    private static File gitletDir = new File(".gitlet");

    /** the head file. */
    private static File headFile = new File(".gitlet/head.txt");

    /** commit directory for storing commit objects. */
    private static File commitDir = new File(".gitlet/commits");

    /** blob directory for storing blob objects. */
    private static File blobDir = new File(".gitlet/blobs");

    /** branch directory for storing commitTree objects. */
    private static File branchDir = new File(".gitlet/branches");

    /** folder for storing staging area. */
    private static File stagingDir = new File(".gitlet/staging");

    /** folder for remote paths. */
    private static File remoteDir = new File(".gitlet/remotes");

    /** staging area. */
    private static StagingArea staging;

    /** Current head commit we are on. **/
    private static Commit head;

    /** Current head commit ID. */
    private static String headID;

    /** Branch name of current branch we are on. */
    private static String branch;

    /** Current branch we are on for storing commits. **/
    private static CommitTree _tree;

    /** the id of the initial commit. */
    private static String initID;

}
