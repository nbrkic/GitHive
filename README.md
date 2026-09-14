# GitHive

A lightweight, open-source Git GUI client built with Java and JavaFX. GitHive provides a clean, dark-themed interface for managing Git repositories without relying on heavy external tools.

---

## Features

### Repository Management
- **Open repository** — browse and open any existing local Git repository
- **Init new repository** — initialize a new Git repo in any folder
- **Clone repository** — clone a remote repository by URL into a selected folder
- **Close repository** — close the current repo and return to the welcome screen
- **Recent repositories** — remembers the last 10 opened repos; click to reopen, X to remove from list
- **Manage Remotes** — add, view and remove remote URLs (origin, upstream, etc.)
- **Clear saved credentials** — remove stored username/token from local storage

### Commit History
- **Commit graph** — visual branch graph rendered per row using colored lanes (similar to GitKraken/SourceTree)
- **Commit table** — shows graph, short hash, message, author and date for each commit
- **Real-time search** — filter commits by message, author or hash as you type (`Ctrl+F` to focus)
- **Auto-refresh** — detects external changes (commits, checkouts) every 5 seconds and reloads automatically

### Branches
- **Branch list** — shows all local branches; current branch is highlighted in blue
- **Create branch** — create a new branch from the current HEAD
- **Delete branch** — delete a local branch
- **Checkout branch** — switch to a different branch
- **Rename branch** — rename any local branch
- **Merge** — merge a selected branch into the current branch
- **Rebase** — rebase a selected branch onto the current branch (with confirmation)

### Staging & Committing
- **Changes window** (`Ctrl+Shift+C`) — dedicated panel for staging and committing
- **Unstaged files list** — shows modified (`M`), untracked (`?`) and deleted (`D`) files
- **Staged files list** — shows files ready to commit
- **Stage Selected** — stage selected files from the unstaged list
- **Stage All** — stage all unstaged files at once
- **Unstage Selected** — move staged files back to unstaged
- **Discard Selected** — permanently discard local changes in selected files (with confirmation)
- **Ignore Selected** — add selected files to `.gitignore`
- **Commit message field** — press `Enter` to commit instantly
- **Amend last commit** — checkbox to amend the previous commit (warns if already pushed)
- **Multi-select** — hold `Ctrl` or `Shift` to select multiple files

### Diff Viewer
- **File diff** — click any file in the CHANGED FILES panel to view its diff
- **Syntax-colored diff** — added lines in green, removed lines in red, hunk headers in blue

### File History
- **Per-file history** — right-click any file in CHANGED FILES → "File History" to see all commits that touched that file

### Tags
- **Create tag** — tag the current HEAD commit with a custom name
- **Delete tag** — remove a tag via right-click context menu

### Stash
- **Stash Save** — stash all current uncommitted changes
- **Stash Pop** — apply and remove the latest stash
- **Stash List** — view all saved stashes

### Push / Pull / Fetch
- **Push** — push current branch to remote
- **Pull** — pull latest changes from remote
- **Fetch** — fetch remote changes in the background without merging
- **Credential storage** — username and Personal Access Token (PAT) are saved locally so you don't re-enter them each time

### Reset & Revert
- **Soft reset** — move HEAD to a commit, keep all changes staged
- **Mixed reset** — move HEAD to a commit, keep changes unstaged
- **Hard reset** — move HEAD to a commit and discard all changes (with confirmation)
- **Revert commit** — create a new commit that undoes a previous commit

### Cherry-pick
- **Cherry-pick** — copy any commit from history onto the current branch via right-click

### Conflict Resolution
- **Conflict detector** — automatically detects merge conflicts after a merge/pull
- **Conflicts button** — appears in the toolbar only when conflicts exist
- **Accept Ours** — resolve a conflicted file by keeping the current branch version
- **Accept Theirs** — resolve a conflicted file by taking the incoming version
- **Mark Resolved** — manually mark a file as resolved after editing
- **Abort Merge** — cancel the entire merge and restore previous state

### Git Config
- **Local config editor** — set `user.name` and `user.email` for the current repository

### Keyboard Shortcuts
| Shortcut | Action |
|---|---|
| `Ctrl+O` | Open repository |
| `Ctrl+R` | Refresh |
| `Ctrl+F` | Focus search bar |
| `Ctrl+Shift+C` | Open Changes window |

### Other
- **Singleton instance** — only one GitHive window can run at a time (port lock on 54321)
- **Welcome screen** — shown when no repo is loaded, with quick-access buttons and recent repos
- **Status bar** — displays current operation status and active branch name at the bottom

---

## Tech Stack

| Technology | Version | Purpose |
|---|---|---|
| Java | 21 | Core language |
| JavaFX | 21.0.4 | UI framework |
| AtlantaFX | 2.0.1 | NordDark theme |
| JGit | 6.9.0 | Git operations |
| Ikonli | 12.3.1 | Icon library |
| Maven | 3.x | Build tool |

---

## Getting Started

### Requirements
- JDK 21 or newer
- Maven 3.x (or use the Maven bundled in your IDE)

### Run from source

```bash
git clone https://github.com/your-username/GitHive.git
cd GitHive
mvn javafx:run
```

### Build standalone `.exe` (Windows)

The `dist/` folder is not included in the repository. To generate a self-contained Windows executable (no Java installation required on the target machine):

```bash
# 1. Build the fat JAR
mvn package

# 2. Create app-image with bundled JRE
jpackage ^
  --type app-image ^
  --name GitHive ^
  --input target ^
  --main-jar githive-shaded.jar ^
  --icon src/main/resources/com/githive/images/hive.ico ^
  --dest dist ^
  --app-version 1.0.0
```

The output will be in `dist/GitHive/GitHive.exe`. Distribute the entire `dist/GitHive/` folder — the `runtime/` subfolder contains the bundled JRE and is required.

---

## Project Structure

```
src/main/java/com/githive/
├── Launcher.java                   # Entry point
├── MainApp.java                    # Bootstrap, splash screen, singleton lock
├── controller/
│   ├── MainController.java         # Main window logic
│   ├── ChangesController.java      # Staging/commit window logic
│   └── GraphCell.java              # Custom cell for rendering the commit graph
├── model/
│   ├── CommitInfo.java             # Commit data record
│   └── GraphRow.java               # Graph lane data per commit row
└── service/
    ├── GitService.java             # All JGit operations
    ├── GraphLayoutService.java     # Commit graph lane computation
    ├── CredentialsService.java     # Local credential storage
    └── RecentReposService.java     # Recent repository list persistence

src/main/resources/com/githive/
├── views/
│   ├── main.fxml                   # Main window layout
│   └── changes.fxml                # Changes/staging window layout
├── css/
│   └── app.css                     # Application stylesheet
└── images/
    ├── hive.png                    # App icon (PNG)
    └── hive.ico                    # App icon (ICO, for Windows exe)
```

---

## License

MIT
