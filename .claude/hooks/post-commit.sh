#!/bin/bash
# PostToolUse hook: remind to update RELEASE_NOTES.md after a git commit.
# Receives tool use JSON on stdin.

input=$(cat)
command=$(echo "$input" | jq -r '.tool_input.command // empty' 2>/dev/null)

if [ -z "$command" ]; then
  exit 0
fi

if echo "$command" | grep -q "git commit"; then
  echo "A git commit was created. Update RELEASE_NOTES.md with a summary of the changes included in this commit."
fi
