#!/bin/bash
# PostToolUse hook: flags when source files (not CLAUDE.md) are edited
INPUT=$(cat)
SESSION_ID=$(echo "$INPUT" | jq -r '.session_id // empty')
FILE_PATH=$(echo "$INPUT" | jq -r '.tool_input.file_path // empty')

# Don't flag CLAUDE.md edits
if echo "$FILE_PATH" | grep -q "CLAUDE\.md"; then
    exit 0
fi

touch "/tmp/claude_edited_${SESSION_ID}"
exit 0
