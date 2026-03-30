#!/usr/bin/env python3
"""Fast translation using Claude/Codex CLI with parallel execution and large chunks."""

import argparse
import json
import os
import re
import subprocess
import sys
import tempfile
import time
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path

SCRIPT_DIR = Path(__file__).parent
REPO_ROOT = SCRIPT_DIR.parent.parent.parent
STRINGS_XML = REPO_ROOT / "WooCommerce" / "src" / "main" / "res" / "values" / "strings.xml"
PROMPTS_DIR = SCRIPT_DIR / "prompts"
RESULTS_DIR = SCRIPT_DIR.parent / "results" / "translations"

sys.path.insert(0, str(SCRIPT_DIR))
from parse_strings import cmd_extract_to_list


def load_prompt_template(strategy: str) -> str:
    template_file = PROMPTS_DIR / f"{strategy}.txt"
    return template_file.read_text()


def chunk_list(lst, size):
    for i in range(0, len(lst), size):
        yield lst[i:i + size]


def call_claude_cli(prompt_text, model=None):
    """Call Claude CLI with prompt via stdin, return response text."""
    with tempfile.NamedTemporaryFile(mode='w', suffix='.txt', delete=False) as f:
        f.write(prompt_text)
        prompt_file = f.name

    try:
        cmd = ["claude", "-p", "--no-session-persistence"]
        if model:
            cmd.extend(["--model", model])

        result = subprocess.run(
            cmd,
            input=prompt_text,
            capture_output=True, text=True,
            timeout=300
        )
        return result.stdout
    except subprocess.TimeoutExpired:
        return ""
    except Exception as e:
        return ""
    finally:
        os.unlink(prompt_file)


def call_codex_cli(prompt_text):
    """Call Codex CLI with prompt, return response text."""
    with tempfile.NamedTemporaryFile(mode='w', suffix='.txt', delete=False) as f:
        f.name
        out_file = f.name + ".out"

    try:
        result = subprocess.run(
            ["codex", "exec", "-o", out_file, prompt_text],
            capture_output=True, text=True,
            timeout=300
        )
        if os.path.exists(out_file):
            with open(out_file) as f:
                return f.read()
        return result.stdout
    except Exception:
        return ""
    finally:
        for f in [out_file]:
            if os.path.exists(f):
                os.unlink(f)


def extract_json_array(text):
    """Extract JSON array from LLM response (handles markdown fences)."""
    if not text:
        return []

    text = re.sub(r'```(?:json)?\s*', '', text)
    text = re.sub(r'```', '', text)

    start = text.find('[')
    if start == -1:
        return []

    depth = 0
    end = start
    for i in range(start, len(text)):
        if text[i] == '[':
            depth += 1
        elif text[i] == ']':
            depth -= 1
            if depth == 0:
                end = i + 1
                break

    try:
        return json.loads(text[start:end])
    except json.JSONDecodeError:
        return []


def translate_chunk(llm, model, prompt_text, chunk_index, total_chunks, lang_code):
    """Send a single chunk to the LLM and return parsed translations."""
    if llm == "claude":
        response = call_claude_cli(prompt_text, model)
    elif llm == "codex":
        response = call_codex_cli(prompt_text)
    else:
        return []

    parsed = extract_json_array(response)
    if parsed:
        print(f"  [OK] chunk {chunk_index}/{total_chunks} ({lang_code}): {len(parsed)} strings")
    else:
        print(f"  [WARN] chunk {chunk_index}/{total_chunks} ({lang_code}): failed to parse response")
    return parsed


def translate_language(llm, model, strategy, lang_name, lang_code, strings, context_map,
                       chunk_size, max_parallel):
    """Translate all strings for one language using parallel chunk processing."""
    template = load_prompt_template(strategy)
    chunks = list(chunk_list(strings, chunk_size))
    total = len(chunks)
    print(f"[{strategy}/{llm}] {lang_code} ({lang_name}): {len(strings)} strings, {total} chunks")

    def process_chunk(idx, chunk):
        if strategy == "contextual":
            enriched = []
            for s in chunk:
                entry = dict(s)
                key = s["key"]
                if key in context_map:
                    entry["context"] = context_map[key]
                enriched.append(entry)
            chunk_json = json.dumps(enriched, ensure_ascii=False, indent=2)
            prompt = template.replace("{LANGUAGE}", lang_name)
            prompt = prompt.replace("{STRINGS_WITH_CONTEXT_JSON}", chunk_json)
        else:
            chunk_json = json.dumps(chunk, ensure_ascii=False, indent=2)
            prompt = template.replace("{LANGUAGE}", lang_name)
            prompt = prompt.replace("{STRINGS_JSON}", chunk_json)

        return translate_chunk(llm, model, prompt, idx + 1, total, lang_code)

    all_translations = []

    # Run chunks in parallel
    with ThreadPoolExecutor(max_workers=max_parallel) as executor:
        futures = {executor.submit(process_chunk, i, c): i for i, c in enumerate(chunks)}
        results = [None] * len(chunks)
        for future in as_completed(futures):
            idx = futures[future]
            try:
                results[idx] = future.result()
            except Exception as e:
                print(f"  [ERR] chunk {idx + 1}/{total} ({lang_code}): {e}")
                results[idx] = []

    for result in results:
        if result:
            all_translations.extend(result)

    return all_translations


