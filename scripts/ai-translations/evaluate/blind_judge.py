#!/usr/bin/env python3
"""Blind A/B evaluation: ask an LLM to judge AI vs Human translations without knowing which is which."""

import argparse
import json
import os
import random
import re
import subprocess
import sys
import tempfile
import time
from pathlib import Path

SCRIPT_DIR = Path(__file__).parent
REPO_ROOT = SCRIPT_DIR.parent.parent.parent

sys.path.insert(0, str(SCRIPT_DIR.parent / "experiment"))
from parse_strings import cmd_extract_to_list


def call_llm(prompt, judge_llm="claude", model=None):
    if judge_llm == "codex":
        out_file = tempfile.NamedTemporaryFile(suffix='.txt', delete=False).name
        try:
            subprocess.run(
                ["codex", "exec", "-o", out_file, prompt],
                capture_output=True, text=True, timeout=300
            )
            if os.path.exists(out_file):
                with open(out_file) as f:
                    return f.read()
            return ""
        except Exception:
            return ""
        finally:
            if os.path.exists(out_file):
                os.unlink(out_file)
    else:
        cmd = ["claude", "-p", "--no-session-persistence"]
        if model:
            cmd.extend(["--model", model])
        try:
            result = subprocess.run(cmd, input=prompt, capture_output=True, text=True, timeout=300)
            return result.stdout
        except Exception:
            return ""


def extract_json_array(text):
    if not text:
        return []
    text = re.sub(r'```(?:json)?\s*', '', text)
    text = re.sub(r'```', '', text)
    start = text.find('[')
    if start == -1:
        return []
    depth = 0
    for i in range(start, len(text)):
        if text[i] == '[': depth += 1
        elif text[i] == ']':
            depth -= 1
            if depth == 0:
                try:
                    return json.loads(text[start:i+1])
                except:
                    return []
    return []


def run_blind_judge(lang_code, lang_name, ai_translations, human_translations,
                    english_strings, sample_size, seed, model, judge_llm="claude"):
    """Run blind A/B comparison for one language."""

    # Build lookup maps
    ai_map = {}
    for s in ai_translations:
        val = s.get("value", "")
        if s.get("type") == "plurals":
            val = " | ".join(f'{k}: {v}' for k, v in s.get("items", {}).items())
        ai_map[s["key"]] = val

    human_map = {}
    for s in human_translations:
        val = s.get("value", "")
        if s.get("type") == "plurals":
            val = " | ".join(f'{k}: {v}' for k, v in s.get("items", {}).items())
        human_map[s["key"]] = val

    english_map = {}
    for s in english_strings:
        val = s.get("value", "")
        if s.get("type") == "plurals":
            val = " | ".join(f'{k}: {v}' for k, v in s.get("items", {}).items())
        english_map[s["key"]] = val

    # Find keys present in both AI and human
    common_keys = [k for k in ai_map if k in human_map and k in english_map]
    if not common_keys:
        return {"language": lang_code, "error": "no common keys", "results": []}

    # Sample
    random.seed(seed)
    sampled_keys = random.sample(common_keys, min(sample_size, len(common_keys)))

    # Randomly assign A/B labels
    assignments = {}  # key -> {"a": "ai"|"human", "b": "ai"|"human"}
    entries = []
    for key in sampled_keys:
        if random.random() < 0.5:
            a_source, b_source = "ai", "human"
            a_text, b_text = ai_map[key], human_map[key]
        else:
            a_source, b_source = "human", "ai"
            a_text, b_text = human_map[key], ai_map[key]
        assignments[key] = {"a": a_source, "b": b_source}
        entries.append({
            "key": key,
            "english": english_map[key],
            "translation_a": a_text,
            "translation_b": b_text
        })

    # Build judge prompt (batch of all entries)
    entries_text = json.dumps(entries, ensure_ascii=False, indent=2)
    prompt = f"""You are a professional translator evaluating translations from English to {lang_name} for a mobile e-commerce app (WooCommerce).

For each entry below, you see the English original and two translations (A and B).
Judge which translation is better for use in a mobile app UI. Consider:
- Accuracy (correct meaning)
- Naturalness (sounds native)
- Length (shorter is better for mobile UI, as long as meaning is preserved)

Return a JSON array with one object per entry:
{{"key": "...", "winner": "A" or "B" or "TIE", "reason": "brief explanation"}}

Translations to evaluate:
{entries_text}"""

    print(f"  [{lang_code}] Calling {judge_llm} judge ({len(sampled_keys)} strings)...")
    response = call_llm(prompt, judge_llm=judge_llm, model=model)
    verdicts = extract_json_array(response)

    if not verdicts:
        print(f"  [{lang_code}] WARN: judge returned no parseable results")
        return {"language": lang_code, "error": "judge parse failed", "results": []}

    # Map verdicts back to ai/human wins
    results = []
    ai_wins = 0
    human_wins = 0
    ties = 0
    for v in verdicts:
        key = v.get("key", "")
        winner_label = v.get("winner", "TIE").upper()
        reason = v.get("reason", "")

        if key not in assignments:
            continue

        mapping = assignments[key]
        if winner_label == "A":
            actual_winner = mapping["a"]
        elif winner_label == "B":
            actual_winner = mapping["b"]
        else:
            actual_winner = "tie"

        if actual_winner == "ai":
            ai_wins += 1
        elif actual_winner == "human":
            human_wins += 1
        else:
            ties += 1

        results.append({
            "key": key,
            "winner": actual_winner,
            "reason": reason,
            "english": english_map.get(key, ""),
            "ai": ai_map.get(key, ""),
            "human": human_map.get(key, ""),
        })

    total = ai_wins + human_wins + ties
    print(f"  [{lang_code}] AI={ai_wins} Human={human_wins} Tie={ties} (total={total})")

    return {
        "language": lang_code,
        "language_name": lang_name,
        "sample_size": len(sampled_keys),
        "judged": total,
        "ai_wins": ai_wins,
        "human_wins": human_wins,
        "ties": ties,
        "ai_win_rate": ai_wins / total if total > 0 else 0,
        "human_win_rate": human_wins / total if total > 0 else 0,
        "tie_rate": ties / total if total > 0 else 0,
        "results": results
    }


