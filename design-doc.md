# gitlet design doc

## Classes and Data Structures

###### Static Variables

* `(File) Main.CWD` The working directory at Gitlet runtime.
* `(File) Main.GITLET_FOLDER` Hidden folder in CWD that contains all files neccessary for Gitlet operation.
* `(File) Main.REFS` Directory that contains one file for each branch. In those files is the SHA-1 ID of that branch's current head commit.
* `(File) Main.COMMITS` Directory that contains all of the serialized commits.
* `(File) Main.BLOBS` Directory that contains all of the serialized blobs.
* `(File) Main.HEAD` File that contains the filepath of the current branch in refs.
* `(File) Main.ADD` Directory that contains the blobs staged for addition.
* `(File) Main.REMOVE` Directory that contains the blobs staged for removal.

### Commit

This class represents commits.

###### Instance Variables

* `(String) _id` - SHA-1 hash identifier.
* `(String) _message`  - The message of a commit.
* `(String) _timestamp` - Timestamp.
* `(String) _branch` - The branch this commit belongs to.
* `(String) _parent` - Prior commit SHA-1 ID.
* `(String) _secondParent` - For merged commits.
* `(Hashmap<String, String>) _blobs` - Map of blobs *_file* to blobs *_id*.

### Blob

This class represents files as a filename and contents.

###### Instance Variables

* `(String) _id` - SHA-1 hash identifier.
* `(String) _file` - The filename of what it represents. (./_file)
* `(Byte[]) _contents` - Serialized contents of a file.

## Algorithms

###### Main

* All commands listed in the project spec.
* `validateArgs(String[] args)` - called by all the commands to check for correct # arguments.

###### Commit

* `Commit(String message, String parent)` - Class constructor. `parent` is the SHA-1 id/filename of the parent of this commit. 
                                          The procedures of this constructor are:
                                          1. Copy parent state.
                                          2. Set `_message` and `_timestamp`.
                                          3. Add/remove blobs from `_blobs` map according to `.gitlet/stage` files.
                                          4. Assign `_id`.
                                          5. Serialize and save this commit to '.gitlet/commits'.
* `Commit()` - Constructs initial commit. 
   
###### Blob

* `Blob(String filePath)` The constructor for a blob object that represents the file at `filePath`. This constructor reads
                          the file's contents as a byte array to `_contents`. It also sets `_filename` before generating 
                          and setting the `_id`. 
                      

## Persistence

Each command in Gitlet makes/modifies files and directories in CWD and ".gitlet/*".
After each call to gitlet.Main, we want to save any new or changed commits/blobs to disk using `Utils.writeObject`. `Utils.writeObject(file, obj)` paramaters are the filepath we want to save
an object to, and the object itself. So, blobs saved for staging go into `(Main.ADD|Main.REMOVE)/blob._id`, commits go into `Main.COMMITS/commit._id`, etc. 

###### Methods

Useful methods for persistence can be found in the `Utils` class. Additionally there are
methods defined in `Main` to assist in persistence:
* `Commit getHeadCommit()` Returns the current head commit object.
* `Commit getBranchHead(String branch)` Returns the current head commit for a branch.
* `Commit getCommit(String id)` Returns the commit with the given `id`.
* `Blob getBlob(String id)` Returns the blob with the given 'id'.

