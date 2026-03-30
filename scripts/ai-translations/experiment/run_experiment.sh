#!/usr/bin/env bash
# run_experiment.sh — orchestrate all translation experiment runs
# Runs all combinations of strategy x LLM x language (64 total by default)
# Usage: ./run_experiment.sh [--languages de,ja] [--strategy naive|contextual] [--llm claude|codex] [--dry-run]

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
RESULTS_BASE="$SCRIPT_DIR/../../results/translations"

# Language map using parallel arrays (macOS ships bash 3, no associative arrays)
LANG_CODES="ar de es fr he id it ja ko nl pt-rBR ru sv tr zh-rCN zh-rTW"
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
        *)      echo "Unknown" ;;
    esac
}

# Defaults: run everything
FILTER_LANGUAGES=""
FILTER_STRATEGY=""
FILTER_LLM=""
DRY_RUN=0

# Parse flags
while [ $# -gt 0 ]; do
    case "$1" in
        --languages)
            FILTER_LANGUAGES="$2"
            shift 2
            ;;
        --strategy)
            FILTER_STRATEGY="$2"
            shift 2
            ;;
        --llm)
            FILTER_LLM="$2"
            shift 2
            ;;
        --dry-run)
            DRY_RUN=1
            shift
            ;;
        *)
            echo "Unknown option: $1" >&2
            echo "Usage: $0 [--languages de,ja] [--strategy naive|contextual] [--llm claude|codex] [--dry-run]" >&2
            exit 1
            ;;
    esac
done

# Build active language list
if [ -n "$FILTER_LANGUAGES" ]; then
    ACTIVE_LANGS=$(echo "$FILTER_LANGUAGES" | tr ',' ' ')
else
    ACTIVE_LANGS="$LANG_CODES"
fi

# Build active strategy list
if [ -n "$FILTER_STRATEGY" ]; then
    ACTIVE_STRATEGIES="$FILTER_STRATEGY"
else
    ACTIVE_STRATEGIES="naive contextual"
fi

# Build active LLM list
if [ -n "$FILTER_LLM" ]; then
    ACTIVE_LLMS="$FILTER_LLM"
else
    ACTIVE_LLMS="claude codex"
fi

# Count total combinations
TOTAL=0
for _strategy in $ACTIVE_STRATEGIES; do
    for _llm in $ACTIVE_LLMS; do
        for _lang in $ACTIVE_LANGS; do
            TOTAL=$((TOTAL + 1))
        done
    done
done

SUCCEEDED=0
FAILED=0
SKIPPED=0
COUNTER=0

for STRATEGY in $ACTIVE_STRATEGIES; do
    for LLM in $ACTIVE_LLMS; do
        for LANG_CODE in $ACTIVE_LANGS; do
            COUNTER=$((COUNTER + 1))
            LANG_NAME=$(lang_name "$LANG_CODE")
            RESULTS_FILE="$RESULTS_BASE/$STRATEGY/$LLM/${LANG_CODE}.json"

            if [ "$DRY_RUN" -eq 1 ]; then
                echo "[dry-run] [$STRATEGY/$LLM] $LANG_CODE ($LANG_NAME) -> $RESULTS_FILE  ($COUNTER/$TOTAL)"
                continue
            fi

            if [ -f "$RESULTS_FILE" ]; then
                echo "[skip] [$STRATEGY/$LLM] $LANG_CODE — results already exist ($COUNTER/$TOTAL)"
                SKIPPED=$((SKIPPED + 1))
                continue
            fi

            echo "[run] [$STRATEGY/$LLM] $LANG_CODE ($LANG_NAME) ... ($COUNTER/$TOTAL)"

            TRANSLATE_SCRIPT="$SCRIPT_DIR/translate_${STRATEGY}.sh"
            if bash "$TRANSLATE_SCRIPT" "$LANG_NAME" "$LANG_CODE" "$LLM"; then
                echo "[done] [$STRATEGY/$LLM] $LANG_CODE ($COUNTER/$TOTAL)"
                SUCCEEDED=$((SUCCEEDED + 1))
            else
                echo "[fail] [$STRATEGY/$LLM] $LANG_CODE ($COUNTER/$TOTAL)" >&2
                FAILED=$((FAILED + 1))
            fi
        done
    done
done

if [ "$DRY_RUN" -eq 1 ]; then
    echo ""
    echo "Dry run complete. Would execute $TOTAL combinations."
else
    echo ""
    echo "Experiment complete."
    echo "  Succeeded: $SUCCEEDED"
    echo "  Failed:    $FAILED"
    echo "  Skipped:   $SKIPPED"
    echo "  Total:     $TOTAL"
fi