def main():
    parser = argparse.ArgumentParser(description="Blind A/B judge: AI vs Human translations")
    parser.add_argument("--ai-dir", required=True, help="Dir with AI translation JSONs (e.g. results/translations/naive/codex/)")
    parser.add_argument("--languages", required=True, help="Comma-separated locale codes")
    parser.add_argument("--sample-size", type=int, default=50)
    parser.add_argument("--seed", type=int, default=42)
    parser.add_argument("--model", default=None)
    parser.add_argument("--judge-llm", choices=["claude", "codex"], default="claude")
    parser.add_argument("--output", required=True)
    args = parser.parse_args()

    LANG_NAMES = {
        "ar": "Arabic", "de": "German", "es": "Spanish", "fr": "French",
        "he": "Hebrew", "id": "Indonesian", "it": "Italian", "ja": "Japanese",
        "ko": "Korean", "nl": "Dutch", "pt-rBR": "Portuguese (Brazil)", "ru": "Russian",
        "sv": "Swedish", "tr": "Turkish", "zh-rCN": "Chinese (Simplified)", "zh-rTW": "Chinese (Traditional)"
    }

    # Load English strings once
    english_xml = REPO_ROOT / "WooCommerce" / "src" / "main" / "res" / "values" / "strings.xml"
    english_strings = cmd_extract_to_list(str(english_xml))

    languages = args.languages.split(",")
    all_results = []

    for lang_code in languages:
        lang_name = LANG_NAMES.get(lang_code, lang_code)

        # Load AI translations
        ai_file = Path(args.ai_dir) / f"{lang_code}.json"
        if not ai_file.exists():
            print(f"  [{lang_code}] SKIP: no AI file at {ai_file}")
            continue
        ai_translations = json.loads(ai_file.read_text())

        # Load human translations
        human_xml = REPO_ROOT / "WooCommerce" / "src" / "main" / "res" / f"values-{lang_code}" / "strings.xml"
        if not human_xml.exists():
            print(f"  [{lang_code}] SKIP: no human file at {human_xml}")
            continue
        human_translations = cmd_extract_to_list(str(human_xml))

        result = run_blind_judge(
            lang_code, lang_name, ai_translations, human_translations,
            english_strings, args.sample_size, args.seed, args.model,
            judge_llm=args.judge_llm
        )
        all_results.append(result)

    # Save results
    output = {
        "strategy": os.path.basename(os.path.dirname(args.ai_dir)),
        "llm": os.path.basename(args.ai_dir),
        "sample_size": args.sample_size,
        "languages": all_results,
        "summary": {}
    }

    # Compute overall summary
    total_ai = sum(r.get("ai_wins", 0) for r in all_results)
    total_human = sum(r.get("human_wins", 0) for r in all_results)
    total_ties = sum(r.get("ties", 0) for r in all_results)
    total_all = total_ai + total_human + total_ties
    if total_all > 0:
        output["summary"] = {
            "total_judged": total_all,
            "ai_wins": total_ai,
            "human_wins": total_human,
            "ties": total_ties,
            "ai_win_rate": total_ai / total_all,
            "human_win_rate": total_human / total_all,
            "tie_rate": total_ties / total_all
        }

    Path(args.output).parent.mkdir(parents=True, exist_ok=True)
    with open(args.output, 'w') as f:
        json.dump(output, f, ensure_ascii=False, indent=2)

    print(f"\nOverall: AI={total_ai} ({total_ai/total_all*100:.0f}%) Human={total_human} ({total_human/total_all*100:.0f}%) Tie={total_ties} ({total_ties/total_all*100:.0f}%)")
    print(f"Saved to {args.output}")


if __name__ == "__main__":
    main()
