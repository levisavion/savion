# Workspace Notes

This repository is a minimal workspace used for small tasks and examples.

## How to add a new repo

You have two common options, depending on whether the repo already exists.

### 1) Clone an existing repo

```bash
git clone <git-url> <folder-name>
```

### 2) Create a brand new repo and push it

```bash
mkdir <folder-name>
cd <folder-name>
git init
echo "# <repo-name>" > README.md
git add README.md
git commit -m "Initial commit"

# Option A: create via GitHub CLI and push
gh repo create <owner>/<repo-name> --source=. --public --push

# Option B: or add a remote manually and push
git remote add origin <git-url>
git push -u origin main
```

### 3) If you need another repo inside this repo

Since this workspace is already a git repo, nesting another repo inside it is
usually not recommended. Use a submodule instead:

```bash
git submodule add <git-url> <path>
```
