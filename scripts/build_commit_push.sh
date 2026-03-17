#!/usr/bin/env bash
set -euo pipefail

COMMIT_MESSAGE="${1:-chore: build ok $(date -u +'%Y-%m-%dT%H:%M:%SZ')}"

./gradlew assembleDebug

git add -A

if [[ -z "$(git status --porcelain)" ]]; then
  echo "No changes to commit."
  exit 0
fi

git commit -m "$COMMIT_MESSAGE"

CURRENT_BRANCH="$(git branch --show-current)"
git push origin "$CURRENT_BRANCH"
