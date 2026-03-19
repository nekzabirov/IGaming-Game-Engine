#!/bin/bash
# PostToolUse hook: flags when .proto files are edited (to trigger API.md update)
INPUT=$(cat)
SESSION_ID=$(echo "$INPUT" | jq -r '.session_id // empty')
FILE_PATH=$(echo "$INPUT" | jq -r '.tool_input.file_path // empty')

# Only flag .proto file edits
if echo "$FILE_PATH" | grep -qE '\.proto$'; then
    touch "/tmp/claude_proto_edited_${SESSION_ID}"
fi

# Clear the flag if API.md was just updated
if echo "$FILE_PATH" | grep -q 'src/main/proto/API\.md'; then
    rm -f "/tmp/claude_proto_edited_${SESSION_ID}"
fi

exit 0
