#!/usr/bin/env bash
# translate_contextual.sh — translate Android strings using context-enriched LLM prompt
# Usage: ./translate_contextual.sh <language_name> <language_code> <llm>
# Example: ./translate_contextual.sh German de claude

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PYTHON="$SCRIPT_DIR/../venv/bin/python3"
STRINGS_XML="$SCRIPT_DIR/../../WooCommerce/src/main/res/values/strings.xml"
PROMPT_TEMPLATE="$SCRIPT_DIR/prompts/contextual.txt"
FIND_USAGE_SCRIPT="$SCRIPT_DIR/find_string_usage.sh"

if [ $# -lt 3 ]; then
    echo "Usage: $0 <language_name> <language_code> <llm>" >&2
    echo "Example: $0 German de claude" >&2
    exit 1
fi

LANG_NAME="$1"
LANG_CODE="$2"
LLM="$3"

OUT_DIR="$SCRIPT_DIR/../results/translations/contextual/$LLM"
mkdir -p "$OUT_DIR"
OUT_FILE="$OUT_DIR/${LANG_CODE}.json"

# Extract all strings to a temp file
ALL_STRINGS_FILE=$(mktemp /tmp/strings_all_XXXXXX.json)
"$PYTHON" "$SCRIPT_DIR/parse_strings.py" extract "$STRINGS_XML" > "$ALL_STRINGS_FILE"

# Extract keys to a temp file for batch usage lookup
KEYS_FILE=$(mktemp /tmp/string_keys_XXXXXX.txt)
"$PYTHON" "$SCRIPT_DIR/parse_strings.py" extract-keys "$STRINGS_XML" > "$KEYS_FILE"

# Run batch usage lookup for all strings in one pass
echo "[$LLM/$LANG_CODE] running batch context lookup ..."
CONTEXT_JSON_FILE=$(mktemp /tmp/context_XXXXXX.json)
bash "$FIND_USAGE_SCRIPT" --batch "$KEYS_FILE" > "$CONTEXT_JSON_FILE"
echo "[$LLM/$LANG_CODE] context lookup done"

# Merge context into the strings array
STRINGS_WITH_CONTEXT_FILE=$(mktemp /tmp/strings_ctx_XXXXXX.json)
"$PYTHON" - "$ALL_STRINGS_FILE" "$CONTEXT_JSON_FILE" "$STRINGS_WITH_CONTEXT_FILE" <<'PYEOF'
import json, sys

strings_file = sys.argv[1]
context_file = sys.argv[2]
out_file = sys.argv[3]

with open(strings_file) as f:
    strings = json.load(f)

with open(context_file) as f:
    context_map = json.load(f)

enriched = []
for entry in strings:
    e = dict(entry)
    key = e.get("key", "")
    if key in context_map:
        e["context"] = context_map[key]
    enriched.append(e)

with open(out_file, "w") as f:
    json.dump(enriched, f, ensure_ascii=False, indent=2)
PYEOF

# Split into chunks of 200
CHUNKS_DIR=$(mktemp -d /tmp/chunks_XXXXXX)
"$PYTHON" - "$STRINGS_WITH_CONTEXT_FILE" "$CHUNKS_DIR" <<'PYEOF'
import json, sys, os

strings_file = sys.argv[1]
chunks_dir = sys.argv[2]
chunk_size = 200

with open(strings_file) as f:
    strings = json.load(f)

chunks = [strings[i:i+chunk_size] for i in range(0, len(strings), chunk_size)]
for idx, chunk in enumerate(chunks):
    chunk_path = os.path.join(chunks_dir, f"chunk_{idx:04d}.json")
    with open(chunk_path, "w") as f:
        json.dump(chunk, f, ensure_ascii=False, indent=2)
PYEOF

TOTAL_CHUNKS=$("$PYTHON" - "$STRINGS_WITH_CONTEXT_FILE" <<'PYEOF'
import json, sys, math
with open(sys.argv[1]) as f:
    strings = json.load(f)
print(math.ceil(len(strings) / 200))
PYEOF
)

# Read the prompt template once
PROMPT_TEMPLATE_CONTENT=$(cat "$PROMPT_TEMPLATE")

# Translate each chunk
RESULTS_DIR=$(mktemp -d /tmp/results_XXXXXX)

CHUNK_INDEX=0
for CHUNK_FILE in "$CHUNKS_DIR"/chunk_*.json; do
    CHUNK_INDEX=$((CHUNK_INDEX + 1))
    echo "[$LLM/$LANG_CODE] chunk $CHUNK_INDEX/$TOTAL_CHUNKS ..."

    STRINGS_JSON=$(cat "$CHUNK_FILE")

    # Build the prompt by substituting placeholders
    PROMPT="${PROMPT_TEMPLATE_CONTENT/\{LANGUAGE\}/$LANG_NAME}"
    PROMPT="${PROMPT/\{STRINGS_WITH_CONTEXT_JSON\}/$STRINGS_JSON}"

    RESULT_FILE="$RESULTS_DIR/result_$(printf '%04d' $CHUNK_INDEX).json"

    # Call the LLM
    LLM_RESPONSE=""
    if [ "$LLM" = "claude" ]; then
        LLM_RESPONSE=$(echo "$PROMPT" | claude --print --no-input 2>/dev/null) || {
            echo "[WARN] chunk $CHUNK_INDEX failed — skipping" >&2
            continue
        }
    elif [ "$LLM" = "codex" ]; then
        CODEX_OUT=$(mktemp /tmp/codex_output_XXXXXX.txt)
        codex exec -o "$CODEX_OUT" "$PROMPT" 2>/dev/null && LLM_RESPONSE=$(cat "$CODEX_OUT") || {
            echo "[WARN] chunk $CHUNK_INDEX failed — skipping" >&2
            rm -f "$CODEX_OUT"
            continue
        }
        rm -f "$CODEX_OUT"
    else
        echo "Error: unknown LLM '$LLM'. Supported: claude, codex" >&2
        exit 1
    fi

    # Extract the JSON array from the response (strip markdown fences and surrounding text)
    EXTRACTED=$("$PYTHON" - "$LLM_RESPONSE" <<'PYEOF'
import sys, re, json

response = sys.argv[1]

# Try to find a JSON array in the response
# Handle markdown code fences
response = re.sub(r'```(?:json)?\s*', '', response)
response = re.sub(r'```', '', response)

# Find the outermost [ ... ]
start = response.find('[')
if start == -1:
    print("[]")
    sys.exit(0)

depth = 0
end = -1
for i, ch in enumerate(response[start:], start):
    if ch == '[':
        depth += 1
    elif ch == ']':
        depth -= 1
        if depth == 0:
            end = i
            break

if end == -1:
    print("[]")
    sys.exit(0)

candidate = response[start:end+1]
try:
    parsed = json.loads(candidate)
    print(json.dumps(parsed, ensure_ascii=False))
except json.JSONDecodeError:
    print("[]")
PYEOF
) || {
        echo "[WARN] chunk $CHUNK_INDEX: failed to extract JSON — skipping" >&2
        continue
    }

    echo "$EXTRACTED" > "$RESULT_FILE"
    echo "[$LLM/$LANG_CODE] chunk $CHUNK_INDEX/$TOTAL_CHUNKS done"
done

# Merge all result chunks into one JSON array
"$PYTHON" - "$RESULTS_DIR" "$OUT_FILE" <<'PYEOF'
import json, os, sys, glob

results_dir = sys.argv[1]
out_file = sys.argv[2]

merged = []
for path in sorted(glob.glob(os.path.join(results_dir, "result_*.json"))):
    try:
        with open(path) as f:
            chunk = json.load(f)
        if isinstance(chunk, list):
            merged.extend(chunk)
    except Exception as e:
        print(f"[WARN] could not read {path}: {e}", file=sys.stderr)

with open(out_file, "w") as f:
    json.dump(merged, f, ensure_ascii=False, indent=2)

print(f"Wrote {len(merged)} translations to {out_file}")
PYEOF

# Cleanup
rm -f "$ALL_STRINGS_FILE" "$KEYS_FILE" "$CONTEXT_JSON_FILE" "$STRINGS_WITH_CONTEXT_FILE"
rm -rf "$CHUNKS_DIR" "$RESULTS_DIR"

echo "Done. Output: $OUT_FILE"
