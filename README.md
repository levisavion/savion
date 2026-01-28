# Workspace

This workspace contains tools for managing repositories.

## How to add a new repo

To add a new repository, you can use the `add_repo.sh` script.

### Usage

```bash
./add_repo.sh <repo_name>
```

Example:

```bash
./add_repo.sh my-new-project
```

This will:
1. Create a directory named `my-new-project`.
2. Initialize a git repository inside it.
3. Create a `README.md` file.
4. Make an initial commit.
