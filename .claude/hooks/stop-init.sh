#!/bin/bash
# Stop hook: triggers /init to update CLAUDE.md if source files were edited
INPUT=$(cat)
SESSION_ID=$(echo "$INPUT" | jq -r '.session_id // empty')

EDIT_MARKER="/tmp/claude_edited_${SESSION_ID}"
INIT_MARKER="/tmp/claude_init_done_${SESSION_ID}"

if [ -f "$EDIT_MARKER" ] && [ ! -f "$INIT_MARKER" ]; then
    touch "$INIT_MARKER"
    echo '{"decision":"block","reason":"Source files were modified. Run /init to update CLAUDE.md before finishing."}'
else
    exit 0
fi
