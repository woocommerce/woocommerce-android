#!/usr/bin/env python3
"""
Compute translation quality metrics by comparing AI translations to human ground truth.

Usage:
  python3 compute_metrics.py \\
    --ai results/translations/naive/claude/de.json \\
    --human human_de.json \\
    --output results/metrics/naive_claude_de.json \\
    [--strategy naive] [--llm claude] [--english english.json]
"""

import argparse
import json
import os
import re
import statistics
import sys

import Levenshtein
import sacrebleu


def load_json(path: str) -> list[dict]:
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)


def get_text(entry: dict) -> str:
    """Return a single string representation of an entry's translation value."""
    if entry.get("type") == "plurals":
        items = entry.get("items", {})
        return " | ".join(items[q] for q in sorted(items))
    return entry.get("value", "")


def infer_from_path(path: str, field: str) -> str:
    """Try to infer strategy or llm from a file path like results/translations/naive/claude/de.json."""
    parts = path.replace("\\", "/").split("/")
    # Look for known LLM names
    known_llms = {"claude", "codex", "gpt", "gemini", "mistral"}
    known_strategies = {"naive", "contextual"}

    for part in parts:
        name = part.lower()
        if field == "llm" and name in known_llms:
            return name
        if field == "strategy" and name in known_strategies:
            return name
    return "unknown"


def infer_language(path: str) -> str:
    basename = os.path.basename(path)
    m = re.match(r"([a-z]{2}(?:-[a-zA-Z]{2,4})?)\.json", basename)
    if m:
        return m.group(1)
    return "unknown"


def compute_metrics(
    ai_path: str,
    human_path: str,
    output_path: str,
    strategy: str | None,
    llm: str | None,
    english_path: str | None,
) -> None:
    ai_entries = load_json(ai_path)
    human_entries = load_json(human_path)

    ai_map = {e["key"]: e for e in ai_entries}
    human_map = {e["key"]: e for e in human_entries}
    english_map: dict[str, str] = {}

    if english_path:
        english_entries = load_json(english_path)
        english_map = {e["key"]: get_text(e) for e in english_entries}

    language = infer_language(ai_path)
    resolved_strategy = strategy or infer_from_path(ai_path, "strategy")
    resolved_llm = llm or infer_from_path(ai_path, "llm")

    per_string = []
    bleu_scores = []
    chrf_scores = []
    lev_scores = []

    matched = 0
    for key in ai_map:
        if key not in human_map:
            continue

        ai_text = get_text(ai_map[key])
        human_text = get_text(human_map[key])

        if not ai_text or not human_text:
            continue

        bleu = sacrebleu.sentence_bleu(ai_text, [human_text]).score
        chrf = sacrebleu.sentence_chrf(ai_text, [human_text]).score
        lev = Levenshtein.ratio(ai_text, human_text)

        entry = {
            "key": key,
            "bleu": round(bleu, 4),
            "chrf": round(chrf, 4),
            "levenshtein_sim": round(lev, 6),
            "ai_value": ai_text,
            "human_value": human_text,
        }
        if english_path:
            entry["english_value"] = english_map.get(key, "")

        per_string.append(entry)
        bleu_scores.append(bleu)
        chrf_scores.append(chrf)
        lev_scores.append(lev)
        matched += 1

    total_strings = len(ai_map)

    aggregates = {
        "bleu_mean": round(statistics.mean(bleu_scores), 4) if bleu_scores else 0.0,
        "bleu_median": round(statistics.median(bleu_scores), 4) if bleu_scores else 0.0,
        "chrf_mean": round(statistics.mean(chrf_scores), 4) if chrf_scores else 0.0,
        "chrf_median": round(statistics.median(chrf_scores), 4) if chrf_scores else 0.0,
        "levenshtein_sim_mean": round(statistics.mean(lev_scores), 6) if lev_scores else 0.0,
        "levenshtein_sim_median": round(statistics.median(lev_scores), 6) if lev_scores else 0.0,
        "total_strings": total_strings,
        "matched_strings": matched,
    }

    output = {
        "language": language,
        "strategy": resolved_strategy,
        "llm": resolved_llm,
        "per_string": per_string,
        "aggregates": aggregates,
    }

    os.makedirs(os.path.dirname(output_path), exist_ok=True) if os.path.dirname(output_path) else None
    with open(output_path, "w", encoding="utf-8") as f:
        json.dump(output, f, ensure_ascii=False, indent=2)

    print(f"Metrics written to {output_path}")
    print(f"  Matched: {matched}/{total_strings} strings")
    print(f"  BLEU mean/median:  {aggregates['bleu_mean']} / {aggregates['bleu_median']}")
    print(f"  chrF mean/median:  {aggregates['chrf_mean']} / {aggregates['chrf_median']}")
    print(f"  Lev  mean/median:  {aggregates['levenshtein_sim_mean']} / {aggregates['levenshtein_sim_median']}")


def main() -> None:
    parser = argparse.ArgumentParser(description="Compute translation quality metrics.")
    parser.add_argument("--ai", required=True, help="Path to AI translation JSON")
    parser.add_argument("--human", required=True, help="Path to human (ground truth) translation JSON")
    parser.add_argument("--output", required=True, help="Path to write metrics JSON output")
    parser.add_argument("--strategy", default=None, help="Translation strategy name (e.g. naive)")
    parser.add_argument("--llm", default=None, help="LLM name (e.g. claude)")
    parser.add_argument("--english", default=None, help="Optional path to English source JSON for per_string output")
    args = parser.parse_args()

    compute_metrics(
        ai_path=args.ai,
        human_path=args.human,
        output_path=args.output,
        strategy=args.strategy,
        llm=args.llm,
        english_path=args.english,
    )


if __name__ == "__main__":
    main()
