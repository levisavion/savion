#!/bin/bash

if [ -z "$1" ]; then
  echo "Usage: $0 <repo_name>"
  exit 1
fi

REPO_NAME=$1

if [ -d "$REPO_NAME" ]; then
  echo "Directory $REPO_NAME already exists."
  exit 1
fi

mkdir "$REPO_NAME"
cd "$REPO_NAME"
git init
echo "# $REPO_NAME" > README.md
git add README.md
git commit -m "Initial commit"

echo "Repository $REPO_NAME created and initialized."