def load_context_map(strings):
    """Run batch context lookup using find_string_usage.sh."""
    keys_file = tempfile.NamedTemporaryFile(mode='w', suffix='.txt', delete=False)
    for s in strings:
        keys_file.write(s["key"] + "\n")
    keys_file.close()

    find_script = SCRIPT_DIR / "find_string_usage.sh"
    try:
        result = subprocess.run(
            ["bash", str(find_script), "--batch", keys_file.name],
            capture_output=True, text=True, timeout=120,
            cwd=str(REPO_ROOT)
        )
        if result.returncode == 0 and result.stdout.strip():
            return json.loads(result.stdout)
    except Exception as e:
        print(f"[WARN] context lookup failed: {e}")
    finally:
        os.unlink(keys_file.name)

    return {}


def main():
    parser = argparse.ArgumentParser(description="Fast AI translation using CLI tools")
    parser.add_argument("--strategy", choices=["naive", "contextual"], required=True)
    parser.add_argument("--languages", required=True, help="Comma-separated locale codes (e.g., de,fr,ja)")
    parser.add_argument("--llm", choices=["claude", "codex"], default="claude")
    parser.add_argument("--model", default=None, help="Model override (e.g., claude-haiku-4-5-20251001)")
    parser.add_argument("--chunk-size", type=int, default=500, help="Strings per chunk (default 500)")
    parser.add_argument("--parallel", type=int, default=4, help="Max parallel CLI calls (default 4)")
    parser.add_argument("--sample", type=int, default=0,
                        help="Random sample size (0 = all strings). Use 300 for experiment.")
    parser.add_argument("--seed", type=int, default=42, help="Random seed for sampling (default 42)")
    parser.add_argument("--force", action="store_true", help="Overwrite existing results")
    args = parser.parse_args()

    LANG_NAMES = {
        "ar": "Arabic", "de": "German", "es": "Spanish", "fr": "French",
        "he": "Hebrew", "id": "Indonesian", "it": "Italian", "ja": "Japanese",
        "ko": "Korean", "nl": "Dutch", "pt-rBR": "Portuguese (Brazil)", "ru": "Russian",
        "sv": "Swedish", "tr": "Turkish", "zh-rCN": "Chinese (Simplified)", "zh-rTW": "Chinese (Traditional)"
    }

    languages = args.languages.split(",")

    # Parse English strings once
    print(f"Parsing {STRINGS_XML}...")
    all_strings = cmd_extract_to_list(str(STRINGS_XML))
    print(f"  {len(all_strings)} total strings")

    # Sample if requested
    if args.sample > 0 and args.sample < len(all_strings):
        import random
        random.seed(args.seed)
        strings = random.sample(all_strings, args.sample)
        print(f"  Sampled {len(strings)} strings (seed={args.seed})")
    else:
        strings = all_strings

    # Load context map once if contextual (only for sampled strings)
    context_map = {}
    if args.strategy == "contextual":
        print("Loading code context (batch grep)...")
        context_map = load_context_map(strings)
        print(f"  Context found for {len(context_map)} keys")

    total_start = time.time()
    for lang_code in languages:
        lang_name = LANG_NAMES.get(lang_code, lang_code)

        out_dir = RESULTS_DIR / args.strategy / args.llm
        out_dir.mkdir(parents=True, exist_ok=True)
        out_file = out_dir / f"{lang_code}.json"

        if out_file.exists() and not args.force:
            print(f"[SKIP] {lang_code}: results exist at {out_file} (use --force to overwrite)")
            continue

        start_time = time.time()
        translations = translate_language(
            args.llm, args.model, args.strategy,
            lang_name, lang_code, strings, context_map,
            args.chunk_size, args.parallel
        )
        elapsed = time.time() - start_time

        with open(out_file, 'w', encoding='utf-8') as f:
            json.dump(translations, f, ensure_ascii=False, indent=2)

        coverage = len(translations) / len(strings) * 100
        print(f"[DONE] {lang_code}: {len(translations)}/{len(strings)} ({coverage:.0f}%) in {elapsed:.1f}s\n")

    total_elapsed = time.time() - total_start
    print(f"\nTotal time: {total_elapsed:.1f}s for {len(languages)} languages")


if __name__ == "__main__":
    main()
