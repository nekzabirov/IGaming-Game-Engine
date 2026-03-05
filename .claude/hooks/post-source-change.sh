#!/bin/bash
# PostToolUse hook: remind to update CLAUDE.md and README.md when source files change.
# Receives tool use JSON on stdin.

input=$(cat)
file_path=$(echo "$input" | jq -r '.tool_input.file_path // empty' 2>/dev/null)

# Skip if no file path or if the file IS a doc file
if [ -z "$file_path" ]; then
  exit 0
fi

case "$file_path" in
  *.md) exit 0 ;;  # Don't trigger on doc file edits themselves
esac

case "$file_path" in
  *.kt|*.kts|*.proto|*/Dockerfile|*/docker-compose*)
    echo "Source code was modified ($file_path). Update CLAUDE.md and README.md if the change affects architecture, adapters, configuration, or public API."
    ;;
esac
