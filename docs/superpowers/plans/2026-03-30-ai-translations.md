# AI Translation Automation — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Evaluate AI translation quality against human translations, integrate AI translation into the release pipeline to eliminate the 1-2 day GlotPress wait and the guaranteed translation rebuild, and document ROI.

**Architecture:** Shell scripts orchestrate Claude/Codex CLI calls for translation. Python scripts handle XML parsing, metrics computation (sacrebleu), and report generation (Plotly). Integration touches Fastlane lanes and Buildkite pipelines. All code lives on `hack/ai-translations` branch under `scripts/ai-translations/`.

**Tech Stack:** Bash, Python 3.14 (sacrebleu, plotly, Levenshtein, anthropic), Claude CLI, Codex CLI (experiment only), Fastlane (Ruby), Buildkite YAML

**CI API Access:** `ANTHROPIC_API_KEY` already exists as a Buildkite organization secret (used by `claude-summarize#v1.1.0` plugin for build failure analysis). Production scripts use the Anthropic Python SDK. Experiment scripts use local CLI tools.

---

## File Map

```
scripts/ai-translations/
├── requirements.txt                    # Python deps: sacrebleu, plotly, python-Levenshtein
├── experiment/
│   ├── parse_strings.py                # strings.xml <-> JSON, shared by all scripts
│   ├── find_string_usage.sh            # Grep codebase for string key usage context
│   ├── translate_naive.sh              # Naive strategy: call LLM CLI with plain prompt
│   ├── translate_contextual.sh         # Contextual strategy: length + code context
│   ├── run_experiment.sh               # Orchestrator: runs all 64 combos
│   └── prompts/
│       ├── naive.txt                   # Prompt template for naive strategy
│       └── contextual.txt              # Prompt template for contextual strategy
├── evaluate/
│   ├── compute_metrics.py              # BLEU, chrF, Levenshtein computation
│   ├── llm_judge.sh                    # LLM-as-judge rating script
│   └── generate_report.py             # Plotly charts -> self-contained HTML
├── integrate/
│   ├── translate.sh                    # Production script: translate new/changed strings
│   └── merge_translations.py          # Merge AI translations into values-*/strings.xml
├── docs/
│   └── ai-translations-pitch.md       # Final ROI pitch document
└── results/                            # .gitignore'd experiment output
    ├── translations/{strategy}/{llm}/{lang}.json
    ├── metrics/{strategy}_{llm}_metrics.json
    ├── llm_judge_scores.json
    └── report.html
```

**Existing files to modify (Phase 2):**
- `fastlane/Fastfile` — add `ai_translate_strings` lane
- `.buildkite/release-pipelines/` — add `ai-translate-strings.yml`
- `.gitignore` — add `scripts/ai-translations/results/`

---

## Phase 1: Experiment Infrastructure

### Task 1: Python Environment Setup

**Files:**
- Create: `scripts/ai-translations/requirements.txt`
- Modify: `.gitignore`

- [ ] **Step 1: Create requirements.txt**

```
sacrebleu>=2.3.0
plotly>=5.18.0
python-Levenshtein>=0.25.0
kaleido>=0.2.1
anthropic>=0.40.0
```

- [ ] **Step 2: Add results dir to .gitignore**

Append to `.gitignore`:
```
# AI translations experiment results
scripts/ai-translations/results/
```

- [ ] **Step 3: Install deps and verify**

Run: `pip3 install -r scripts/ai-translations/requirements.txt`
Expected: all packages install successfully

- [ ] **Step 4: Create results directory structure**

Run:
```bash
mkdir -p scripts/ai-translations/results/{translations/{naive,contextual}/{claude,codex},metrics}
```

- [ ] **Step 5: Commit**

```bash
git add scripts/ai-translations/requirements.txt .gitignore
git commit -m "Add Python dependencies for AI translation experiment"
```

---

### Task 2: String Parser (XML <-> JSON)

**Files:**
- Create: `scripts/ai-translations/experiment/parse_strings.py`

This is the foundation — all other scripts depend on it.

- [ ] **Step 1: Write parse_strings.py**

The script needs three modes:
1. `extract` — read a strings.xml, output JSON array of `{key, value, translatable}` objects
2. `extract-keys` — output only string keys (for diffing between releases)
3. `to-xml` — read JSON translations, write a strings.xml file

Key details from the actual file format:
- XML declaration: `<?xml version="1.0" encoding="UTF-8"?>`
- Root: `<resources xmlns:tools="http://schemas.android.com/tools">`
- Translated files have GlotPress comment headers (lines 2-7 with Translation-Revision-Date, Language, etc.)
- Skip strings with `translatable="false"` attribute
- Do NOT skip `content_override="true"` strings — some are translatable (e.g., `enter_site_address`)
  Only skip if also marked `translatable="false"`
