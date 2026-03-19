#!/bin/bash
# Stop hook: blocks if .proto files were edited but API.md was not updated
INPUT=$(cat)
SESSION_ID=$(echo "$INPUT" | jq -r '.session_id // empty')

PROTO_MARKER="/tmp/claude_proto_edited_${SESSION_ID}"

if [ -f "$PROTO_MARKER" ]; then
    rm -f "$PROTO_MARKER"
    echo '{"decision":"block","reason":"Proto files were modified. Update src/main/proto/API.md to reflect the proto changes before finishing."}'
else
    exit 0
fi
