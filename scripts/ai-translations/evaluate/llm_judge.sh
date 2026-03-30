#!/usr/bin/env bash
# llm_judge.sh — LLM-as-judge evaluation for AI translations
# Usage: ./llm_judge.sh <ai_translations_dir> <human_translations_dir> <output_file> [--sample-size N] [--languages lang1,lang2]
# Example: ./llm_judge.sh results/translations/contextual/claude/ results/human/ results/llm_judge_scores.json

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PYTHON="$SCRIPT_DIR/../venv/bin/python3"
REPO_ROOT="$SCRIPT_DIR/../../.."
STRINGS_XML="$REPO_ROOT/WooCommerce/src/main/res/values/strings.xml"
PARSE_STRINGS="$SCRIPT_DIR/../experiment/parse_strings.py"

if [ $# -lt 3 ]; then
    echo "Usage: $0 <ai_translations_dir> <human_translations_dir> <output_file> [--sample-size N] [--languages lang1,lang2]" >&2
    echo "Example: $0 results/translations/contextual/claude/ results/human/ results/llm_judge_scores.json" >&2
    exit 1
fi

AI_DIR="$1"
HUMAN_DIR="$2"
OUTPUT_FILE="$3"
shift 3

SAMPLE_SIZE=100
FILTER_LANGUAGES=""

while [ $# -gt 0 ]; do
    case "$1" in
        --sample-size)
            SAMPLE_SIZE="$2"
            shift 2
            ;;
        --languages)
            FILTER_LANGUAGES="$2"
            shift 2
            ;;
        *)
            echo "Unknown option: $1" >&2
            exit 1
            ;;
    esac
done

# Map locale code to human-readable language name (same as run_experiment.sh)
lang_name() {
    case "$1" in
        ar)     echo "Arabic" ;;
        de)     echo "German" ;;
        es)     echo "Spanish" ;;
        fr)     echo "French" ;;
        he)     echo "Hebrew" ;;
        id)     echo "Indonesian" ;;
        it)     echo "Italian" ;;
        ja)     echo "Japanese" ;;
        ko)     echo "Korean" ;;
        nl)     echo "Dutch" ;;
        pt-rBR) echo "Portuguese (Brazil)" ;;
        ru)     echo "Russian" ;;
        sv)     echo "Swedish" ;;
        tr)     echo "Turkish" ;;
        zh-rCN) echo "Chinese (Simplified)" ;;
        zh-rTW) echo "Chinese (Traditional)" ;;
        *)      echo "$1" ;;
    esac
}

# Map locale code to Android resource directory name
locale_to_res_dir() {
    echo "values-$1"
}

# Extract JSON from LLM response (strip markdown fences, find outermost [...])
extract_json_array() {
    local response="$1"
    "$PYTHON" - "$response" <<'PYEOF'
import sys, re, json

response = sys.argv[1]

response = re.sub(r'```(?:json)?\s*', '', response)
response = re.sub(r'```', '', response)

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
}

