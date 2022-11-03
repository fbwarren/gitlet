# gitlet

Use `java gitlet.Main init` to initialize a new gitlet repo in the current directory  
Then, use `java gitlet.Main [command] [args]` where `[command]` is any supported command and `[args]` are the appropriate arguments.  

**Supported Commands**
- `init`
- `add [file name]`
- `commit [message]`
- `rm [file name]`
- `log`
- `global-log`
- `find [commit message]`
- `status`
- `checkout -- [file name]`
- `checkout [commit id] -- [file name]`
- `checkout -- [branch name]`
- `branch [branch name]`
- `rm-branch [branch name]`
- `reset [commit id]`
- `merge [branch name]`