- Handle `<plurals>` elements — they have `<item quantity="one">` / `<item quantity="other">` etc.
  Different languages have different plural forms (Arabic: 6, Russian: 3, English: 2).
  Output plurals as `{key, type: "plurals", items: {quantity: value}}` in JSON
- Skip `<string-array>` elements (all are `translatable="false"` in this project)
- HTML entities like `&lt;b&gt;` must be preserved as-is
- String values can contain `%1$s`, `%1$d` format placeholders — preserve these

Input path: `WooCommerce/src/main/res/values/strings.xml` (English)
Input path: `WooCommerce/src/main/res/values-{locale}/strings.xml` (translations)

Usage:
```bash
# Extract English strings to JSON
python3 parse_strings.py extract WooCommerce/src/main/res/values/strings.xml > english.json

# Extract translated strings
python3 parse_strings.py extract WooCommerce/src/main/res/values-de/strings.xml > german.json

# Convert JSON translations back to XML
python3 parse_strings.py to-xml translations.json > strings.xml
```

- [ ] **Step 2: Test with actual strings.xml**

Run: `python3 scripts/ai-translations/experiment/parse_strings.py extract WooCommerce/src/main/res/values/strings.xml | python3 -c "import json,sys; data=json.load(sys.stdin); print(f'Parsed {len(data)} strings'); print(json.dumps(data[:3], indent=2))"`

Expected: ~3800+ strings parsed, first 3 displayed with correct keys and values

- [ ] **Step 3: Test with translated file**

Run: `python3 scripts/ai-translations/experiment/parse_strings.py extract WooCommerce/src/main/res/values-de/strings.xml | python3 -c "import json,sys; data=json.load(sys.stdin); print(f'Parsed {len(data)} German strings')"`

Expected: similar count to English (some strings may be missing in translation)

- [ ] **Step 4: Test round-trip (to-xml)**

Run:
```bash
python3 scripts/ai-translations/experiment/parse_strings.py extract WooCommerce/src/main/res/values-de/strings.xml > /tmp/de_test.json
python3 scripts/ai-translations/experiment/parse_strings.py to-xml /tmp/de_test.json > /tmp/de_test.xml
head -20 /tmp/de_test.xml
```

Expected: valid Android strings.xml format

- [ ] **Step 5: Commit**

```bash
git add scripts/ai-translations/experiment/parse_strings.py
git commit -m "Add strings.xml parser for AI translation experiment"
```

---

### Task 3: String Usage Context Finder

**Files:**
- Create: `scripts/ai-translations/experiment/find_string_usage.sh`

This script greps the codebase for how a string resource is used (button, title, dialog, etc.) to provide context to the contextual translation strategy.

- [ ] **Step 1: Write find_string_usage.sh**

Two modes:
1. **Single key**: `./find_string_usage.sh order_detail_title` — outputs code snippets for one key
2. **Batch mode**: `./find_string_usage.sh --batch keys.txt` — greps for ALL keys in one pass,
   outputs a JSON map of `{key: "context snippet"}`. This is critical for performance —
   grepping 3,800 keys one-by-one would take hours.

Batch mode approach:
- Build a single regex alternation: `R\.string\.(key1|key2|key3|...)` (split into chunks if too long)
- Run one `grep -rn` pass over `WooCommerce/src/main/kotlin/` and `WooCommerce/src/main/res/`
- Parse output to map each match back to its string key
- Limit to 3 matches per key (enough for context, not overwhelming)

Search patterns:
- `R.string.{key}` in Kotlin/Java files
- `@string/{key}` in XML layout files

- [ ] **Step 2: Test with a known string**

Run: `bash scripts/ai-translations/experiment/find_string_usage.sh app_name`

Expected: at least one match showing where `R.string.app_name` or `@string/app_name` is referenced

- [ ] **Step 3: Test with a UI-specific string**

Run: `bash scripts/ai-translations/experiment/find_string_usage.sh orderdetail_order_status_ordernum`

Expected: code context showing this is used in order detail UI

- [ ] **Step 4: Commit**

```bash
git add scripts/ai-translations/experiment/find_string_usage.sh
git commit -m "Add string usage context finder for contextual translations"
```

---

### Task 4: Prompt Templates

**Files:**
- Create: `scripts/ai-translations/experiment/prompts/naive.txt`
- Create: `scripts/ai-translations/experiment/prompts/contextual.txt`

- [ ] **Step 1: Write naive prompt template**

The naive prompt gives minimal instructions — just translate:

```
Translate the following Android string resources from English to {LANGUAGE}.

Return a JSON array of objects. For regular strings: {"key": "...", "value": "..."}.
For plurals: {"key": "...", "type": "plurals", "items": {"one": "...", "other": "...", ...}}.
For plurals, provide ALL the plural forms that {LANGUAGE} requires
(e.g., Arabic needs: zero, one, two, few, many, other).

Keep all format placeholders like %1$s, %1$d, %s, %d exactly as they are.
Keep all HTML tags like <b>, </b>, <br> exactly as they are.
Do not translate string content that looks like a URL, email, or technical identifier.

Input strings:
{STRINGS_JSON}
```

