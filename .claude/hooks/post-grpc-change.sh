#!/bin/bash
# PostToolUse hook: remind to update GRPC_API_REFERENCE.md when proto files change.
# Receives tool use JSON on stdin.

input=$(cat)
file_path=$(echo "$input" | jq -r '.tool_input.file_path // empty' 2>/dev/null)

if [ -z "$file_path" ]; then
  exit 0
fi

case "$file_path" in
  *.proto)
    echo "Proto file was modified ($file_path). Update docs/GRPC_API_REFERENCE.md to reflect the gRPC API changes (messages, fields, services, error codes)."
    ;;
esac
