#!/usr/bin/env bash
# find_string_usage.sh — find where Android string keys are used in code
# Usage:
#   Single key:  ./find_string_usage.sh <key>
#   Batch mode:  ./find_string_usage.sh --batch <keys_file>

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
KT_DIR="$REPO_ROOT/WooCommerce/src/main/kotlin"
LAYOUT_DIR="$REPO_ROOT/WooCommerce/src/main/res/layout"
XML_DIR="$REPO_ROOT/WooCommerce/src/main/res/xml"

# Maximum keys per grep chunk (avoids regex engine / arg-list limits)
CHUNK_SIZE=200

# ---------------------------------------------------------------------------
# Single key mode
# ---------------------------------------------------------------------------
single_key_mode() {
    local key="$1"
    local found=0

    echo "=== Usages of '$key' ==="

    # Search Kotlin/Java files for R.string.<key>
    local kt_results
    kt_results=$(grep -rn --include="*.kt" --include="*.java" \
        -B1 -A1 "R\.string\.${key}\b" "$KT_DIR" 2>/dev/null | head -n 50 || true)
    if [ -n "$kt_results" ]; then
        echo "$kt_results"
        found=1
    fi

    # Search XML files for @string/<key>
    for dir in "$LAYOUT_DIR" "$XML_DIR"; do
        [ -d "$dir" ] || continue
        local xml_results
        xml_results=$(grep -rn --include="*.xml" \
            -B1 -A1 "@string/${key}\b" "$dir" 2>/dev/null | head -n 50 || true)
        if [ -n "$xml_results" ]; then
            echo "$xml_results"
            found=1
        fi
    done

    if [ "$found" -eq 0 ]; then
        echo "(no usages found)"
    fi
}

# ---------------------------------------------------------------------------
# Batch mode
# ---------------------------------------------------------------------------
batch_mode() {
    local keys_file="$1"

    if [ ! -f "$keys_file" ]; then
        echo "Error: keys file '$keys_file' not found" >&2
        exit 1
    fi

    # Read keys into a temp file stripped of blank lines / comments
    local clean_keys
    clean_keys=$(mktemp)
    grep -v '^\s*$' "$keys_file" | grep -v '^\s*#' > "$clean_keys"

    local total
    total=$(wc -l < "$clean_keys" | tr -d ' ')

    if [ "$total" -eq 0 ]; then
        rm -f "$clean_keys"
        echo "{}"
        return
    fi

    # Collect all grep hits into a temp file: one hit per line, tab-separated
    # format:  KEY\tFILE:LINENO:TEXT
    local hits_file
    hits_file=$(mktemp)

    local offset=0
    while [ "$offset" -lt "$total" ]; do
        # Extract a chunk of keys using sed (bash-3 compatible)
        local chunk_keys
        chunk_keys=$(sed -n "$((offset+1)),$((offset+CHUNK_SIZE))p" "$clean_keys")

        # Build alternation for this chunk: (key1|key2|...)
        local alt
        alt=$(printf '%s' "$chunk_keys" | tr '\n' '|' | sed 's/|$//')
        alt="($alt)"

        # --- Kotlin / Java grep ---
        grep -rn -E --include="*.kt" --include="*.java" \
            "R\.string\.$alt" "$KT_DIR" 2>/dev/null \
        | while IFS=: read -r filepath lineno text; do
            local key
            key=$(printf '%s' "$text" | grep -oE "R\.string\.[A-Za-z0-9_]+" | head -1 | sed 's/R\.string\.//')
            [ -n "$key" ] && printf '%s\t%s:%s:%s\n' "$key" "$filepath" "$lineno" "$text"
        done >> "$hits_file" || true

        # --- XML grep ---
        for dir in "$LAYOUT_DIR" "$XML_DIR"; do
            [ -d "$dir" ] || continue
            grep -rn -E --include="*.xml" \
                "@string/$alt" "$dir" 2>/dev/null \
            | while IFS=: read -r filepath lineno text; do
                local key
                key=$(printf '%s' "$text" | grep -oE "@string/[A-Za-z0-9_]+" | head -1 | sed 's|@string/||')
                [ -n "$key" ] && printf '%s\t%s:%s:%s\n' "$key" "$filepath" "$lineno" "$text"
            done >> "$hits_file" || true
        done

        offset=$((offset + CHUNK_SIZE))
    done

    rm -f "$clean_keys"

    # Use Python to assemble the JSON map (max 3 snippets per key)
    python3 - "$hits_file" <<'PYEOF'
import sys, json, collections

hits_file = sys.argv[1]
result = collections.OrderedDict()
counts = collections.defaultdict(int)
MAX_PER_KEY = 3

with open(hits_file) as f:
    for line in f:
        line = line.rstrip('\n')
        if '\t' not in line:
            continue
        key, snippet = line.split('\t', 1)
        if counts[key] < MAX_PER_KEY:
            if key not in result:
                result[key] = snippet
            else:
                result[key] += '\n' + snippet
            counts[key] += 1

print(json.dumps(result, indent=2, ensure_ascii=False))
PYEOF

    rm -f "$hits_file"
}

# ---------------------------------------------------------------------------
# Entry point
# ---------------------------------------------------------------------------
if [ $# -eq 0 ]; then
    echo "Usage:"
    echo "  $0 <string_key>            — single key mode"
    echo "  $0 --batch <keys_file>     — batch mode (outputs JSON)"
    exit 1
fi

if [ "$1" = "--batch" ]; then
    if [ $# -lt 2 ]; then
        echo "Error: --batch requires a keys file argument" >&2
        exit 1
    fi
    batch_mode "$2"
else
    single_key_mode "$1"
fi