- [ ] **Step 2: Write contextual prompt template**

The contextual prompt adds length guidance and code usage context:

```
You are translating Android app UI strings for WooCommerce, an e-commerce app.
Translate the following strings from English to {LANGUAGE}.

IMPORTANT GUIDELINES:
1. Keep translations similar in length to the English original. Mobile UI has limited space.
2. Keep all format placeholders like %1$s, %1$d, %s, %d exactly as they are.
3. Keep all HTML tags like <b>, </b>, <br> exactly as they are.
4. Do not translate URLs, emails, or technical identifiers.
5. Use the code context below each string to understand where it appears in the UI
   (e.g., button label, dialog title, error message) and translate accordingly.
6. For plurals, provide ALL the plural forms that {LANGUAGE} requires
   (e.g., Arabic needs: zero, one, two, few, many, other).

Return a JSON array of objects. For regular strings: {"key": "...", "value": "..."}.
For plurals: {"key": "...", "type": "plurals", "items": {"one": "...", "other": "...", ...}}.

Input strings with context:
{STRINGS_WITH_CONTEXT_JSON}
```

- [ ] **Step 3: Commit**

```bash
git add scripts/ai-translations/experiment/prompts/
git commit -m "Add prompt templates for naive and contextual translation strategies"
```

---

### Task 5: Translation Scripts (Naive + Contextual)

**Files:**
- Create: `scripts/ai-translations/experiment/translate_naive.sh`
- Create: `scripts/ai-translations/experiment/translate_contextual.sh`

Both scripts take a language code and an LLM name (claude/codex) and produce a JSON file of translations.

- [ ] **Step 1: Write translate_naive.sh**

Arguments: `<language_name> <language_code> <llm>` (e.g., `German de claude`)

Flow:
1. Extract English strings using `parse_strings.py extract`
2. Split into chunks of ~200 strings if needed (check total count)
3. For each chunk, substitute into `prompts/naive.txt` template
4. Call `claude` or `codex` CLI with the prompt (pipe via stdin)
   - Claude: `echo "$PROMPT" | claude --print --no-input`
   - Codex: `codex exec "$PROMPT"`
5. Parse JSON response, merge chunks
6. Save to `results/translations/naive/{llm}/{lang_code}.json`

- [ ] **Step 2: Write translate_contextual.sh**

Same arguments and flow as naive, but:
1. Run `find_string_usage.sh --batch` to get context for ALL strings in one pass
2. Build JSON with context: `{key, value, context: "Used in: ..."}`
3. Use `prompts/contextual.txt` template
4. Same LLM call pattern

- [ ] **Step 3: Test naive with a small subset (5 strings, 1 language)**

Run a quick manual test with just 5 strings to verify the CLI integration works:
```bash
# Create a small test subset first
python3 scripts/ai-translations/experiment/parse_strings.py extract WooCommerce/src/main/res/values/strings.xml | python3 -c "import json,sys; d=json.load(sys.stdin); json.dump(d[:5],sys.stdout)" > /tmp/test_5_strings.json
```

Then run: `bash scripts/ai-translations/experiment/translate_naive.sh German de claude`
(Modify the script temporarily to use the 5-string file for testing)

Expected: JSON output with 5 German translations

- [ ] **Step 4: Commit**

```bash
git add scripts/ai-translations/experiment/translate_naive.sh scripts/ai-translations/experiment/translate_contextual.sh
git commit -m "Add naive and contextual translation scripts"
```

---

### Task 6: Experiment Orchestrator

**Files:**
- Create: `scripts/ai-translations/experiment/run_experiment.sh`

- [ ] **Step 1: Write run_experiment.sh**

This script runs all 64 combinations (2 strategies x 2 LLMs x 16 languages).

Language map (from `fastlane/Fastfile` lines 99-116):
```bash
declare -A LANGUAGES=(
  [ar]="Arabic" [de]="German" [es]="Spanish" [fr]="French"
  [he]="Hebrew" [id]="Indonesian" [it]="Italian" [ja]="Japanese"
  [ko]="Korean" [nl]="Dutch" [pt-rBR]="Portuguese (Brazil)" [ru]="Russian"
  [sv]="Swedish" [tr]="Turkish" [zh-rCN]="Chinese (Simplified)" [zh-rTW]="Chinese (Traditional)"
)
```

Note: Android locale codes use `-r` prefix for region (e.g., `pt-rBR`), but GlotPress uses `-` (e.g., `pt-br`). The `values-*` directories use the Android format.