# Collect language codes from AI dir JSON files
collect_languages() {
    local dir="$1"
    for f in "$dir"/*.json; do
        [ -f "$f" ] || continue
        basename "$f" .json
    done
}

# Extract English source strings to a temp file (reused across all languages)
ENGLISH_JSON=$(mktemp /tmp/judge_english_XXXXXX.json)
"$PYTHON" "$PARSE_STRINGS" extract "$STRINGS_XML" > "$ENGLISH_JSON"

# Accumulate per-language results in a temp dir
LANG_RESULTS_DIR=$(mktemp -d /tmp/judge_lang_results_XXXXXX)

# Build list of languages to evaluate
if [ -n "$FILTER_LANGUAGES" ]; then
    ACTIVE_LANGS=$(echo "$FILTER_LANGUAGES" | tr ',' ' ')
else
    ACTIVE_LANGS=$(collect_languages "$AI_DIR")
fi

for LANG_CODE in $ACTIVE_LANGS; do
    AI_FILE="$AI_DIR/${LANG_CODE}.json"
    if [ ! -f "$AI_FILE" ]; then
        echo "[judge] $LANG_CODE — AI file not found, skipping" >&2
        continue
    fi

    LANG_NAME=$(lang_name "$LANG_CODE")
    RES_DIR=$(locale_to_res_dir "$LANG_CODE")
    HUMAN_XML="$REPO_ROOT/WooCommerce/src/main/res/$RES_DIR/strings.xml"

    # Try to get human reference from provided human_translations_dir first, fall back to XML
    HUMAN_JSON=""
    HUMAN_JSON_FILE=$(mktemp /tmp/judge_human_XXXXXX.json)

    if [ -f "$HUMAN_DIR/${LANG_CODE}.json" ]; then
        cp "$HUMAN_DIR/${LANG_CODE}.json" "$HUMAN_JSON_FILE"
        HUMAN_JSON="$HUMAN_JSON_FILE"
    elif [ -f "$HUMAN_XML" ]; then
        "$PYTHON" "$PARSE_STRINGS" extract "$HUMAN_XML" > "$HUMAN_JSON_FILE"
        HUMAN_JSON="$HUMAN_JSON_FILE"
    else
        echo "[judge] $LANG_CODE — no human reference found (tried $HUMAN_DIR/${LANG_CODE}.json and $HUMAN_XML), skipping" >&2
        rm -f "$HUMAN_JSON_FILE"
        continue
    fi

    echo "[judge] $LANG_CODE ($LANG_NAME) — sampling $SAMPLE_SIZE strings ..."

    # Sample strings that exist in both AI and human files, build batch input JSON
    SAMPLE_JSON=$(mktemp /tmp/judge_sample_XXXXXX.json)
    "$PYTHON" - "$AI_FILE" "$HUMAN_JSON" "$ENGLISH_JSON" "$SAMPLE_SIZE" "$SAMPLE_JSON" <<'PYEOF'
import json, sys, random

ai_file     = sys.argv[1]
human_file  = sys.argv[2]
english_file= sys.argv[3]
sample_size = int(sys.argv[4])
out_file    = sys.argv[5]

with open(ai_file) as f:
    ai_list = json.load(f)
with open(human_file) as f:
    human_list = json.load(f)
with open(english_file) as f:
    english_list = json.load(f)

def get_text(entry):
    if entry.get("type") == "plurals":
        items = entry.get("items", {})
        return " | ".join(items[q] for q in sorted(items))
    return entry.get("value", "")

ai_map      = {e["key"]: get_text(e) for e in ai_list}
human_map   = {e["key"]: get_text(e) for e in human_list}
english_map = {e["key"]: get_text(e) for e in english_list}

common_keys = [k for k in ai_map if k in human_map and ai_map[k] and human_map[k]]
random.shuffle(common_keys)
sampled = common_keys[:sample_size]

result = []
for key in sampled:
    result.append({
        "key": key,
        "english": english_map.get(key, ""),
        "ai": ai_map[key],
        "human": human_map[key],
    })

with open(out_file, "w") as f:
    json.dump(result, f, ensure_ascii=False, indent=2)

print(len(result))
PYEOF

    SAMPLED_COUNT=$("$PYTHON" -c "import json,sys; data=json.load(open('$SAMPLE_JSON')); print(len(data))")
    echo "[judge] $LANG_CODE — got $SAMPLED_COUNT strings to judge"

    if [ "$SAMPLED_COUNT" -eq 0 ]; then
        echo "[judge] $LANG_CODE — no overlapping strings found, skipping" >&2
        rm -f "$HUMAN_JSON_FILE" "$SAMPLE_JSON"
        continue
    fi

    # Split sample into batches of 20 and call Claude for each batch
    BATCH_RESULTS_DIR=$(mktemp -d /tmp/judge_batches_XXXXXX)

    "$PYTHON" - "$SAMPLE_JSON" "$BATCH_RESULTS_DIR" <<'PYEOF'
import json, sys, os

sample_file  = sys.argv[1]
batches_dir  = sys.argv[2]
batch_size   = 20

with open(sample_file) as f:
    items = json.load(f)

batches = [items[i:i+batch_size] for i in range(0, len(items), batch_size)]
for idx, batch in enumerate(batches):
    path = os.path.join(batches_dir, f"batch_{idx:04d}.json")
    with open(path, "w") as f:
        json.dump(batch, f, ensure_ascii=False, indent=2)
PYEOF

    LANG_ALL_RATINGS=()
    BATCH_INDEX=0

    for BATCH_FILE in "$BATCH_RESULTS_DIR"/batch_*.json; do
        BATCH_INDEX=$((BATCH_INDEX + 1))

        TRANSLATIONS_BLOCK=$("$PYTHON" - "$BATCH_FILE" "$LANG_NAME" <<'PYEOF'
import json, sys

batch_file = sys.argv[1]
lang_name  = sys.argv[2]

with open(batch_file) as f:
    batch = json.load(f)

lines = []
for item in batch:
    lines.append(f'Key: "{item["key"]}"')
    lines.append(f'English: "{item["english"]}"')
    lines.append(f'AI Translation: "{item["ai"]}"')
    lines.append(f'Human Reference: "{item["human"]}"')
    lines.append("")

print("\n".join(lines).rstrip())
PYEOF
)

        PROMPT="You are evaluating translations from English to ${LANG_NAME} for a mobile e-commerce app.

For each translation below, rate the AI translation on three dimensions (1-5 scale):
- accuracy: Does it convey the same meaning as the English? (5=perfect, 1=wrong meaning)
- naturalness: Does it sound natural to a native speaker? (5=native quality, 1=clearly machine)
- length_fit: Is it a similar length to the English original? (5=same length, 1=much longer/shorter)

Return a JSON array of objects: [{\"key\": \"...\", \"accuracy\": N, \"naturalness\": N, \"length_fit\": N}, ...]

Translations to evaluate:
${TRANSLATIONS_BLOCK}"

        RESPONSE=$(echo "$PROMPT" | claude --print --no-input 2>/dev/null) || {
            echo "[WARN] $LANG_CODE batch $BATCH_INDEX failed — skipping" >&2
            continue
        }

        RATINGS=$(extract_json_array "$RESPONSE") || {
            echo "[WARN] $LANG_CODE batch $BATCH_INDEX: could not extract JSON — skipping" >&2
            continue
        }

        echo "$RATINGS" > "$BATCH_RESULTS_DIR/ratings_$(printf '%04d' $BATCH_INDEX).json"
        echo "[judge] $LANG_CODE batch $BATCH_INDEX done"
    done

    # Merge all batch ratings for this language
    LANG_RESULT_FILE="$LANG_RESULTS_DIR/${LANG_CODE}.json"
    "$PYTHON" - "$BATCH_RESULTS_DIR" "$LANG_RESULT_FILE" <<'PYEOF'
import json, sys, os, glob

batches_dir = sys.argv[1]
out_file    = sys.argv[2]

all_ratings = []
for path in sorted(glob.glob(os.path.join(batches_dir, "ratings_*.json"))):
    try:
        with open(path) as f:
            chunk = json.load(f)
        if isinstance(chunk, list):
            all_ratings.extend(chunk)
    except Exception as e:
        print(f"[WARN] could not read {path}: {e}", file=sys.stderr)

with open(out_file, "w") as f:
    json.dump(all_ratings, f, ensure_ascii=False, indent=2)

print(len(all_ratings))
PYEOF

    echo "[judge] $LANG_CODE — saved $(cat "$LANG_RESULT_FILE" | "$PYTHON" -c 'import json,sys; print(len(json.load(sys.stdin)))') ratings"

    # Cleanup per-language temps
    rm -f "$HUMAN_JSON_FILE" "$SAMPLE_JSON"
    rm -rf "$BATCH_RESULTS_DIR"
done

# Aggregate all language results into the final output JSON
echo "[judge] aggregating results -> $OUTPUT_FILE ..."

mkdir -p "$(dirname "$OUTPUT_FILE")"

"$PYTHON" - "$LANG_RESULTS_DIR" "$OUTPUT_FILE" "$SAMPLE_SIZE" <<'PYEOF'
import json, sys, os, glob, statistics

lang_results_dir = sys.argv[1]
out_file         = sys.argv[2]
sample_size      = int(sys.argv[3])

results = {}

for path in sorted(glob.glob(os.path.join(lang_results_dir, "*.json"))):
    lang_code = os.path.basename(path).replace(".json", "")
    try:
        with open(path) as f:
            ratings = json.load(f)
    except Exception as e:
        print(f"[WARN] could not read {path}: {e}", file=sys.stderr)
        continue

    if not ratings:
        continue

    accuracy_scores   = [r["accuracy"]   for r in ratings if "accuracy"   in r]
    naturalness_scores= [r["naturalness"] for r in ratings if "naturalness" in r]
    length_fit_scores = [r["length_fit"]  for r in ratings if "length_fit"  in r]

    def mean_or_none(lst):
        return round(statistics.mean(lst), 4) if lst else None

    results[lang_code] = {
        "per_string": ratings,
        "aggregates": {
            "accuracy_mean":    mean_or_none(accuracy_scores),
            "naturalness_mean": mean_or_none(naturalness_scores),
            "length_fit_mean":  mean_or_none(length_fit_scores),
        }
    }

output = {
    "judge": "claude",
    "sample_size_per_language": sample_size,
    "results": results,
}

with open(out_file, "w") as f:
    json.dump(output, f, ensure_ascii=False, indent=2)

print(f"Wrote results for {len(results)} language(s) to {out_file}")
PYEOF

# Cleanup
rm -f "$ENGLISH_JSON"
rm -rf "$LANG_RESULTS_DIR"

echo "Done. Output: $OUTPUT_FILE"
