package gitlet;

import java.io.File;
import java.util.Arrays;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Frank Warren
 */
public class Main {
    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String[] args) {
        try {
            mainCheck(args);
            switch (args[0]) {
            case "init":
                init(args);
                break;
            case "add":
                add(args);
                break;
            case "commit":
                commit(args);
                break;
            case "rm":
                rm(args);
                break;
            case "log":
                log(args);
                break;
            case "global-log":
                globalLog(args);
                break;
            case "find":
                find(args);
                break;
            case "status":
                status(args);
                break;
            case "checkout":
                checkout(args);
                break;
            case "branch":
                branch(args);
                break;
            case "rm-branch":
                rmBranch(args);
                break;
            case "reset":
                reset(args);
                break;
            case "merge":
                merge(args);
                break;
            default:
                System.out.println("No command with that name exists.");
            }
        } catch (GitletException error) {
            System.out.println(error.getMessage());
        }
        System.exit(0);
    }

    /** Initializes a gitlet repository in the current working directory if
     * it does not already exist.
     * All repos start with the same initialCommit and no files on branch
     * "master". ARGS should only contain 'init'. */
    static void init(String[] args) {
        validateArgs(args, 1);
        if (!GITLET_FOLDER.exists()) {
            GITLET_FOLDER.mkdir();
            REFS.mkdir();
            COMMITS.mkdir();
            BLOBS.mkdir();
            STAGE.mkdir();
            ADD.mkdir();
            REMOVE.mkdir();
            File master = Utils.join(REFS, "master");
            Commit initialCommit = new Commit();
            initialCommit.save();
            Utils.writeContents(master, initialCommit.toString());
            Utils.writeContents(HEAD, master.getAbsolutePath());
        } else {
            throw new GitletException("A Gitlet version-control system "
                    + "already exists in the current directory.");
        }
    }

    /** Adds a copy of a file as it currently exists to the staging area.
     * Staging an already-staged file overwrites previous entry. If the file
     * is identical to the version in the current commit, it is removed from
     * the staging area if it was already there.
     * ARGS should only contain 'add', [file name]. */
    static void add(String[] args) {
        validateArgs(args, 2);
        File file = Utils.join(CWD, args[1]);
        if (!file.exists()) {
            throw new GitletException("File does not exist.");
        }
        File stageFile = Utils.join(ADD, args[1]);
        Utils.writeContents(stageFile, Utils.readContents(file));
        Blob blob = new Blob(args[1]);
        Commit head = Utils.readObject(COMMITS, getHeadId(), Commit.class);
        String fileIdHead = head.getBlobs().get(args[1]);
        if (blob.toString().equals(fileIdHead)) {
            stageFile.delete();
        }
        Utils.join(REMOVE, args[1]).delete();
    }

    /** Creates a new commit as a copy of its parent.
     * Then, starts/stops tracking files in .gitlet/stage.
     * Finally, saves commit to .gitlet/commits.
     * ARGS should only contain 'commit', [commit message]. */
    static void commit(String[] args) {
        validateArgs(args, 2);
        Commit commit = new Commit(args[1], getHeadId());
        commit.save();
        saveNewHead(commit.toString());
    }

    /** Removes file from .gitlet/stage/add if it is there.
     *  Also, if the file is tracked by the current commit, the file is added
     *  to .gitlet/stage/remove (staged for removal) and removed from CWD.
     * ARGS should only contain 'rm', [file name]. */
    static void rm(String[] args) {
        validateArgs(args, 2);
        File file = Utils.join(CWD, args[1]);
        File stagedFile = Utils.join(ADD, args[1]);
        Commit head = Utils.readObject(COMMITS, getHeadId(), Commit.class);
        if (head.getBlobs().containsKey(args[1])) {
            File stageFile = Utils.join(REMOVE, args[1]);
            Utils.writeContents(stageFile, "staged for removal");
            file.delete();
        } else if (!stagedFile.exists()) {
            throw new GitletException("No reason to remove the file.");
        }
        Utils.join(ADD, args[1]).delete();
    }

    /** Starts at the current head commit, and displays information about
     * each commit backwards along each first parent link.
     * ARGS should only contain 'log'. */
    static void log(String[] args) {
        validateArgs(args, 1);
        Commit commit = Utils.readObject(COMMITS, getHeadId(), Commit.class);
        boolean oneMore = true;
        while (true) {
            System.out.println("===");
            System.out.println("commit " + commit);
            if (commit.getSecondParent() != null) {
                System.out.println("Merge: "
                        + commit.getParent().substring(0, 7) + " "
                        + commit.getSecondParent().substring(0, 7));
            }
            System.out.println("Date: " + commit.getTimestamp());
            System.out.println(commit.getMessage() + "\n");
            if (commit.getParent() == null) {
                break;
            }
            commit = Utils.readObject(
                    COMMITS, commit.getParent(), Commit.class);
        }
    }

    /** Displays info about all commits ever created. ARGS should only
     * contain 'global-log'. */
    static void globalLog(String[] args) {
        validateArgs(args, 1);
        Commit commit;
        for (File dir : COMMITS.listFiles()) {
            for (File file : dir.listFiles()) {
                commit = Utils.readObject(file, Commit.class);
                System.out.println("===");
                System.out.println("commit " + commit);
                if (commit.getSecondParent() != null) {
                    System.out.println("Merge: "
                            + commit.getParent().substring(0, 7) + " "
                            + commit.getSecondParent().substring(0, 7));
                }
                System.out.println("Date: " + commit.getTimestamp());
                System.out.println(commit.getMessage() + "\n");
            }
        }
    }

    /** Prints out ids of all commits that have the given commit message.
     * ARGS should only contain 'find', [commit message]. */
    static void find(String[] args) {
        validateArgs(args, 2);
        boolean found = false;
        Commit commit;
        for (File dir : COMMITS.listFiles()) {
            for (File file : dir.listFiles()) {
                commit = Utils.readObject(file, Commit.class);
                if (commit.getMessage().equals(args[1])) {
                    System.out.println(commit);
                    found = true;
                }
            }
        }
        if (!found) {
            throw new GitletException("Found no commit with that message.");
        }
    }

    /** Displays current branches, staged files, modified files, and
     * untracked files. ARGS should only contain 'status'. */
    static void status(String[] args) {
        validateArgs(args, 1);
        File headName = new File(Utils.readContentsAsString(HEAD));
        Commit head = Utils.readObject(COMMITS, getHeadId(), Commit.class);
        System.out.println("=== Branches ===");
        for (String file : Utils.plainFilenamesIn(REFS)) {
            if (file.equals(headName.getName())) {
                System.out.println("*" + file);
            } else {
                System.out.println(file);
            }
        }
        System.out.println("\n=== Staged Files ===");
        for (String file : Utils.plainFilenamesIn(ADD)) {
            System.out.println(file);
        }
        System.out.println("\n=== Removed Files ===");
        for (String file : Utils.plainFilenamesIn(REMOVE)) {
            System.out.println(file);
        }
        System.out.println("\n=== Modifications Not Staged For Commit ===");
        System.out.println("\n=== Untracked Files ===");
        System.out.println();
    }

    /** Takes a file or files from the head commit or a specified branch and
     * writes them to CWD. ARGS should only contain 'checkout', followed by
     * either '--', [filename] OR [commit id], '--' [filename],
     * OR [branch name]. */
    static void checkout(String[] args) {
        validateArgs(args, 2, 3, 4);
        Commit head = Utils.readObject(COMMITS, getHeadId(), Commit.class);
        Commit branch = null;
        switch (args.length) {
        case 4:
            try {
                head = Utils.readObject(COMMITS, args[1], Commit.class);
            } catch (IllegalArgumentException e) {
                throw new GitletException("No commit with that id exists.");
            }
            /* fall through */
        case 3:
            File file = Utils.join(CWD, args[args.length - 1]);
            String blobId = head.getBlobs().get(args[args.length - 1]);
            if (blobId == null) {
                throw new GitletException("File does not exist in that "
                        + "commit.");
            }
            Blob blob = Utils.readObject(BLOBS, blobId, Blob.class);
            Utils.writeContents(file, blob.getContents());
            break;
        case 2:
            File branchPath = Utils.join(REFS, args[1]);
            if (!branchPath.exists()) {
                try {
                    branch = Utils.readObject(COMMITS, args[1], Commit.class);
                } catch (IllegalArgumentException e) {
                    throw new GitletException("No commit with that id exists.");
                }
            } else if (branchPath.equals(
                new File(Utils.readContentsAsString(HEAD)))) {
                throw new GitletException("No need to checkout the "
                        + "current branch.");
            }
            if (branch == null) {
                String branchId = Utils.readContentsAsString(branchPath);
                branch = Utils.readObject(COMMITS, branchId, Commit.class);
            }
            for (String s : Utils.plainFilenamesIn(CWD)) {
                if (head.getBlobs().get(s) == null
                    && branch.getBlobs().get(s) != null) {
                    throw new GitletException("There is an untracked file in th"
                            + "e way; delete it, or add and commit it first.");
                }
            }
            branch.getBlobs().forEach(
                (name, id) -> Utils.writeContents(Utils.join(CWD, name),
                    Utils.readObject(BLOBS, id, Blob.class).getContents()));
            for (String key : head.getBlobs().keySet()) {
                if (!branch.getBlobs().containsKey(key)) {
                    Utils.join(CWD, key).delete();
                }
            }
            Utils.writeContents(HEAD, branchPath.toString());
            break;
        default:
            break;
        }
    }

    /** Creates a new branch with the given name and points HEAD at it.
     * ARGS should only contain 'branch', [branch name]. */
    static void branch(String[] args) {
        validateArgs(args, 2);
        File branch = Utils.join(REFS, args[1]);
        if (branch.exists()) {
            throw new GitletException("A branch with that name "
                    + "already exists.");
        }
        Utils.writeContents(branch, getHeadId());
    }

    /** Deletes the branch with the given name. ARGS should only contain
     * 'rm-branch' + [branch name]. */
    static void rmBranch(String[] args) {
        validateArgs(args, 2);
        File branch = Utils.join(REFS, args[1]);
        if (!branch.exists()) {
            throw new GitletException("A branch with that "
                    + "name does not exist.");
        }
        if (Utils.readContentsAsString(HEAD).equals(branch.toString())) {
            throw new GitletException("Cannot remove the current branch.");
        }
        branch.delete();
    }

    /** Checks out all the files tracked by the given commit. Removes tracked
     * files that are not present in that commit. ARGS should only contain
     * 'reset' + [branch name].*/
    static void reset(String[] args) {
        validateArgs(args, 2);
        checkout(new String[]{"checkout", args[1]});
    }

    /** Merges files from a branch into the current branch. ARGS should only
     * contain 'merge', [branch name]. */
    static void merge(String[] args) {
        validateArgs(args, 2);
        String branch = Utils.readContentsAsString(Utils.join(REFS, args[1]));
        for (String ancestor : Utils.readObject(
                COMMITS, getHeadId(), Commit.class).getAncestors()) {
            for (String branchAnc : Utils.readObject(
                    COMMITS, branch, Commit.class).getAncestors()) {
                if (ancestor.equals(branchAnc)) {
                    break;
                }
            }
        }
    }

    /** Returns the current head commit id. */
    static String getHeadId() {
        File headLocation = new File(Utils.readContentsAsString(HEAD));
        return Utils.readContentsAsString(headLocation);
    }

    /** Overwrites .gitlet/HEAD with commit with the given ID. */
    static void saveNewHead(String id) {
        Utils.writeContents(new File(Utils.readContentsAsString(HEAD)), id);
    }

    /** Checks the size of ARGS against expected number(s) N.
     */
    static void validateArgs(String[] args, int... n) {
        if (Arrays.stream(n).noneMatch(i -> i == args.length)) {
            throw new GitletException("Incorrect operands.");
        }
        if (args[0].equals("checkout") && args.length > 2) {
            for (int i = 0; !args[i].equals("--"); i += 1) {
                if (i == args.length - 1) {
                    throw new GitletException("Incorrect operands.");
                }
            }
        }
    }

    /** Checks the ARGS and throws errors as necessary. */
    static void mainCheck(String[] args) {
        if (args.length == 0) {
            throw new GitletException("Please enter a command.");
        }
        if (!args[0].equals("init") && !GITLET_FOLDER.exists()) {
            throw new GitletException("Not in an initialized Gitlet directory"
                    + ".");
        }
    }

    /** CWD directory. */
    static final File CWD = new File(System.getProperty("user.dir"));

    /** CWD/.gitlet/. */
    static final File GITLET_FOLDER = Utils.join(CWD, ".gitlet");

    /** CWD/.gitlet/REFS/. */
    static final File REFS = Utils.join(GITLET_FOLDER, "refs");

    /** CWD/.gitlet/commits/. */
    static final File COMMITS = Utils.join(GITLET_FOLDER, "commits");

    /** CWD/.gitlet/blobs/. */
    static final File BLOBS = Utils.join(GITLET_FOLDER, "blobs");

    /** CWD/.gitlet/HEAD. */
    static final File HEAD = Utils.join(GITLET_FOLDER, "HEAD");

    /** CWD/.gitlet/stage/. */
    static final File STAGE = Utils.join(GITLET_FOLDER, "stage");

    /** CWD/.gitlet/stage/add. */
    static final File ADD = Utils.join(STAGE, "add");

    /** CWD/.gitlet/stage/remove. */
    static final File REMOVE = Utils.join(STAGE, "remove");
}