Flow:
1. Parse English strings once (shared across all runs)
2. For each strategy (naive, contextual):
   - For each LLM (claude, codex):
     - For each language:
       - Run translation script
       - Log progress: `[strategy/llm] lang: done (N/64)`
3. Print summary when complete

Support `--languages` flag to run a subset (e.g., `--languages de,ja` for testing).
Support `--strategy` flag to run only one strategy.
Support `--llm` flag to run only one LLM.

- [ ] **Step 2: Test with single combo**

Run: `bash scripts/ai-translations/experiment/run_experiment.sh --languages de --strategy naive --llm claude`

Expected: produces `results/translations/naive/claude/de.json`

- [ ] **Step 3: Commit**

```bash
git add scripts/ai-translations/experiment/run_experiment.sh
git commit -m "Add experiment orchestrator for translation runs"
```

---

## Phase 1: Evaluation

### Task 7: Metrics Computation

**Files:**
- Create: `scripts/ai-translations/evaluate/compute_metrics.py`

- [ ] **Step 1: Write compute_metrics.py**

Takes AI translation JSON + human translation JSON (ground truth), computes per-string metrics.

Usage:
```bash
python3 compute_metrics.py \
  --ai results/translations/naive/claude/de.json \
  --human <(python3 ../experiment/parse_strings.py extract WooCommerce/src/main/res/values-de/strings.xml) \
  --output results/metrics/naive_claude_de.json
```

For each string key present in both files, compute:
- **BLEU** — using `sacrebleu.sentence_bleu(ai_translation, [human_translation])`
- **chrF** — using `sacrebleu.sentence_chrf(ai_translation, [human_translation])`
- **Normalized Levenshtein** — `Levenshtein.ratio(ai, human)` (1.0 = identical)

Output JSON format:
```json
{
  "language": "de",
  "strategy": "naive",
  "llm": "claude",
  "per_string": [
    {"key": "app_name", "bleu": 100.0, "chrf": 100.0, "levenshtein_sim": 1.0}
  ],
  "aggregates": {
    "bleu_mean": 45.2,
    "bleu_median": 48.0,
    "chrf_mean": 62.1,
    "chrf_median": 65.0,
    "levenshtein_sim_mean": 0.72,
    "levenshtein_sim_median": 0.75
  },
  "coverage": {"total_human": 3800, "total_ai": 3800, "matched": 3800}
}
```

- [ ] **Step 2: Test with actual data (after at least one translation run exists)**

Run: `python3 scripts/ai-translations/evaluate/compute_metrics.py --ai results/translations/naive/claude/de.json --human <(python3 scripts/ai-translations/experiment/parse_strings.py extract WooCommerce/src/main/res/values-de/strings.xml) --output /tmp/test_metrics.json && python3 -c "import json; d=json.load(open('/tmp/test_metrics.json')); print(json.dumps(d['aggregates'], indent=2))"`

Expected: aggregated BLEU/chrF/Levenshtein scores

- [ ] **Step 3: Commit**

```bash
git add scripts/ai-translations/evaluate/compute_metrics.py
git commit -m "Add metrics computation for translation evaluation"
```

---

### Task 8: LLM-as-Judge

**Files:**
- Create: `scripts/ai-translations/evaluate/llm_judge.sh`

- [ ] **Step 1: Write llm_judge.sh**

Samples ~100 strings per language and asks both Claude and Codex to rate translations.

Arguments: `<ai_translations.json> <human_translations.json> <language> <output.json>`

Flow:
1. Load AI translations and human translations
2. Randomly sample 100 string keys
3. For each sampled string, build a judge prompt:
   ```
   You are evaluating a translation from English to {LANGUAGE} for a mobile e-commerce app.

   English original: "{english}"
   AI translation: "{ai_translation}"
   Human reference: "{human_translation}"

   Rate the AI translation on three dimensions (1-5 scale):
   - accuracy: Does it convey the same meaning as the English? (5=perfect, 1=wrong meaning)
   - naturalness: Does it sound natural to a native speaker? (5=native quality, 1=clearly machine)
   - length_fit: Is it a similar length to the English original? (5=same length, 1=much longer/shorter)

   Return ONLY a JSON object: {"accuracy": N, "naturalness": N, "length_fit": N}
   ```
4. Call Claude CLI and Codex CLI to judge (both judge all translations)
5. Aggregate scores and save

- [ ] **Step 2: Commit**

```bash
git add scripts/ai-translations/evaluate/llm_judge.sh
git commit -m "Add LLM-as-judge evaluation script"
```

---

### Task 9: Report Generator

**Files:**
- Create: `scripts/ai-translations/evaluate/generate_report.py`

- [ ] **Step 1: Write generate_report.py**

Reads all metrics JSON files from `results/metrics/` and generates a self-contained HTML report.

Usage: `python3 generate_report.py --metrics-dir results/metrics/ --output results/report.html`

Charts to generate (all using Plotly, embedded in single HTML file):

1. **Grouped bar chart** — Average BLEU score per language, bars grouped by strategy+LLM combo.
   X-axis: languages, Y-axis: BLEU score. 4 bars per language (naive/claude, naive/codex, contextual/claude, contextual/codex).

2. **Same as above but for chrF** — chrF is often more meaningful for non-Latin scripts.

3. **Heatmap** — Rows: languages, Columns: strategy+LLM combos. Cell value: BLEU score. Color scale: red (low) to green (high).

4. **Box plots** — One plot per strategy+LLM combo showing the distribution of per-string BLEU scores across all languages. Shows variance and outliers.

5. **Scatter plot** — X: English string length (characters), Y: BLEU score. One trace per strategy. Shows if quality drops for longer strings.

6. **Radar chart** — LLM judge dimensions (accuracy, naturalness, length_fit) per strategy+LLM combo. If judge data exists.

7. **Summary table** — Top section of the report showing aggregate numbers:
   | Strategy | LLM | Avg BLEU | Avg chrF | Avg Levenshtein Similarity | Judge Accuracy | Judge Naturalness |

8. **Worst strings table** — Bottom 20 strings by BLEU score with English, AI translation, and human reference shown side-by-side.

The HTML should have a dark theme, navigation sidebar, and each chart in its own section.

- [ ] **Step 2: Test with mock data if no real results yet**

Create a small mock metrics file and run:
```bash
python3 scripts/ai-translations/evaluate/generate_report.py --metrics-dir results/metrics/ --output /tmp/test_report.html
open /tmp/test_report.html
```

Expected: HTML file opens in browser with charts rendered

- [ ] **Step 3: Commit**

```bash
git add scripts/ai-translations/evaluate/generate_report.py
git commit -m "Add Plotly report generator for translation evaluation"
```

---

### Task 10: Run Full Experiment

This is the execution step — not code to write, but commands to run.

- [ ] **Step 1: Run all 64 translation combos**

```bash
cd scripts/ai-translations
bash experiment/run_experiment.sh 2>&1 | tee results/experiment.log
```

This will take a while (64 LLM calls with large prompts). Monitor progress in the log.

- [ ] **Step 2: Compute metrics for all results**

```bash
# Script to compute metrics for all result files
for strategy in naive contextual; do
  for llm in claude codex; do
    for lang_file in results/translations/$strategy/$llm/*.json; do
      lang=$(basename "$lang_file" .json)
      android_locale=$lang  # map if needed
      human_file="WooCommerce/src/main/res/values-${android_locale}/strings.xml"
      python3 evaluate/compute_metrics.py \
        --ai "$lang_file" \
        --human <(python3 experiment/parse_strings.py extract "../../$human_file") \
        --output "results/metrics/${strategy}_${llm}_${lang}.json"
    done
  done
done
```

- [ ] **Step 3: Run LLM judge on a sample**

```bash
bash evaluate/llm_judge.sh
```

- [ ] **Step 4: Generate the report**

```bash
python3 evaluate/generate_report.py --metrics-dir results/metrics/ --output results/report.html
open results/report.html
```

- [ ] **Step 5: Commit the report**

```bash
git add -f results/report.html
git commit -m "Add AI translation evaluation report"
```

---

## Phase 2: Release Pipeline Integration

### Learnings from Phase 1 that affect Phase 2

These findings from the experiment should guide the production implementation:

1. **Use naive strategy (no code context grep needed).** The experiment showed contextual
   strategy performs identically to naive in blind judge evaluation (38% vs 39% AI win rate).
   Naive is ~40% faster and much simpler to implement.

2. **Use Codex/OpenAI for translations, not Claude.** Codex scored higher on both
   similarity metrics (BLEU 60.6 vs 56.4) and blind judge (40% vs 36% AI win rate).
   However, Claude is acceptable as fallback since it still beats human translations.

3. **Chunking: use ~50-100 strings per chunk, not 300+.** Claude failed on Chinese
   Simplified with 300-string chunks. Smaller chunks are more reliable across all languages.

4. **CLI is too slow for CI.** Claude CLI takes ~108s per language due to process startup
   overhead. Use the Anthropic Python SDK or OpenAI API directly for production.
   `ANTHROPIC_API_KEY` is already in Buildkite secrets. For Codex/OpenAI, an `OPENAI_API_KEY`
   would need to be added.

5. **`parse_strings.py` already has `cmd_extract_to_list()`.** The production script can
   import this directly instead of calling the CLI.

6. **venv is required.** macOS system Python blocks global pip installs. The Buildkite
   pipeline needs to set up a venv or use `pip install --user`.

7. **The prompt is simple.** Just "translate to {LANGUAGE}, keep placeholders and HTML"
   produces good results. No complex instructions needed.

8. **A typical release adds 20-50 new strings.** Translating 50 strings to 16 languages
   fits in a single chunk per language - no chunking needed for incremental translations.

### Task 11: Production Translation Script

**Files:**
- Create: `scripts/ai-translations/integrate/translate.py` (Python, not shell - reuses parse_strings.py)

- [ ] **Step 1: Write translate.py**

This is the production-ready script that translates only new/changed strings.
Uses the **Anthropic Python SDK** (or OpenAI SDK) so it works on CI.

Arguments: `[--previous-tag TAG]` (defaults to latest release tag)

Flow:
1. Find previous release tag: `git describe --tags --abbrev=0 --match="*.*"` (or use provided tag)
2. Diff `strings.xml` between current and previous tag:
   ```python
   # Extract keys from previous version using parse_strings.py
   prev_xml = subprocess.check_output(["git", "show", f"{prev_tag}:WooCommerce/src/main/res/values/strings.xml"])
   prev_strings = parse_strings.extract_from_text(prev_xml)
   current_strings = parse_strings.cmd_extract_to_list(current_xml_path)
   ```
3. Compute diff: new keys and changed values
4. Build prompt using naive template (no code context needed - Phase 1 showed it doesn't help)
5. Call LLM API to translate to all 16 languages (parallel, one chunk per language since
   incremental translations are small - typically 20-50 strings)
6. Output per-language JSON files to a temp directory

Usage:
```bash
bash scripts/ai-translations/integrate/translate.sh
# Output: /tmp/ai-translations/{lang_code}.json for each language
```

- [ ] **Step 2: Test by simulating a diff**

Run against the 4 strings that differ between `fastlane/resources/values/strings.xml` (frozen) and current `strings.xml`:
```bash
bash scripts/ai-translations/integrate/translate.sh --previous-tag HEAD~10
```

Expected: JSON files with translations for just the changed strings

- [ ] **Step 3: Commit**

```bash
git add scripts/ai-translations/integrate/translate.sh
git commit -m "Add production translation script for new/changed strings"
```

---

### Task 12: Translation Merger

**Files:**
- Create: `scripts/ai-translations/integrate/merge_translations.py`

- [ ] **Step 1: Write merge_translations.py**

Merges AI-translated strings into existing `values-*/strings.xml` files.

Arguments: `--translations-dir /tmp/ai-translations/ --res-dir WooCommerce/src/main/res/`

For each language JSON file in the translations dir:
1. Map language code to Android locale dir (e.g., `pt-br` → `values-pt-rBR`)
2. **Validate** the translation JSON before merging:
   - All format placeholders (`%1$s`, `%d`, etc.) from the English string must be present
   - All HTML tags must be balanced
   - JSON must contain the same keys as the input (no missing, no duplicates)
   - XML output must be well-formed (parse test)
   - For plurals: required quantity forms must be present for the target language
   - Log warnings for any strings that fail validation (skip those, keep existing translation)
3. Read existing `values-{locale}/strings.xml`
4. Also update legacy locale dirs: copy `values-he` → `values-iw`, `values-id` → `values-in`
5. For each validated translated string:
   - If key exists: update the value
   - If key is new: insert at the same position as in the English file
6. Write the updated XML, preserving:
   - XML declaration and encoding
   - GlotPress comment headers (update Translation-Revision-Date)
   - Existing string ordering
   - `tools:` namespace attributes

Language code to Android locale mapping (includes legacy dirs):
```python
LOCALE_MAP = {
    "ar": ["ar"], "de": ["de"], "es": ["es"], "fr": ["fr"],
    "he": ["he", "iw"], "id": ["id", "in"], "it": ["it"], "ja": ["ja"],
    "ko": ["ko"], "nl": ["nl"], "pt-br": ["pt-rBR"], "ru": ["ru"],
    "sv": ["sv"], "tr": ["tr"], "zh-cn": ["zh-rCN"], "zh-tw": ["zh-rTW"]
}
```

- [ ] **Step 2: Test merge on a single language**

```bash
# Create a test translation
echo '[{"key":"test_new_string","value":"Testzeichenfolge"}]' > /tmp/ai-translations/de.json
python3 scripts/ai-translations/integrate/merge_translations.py \
  --translations-dir /tmp/ai-translations/ \
  --res-dir WooCommerce/src/main/res/ \
  --dry-run
```

Expected: shows what would change in `values-de/strings.xml` without writing

- [ ] **Step 3: Commit**

```bash
git add scripts/ai-translations/integrate/merge_translations.py
git commit -m "Add translation merger for values-*/strings.xml files"
```

---

### Task 13: Fastlane Lane + Buildkite Pipeline

**Files:**
- Modify: `fastlane/Fastfile`
- Create: `.buildkite/release-pipelines/ai-translate-strings.yml`

- [ ] **Step 1: Add Fastlane lane to Fastfile**

Add after the existing `download_translations` lane (~line 475).
Must follow existing lane patterns: accept `skip_confirm` parameter, call `configure_apply`.

```ruby
#####################################################################################
# ai_translate_strings
# -----------------------------------------------------------------------------------
# This lane translates new/changed strings using AI and updates the strings.xml files
# -----------------------------------------------------------------------------------
# Usage:
# bundle exec fastlane ai_translate_strings skip_confirm:true
#####################################################################################
desc 'Translate new/changed strings using AI and update the strings.xml files'
lane :ai_translate_strings do |skip_confirm: false|
  ensure_git_status_clean
  ensure_git_branch_is_release_branch!

  UI.important("Translating strings using AI for release: #{release_version_current}")
  UI.user_error!("Terminating as requested.") unless skip_confirm || UI.confirm('Do you want to continue?')

  configure_apply(force: is_ci)

  # Set up Python venv (macOS blocks global pip installs)
  venv_dir = File.join(Dir.pwd, '..', 'scripts', 'ai-translations', 'venv')
  python = File.join(venv_dir, 'bin', 'python3')
  unless File.exist?(python)
    sh('python3', '-m', 'venv', venv_dir)
    sh(python, '-m', 'pip', 'install', '-r',
       File.join(Dir.pwd, '..', 'scripts', 'ai-translations', 'requirements.txt'))
  end

  # Run the AI translation script (Python, uses Anthropic/OpenAI SDK)
  sh(python, File.join(Dir.pwd, '..', 'scripts', 'ai-translations', 'integrate', 'translate.py'))

  # Merge translations into strings.xml files
  sh(python, File.join(Dir.pwd, '..', 'scripts', 'ai-translations', 'integrate', 'merge_translations.py'),
     '--translations-dir', '/tmp/ai-translations/',
     '--res-dir', File.join(Dir.pwd, '..', 'WooCommerce', 'src', 'main', 'res'))

  # Stage and commit
  sh('git', 'add', 'WooCommerce/src/main/res/values-*/')
  git_commit(
    path: 'WooCommerce/src/main/res/values-*/',
    message: "Update translations using AI for #{release_version_current}",
    allow_nothing_to_commit: true
  )

  push_to_git_remote(tags: false)
  UI.success("Successfully translated strings for #{release_version_current}")
end
```

- [ ] **Step 2: Create Buildkite pipeline YAML**

Create `.buildkite/release-pipelines/ai-translate-strings.yml`:

```yaml
# yaml-language-server: $schema=https://raw.githubusercontent.com/buildkite/pipeline-schema/main/schema.json
---

steps:
  - label: "AI Translate Strings"
    plugins: [$CI_TOOLKIT]
    command: |
      echo '--- :robot_face: Use bot for git operations'
      source use-bot-for-git

      .buildkite/commands/checkout-release-branch.sh "$RELEASE_VERSION"

      echo '--- :ruby: Setup Ruby Tools'
      install_gems

      echo '--- :globe_with_meridians: AI Translate Strings'
      bundle exec fastlane ai_translate_strings skip_confirm:true
      # Note: Fastlane lane handles venv setup internally
    agents:
        queue: "mac-metal"
    retry:
      manual:
        reason: If release jobs fail, you should always re-trigger the task from Releases V2 rather than retrying the individual job from Buildkite
        allowed: false
```

- [ ] **Step 3: Commit**

```bash
git add fastlane/Fastfile .buildkite/release-pipelines/ai-translate-strings.yml
git commit -m "Add AI translation Fastlane lane and Buildkite pipeline"
```

---

### Task 14: Release Changes Analysis Document

**Files:**
- Create: `scripts/ai-translations/docs/release-changes-analysis.md`

This is the thorough analysis of ALL changes needed to achieve the single-build
optimization (Option C: Play Store track promotion). We build this document, not the changes.

- [ ] **Step 1: Write release-changes-analysis.md**

Structure — for each required change, document:
- What needs to change (specific file, function, line numbers)
- Why it needs to change
- What breaks if we don't change it
- Proposed solution

Changes to document:

**1. Version naming — drop RC suffix**
- File: `fastlane/Fastfile`, `start_code_freeze` lane
- Current: sets version to `X.Y-rc-1` via `beta_version_next`
- Change: set version to `X.Y` directly, use build code for iteration
- Dependency: `Fastlane::Wpmreleasetoolkit::Versioning::RCNotationVersionFormatter`
  in `fastlane-plugin-wpmreleasetoolkit` gem (source: github.com/wordpress-mobile/release-toolkit)

**2. Build routing — decouple from version name**
- File: `fastlane/Fastfile`, `build_and_upload_google_play` lane (~line 650)
- Current: `beta_version?(version_name_current)` checks for `-rc-` → routes to beta track
- Problem: without `-rc-`, first build routes to production at 10% rollout
- Solution: pass explicit `track` parameter via env var through `trigger_release_build`

**3. Beta iteration without RC**
- File: `fastlane/Fastfile`, `new_beta_release` lane (~line 338)
- Current: `beta_version_next` parses `X.Y-rc-1` → `X.Y-rc-2`
- Problem: parsing fails without `-rc-` suffix
- Solution: just increment build code, skip version name change

**4. Finalize release — split into admin + promote**
- File: `fastlane/Fastfile`, `finalize_release` lane (~line 519)
- Current responsibilities: version rename + build code bump + trigger build +
  backmerge PR + remove branch protection + close milestone
- Split: `promote_to_production` (Play Store track promotion) +
  `finalize_release` (admin: backmerge, milestone, branch protection, no build)

**5. Releases V2 scenario**
- File: `wpcom-trunk/wp-content/lib/a8c/releases-v2/config/scenarios/wcandroid.php`
- Add "AI Translate Strings" Buildkite button to Code Freeze milestone
  (include proposed PHP code for the Task::buildkite() entry)
- Change "Play Store Submission" milestone to remove Download Translations + Finalize,
  replace with Promote + Admin Finalize
- Update Slack messages to remove `rc-1` references
- Note: requires wpcom deploy, coordinate with Apps Infrastructure team

**6. Release toolkit gem changes**
- Repo: github.com/wordpress-mobile/release-toolkit
- `RCNotationVersionFormatter` — no longer used for this product
- `beta_version?` / `beta_version_next` — need alternative for non-RC products
- Impact assessment: is this WCAndroid-specific or does it affect other mobile apps?

**7. Deferred items**
- Play Store metadata translations (app title, description, release notes)
- Wear OS translations
- `freeze_strings_for_translation` step relevance

- [ ] **Step 2: Commit**

```bash
git add scripts/ai-translations/docs/release-changes-analysis.md
git commit -m "Add thorough release changes analysis document"
```

---

## Phase 3: ROI Pitch

### Task 15: Pitch Document

**Files:**
- Create: `scripts/ai-translations/docs/ai-translations-pitch.md`

- [ ] **Step 1: Write the pitch document**

Structure:
1. **Problem** — Current release requires 2 builds minimum due to GlotPress translation
   delay. Each release cycle loses 1-2 days waiting for translations.

2. **Solution** — AI-translate strings at code freeze time. Eliminate the translation wait.
   With additional changes (documented in release-changes-analysis.md), eliminate the
   rebuild entirely by using Play Store track promotion.

3. **Evidence** — Link to experiment report (`results/report.html`). Summarize headline metrics:
   - Average BLEU/chrF scores across all languages
   - LLM judge ratings
   - Best strategy + LLM combination
   - Any languages with notably lower quality

4. **Process comparison** — Current vs proposed release flow.
   Include the visual diagrams (reference the HTML visuals we created during brainstorming
   or embed ASCII art versions).

5. **Impact** — Quantify savings:
   - Time: ~1-2 days per release x 26 releases/year = 26-52 days of calendar time
   - Builds: eliminate 1 guaranteed build per release = 26 builds/year
   - Cost: GlotPress translation costs vs LLM API costs per release
   - Engineering time: release manager hours managing the extra build cycle

6. **What we built** — Link to branch `hack/ai-translations`:
   - Working translation experiment with results
   - Working production translation scripts
   - Fastlane lane + Buildkite pipeline (ready to integrate)
   - Thorough analysis of all remaining changes needed

7. **What's left to ship it** — Reference release-changes-analysis.md:
   - Version naming changes (Fastfile + release-toolkit)
   - Build routing changes (Fastfile)
   - Releases V2 scenario update (requires wpcom deploy)
   - CI setup (Claude CLI on mac-metal agents or API key)
   - One real release trial run

8. **Risks and mitigations**
   - Quality for specific languages → show per-language data
   - CLI availability on CI → can use API as fallback
   - Version naming is a convention change → WCAndroid-specific, documented path forward

- [ ] **Step 2: Commit**

```bash
git add scripts/ai-translations/docs/ai-translations-pitch.md
git commit -m "Add AI translations ROI pitch document"
```

---

## Final Wrap-Up

### Task 16: Polish and Final Commit

- [ ] **Step 1: Review all scripts are executable**

```bash
chmod +x scripts/ai-translations/experiment/*.sh
chmod +x scripts/ai-translations/evaluate/*.sh
chmod +x scripts/ai-translations/integrate/*.sh
```

- [ ] **Step 2: Verify directory structure**

```bash
find scripts/ai-translations -type f | sort
```

Expected: all files from the file map present

- [ ] **Step 3: Final commit**

```bash
git add scripts/ai-translations/
git commit -m "Polish AI translations scripts and finalize hack week deliverables"
```

- [ ] **Step 4: Verify branch state**

```bash
git log --oneline hack/ai-translations ^trunk
```

Expected: clean list of commits building up the feature