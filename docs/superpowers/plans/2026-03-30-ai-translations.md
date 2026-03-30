# AI Translation Automation — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Evaluate AI translation quality against human translations, integrate AI translation into the release pipeline to eliminate the 1-2 day GlotPress wait and the guaranteed translation rebuild, and document ROI.

**Architecture:** Shell scripts orchestrate Claude/Codex CLI calls for translation. Python scripts handle XML parsing, metrics computation (sacrebleu), and report generation (Plotly). Integration touches Fastlane lanes and Buildkite pipelines. All code lives on `hack/ai-translations` branch under `scripts/ai-translations/`.

**Tech Stack:** Bash, Python 3.14 (sacrebleu, plotly, Levenshtein), Claude CLI, Codex CLI, Fastlane (Ruby), Buildkite YAML

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
- Some strings have `translatable="false"` — skip these
- Some strings have `content_override="true"` — skip these too
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

Takes a string key (e.g., `order_detail_title`) and outputs code snippets where it's used.

Search patterns:
- `R.string.{key}` in Kotlin/Java files
- `@string/{key}` in XML layout files
- Limit output to 5 most relevant matches (first line of each match + surrounding context)

Usage:
```bash
./find_string_usage.sh order_detail_title
# Output: file:line snippets showing how the string is used
```

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
Return ONLY a JSON array of objects with "key" and "value" fields.
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

Return ONLY a JSON array of objects with "key" and "value" fields.

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
   - Codex: `echo "$PROMPT" | codex --full-auto --quiet`
5. Parse JSON response, merge chunks
6. Save to `results/translations/naive/{llm}/{lang_code}.json`

- [ ] **Step 2: Write translate_contextual.sh**

Same arguments and flow as naive, but:
1. For each string, call `find_string_usage.sh` to get code context
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
git add results/report.html
git commit -m "Add AI translation evaluation report"
```

---

## Phase 2: Release Pipeline Integration

### Task 11: Production Translation Script

**Files:**
- Create: `scripts/ai-translations/integrate/translate.sh`

- [ ] **Step 1: Write translate.sh**

This is the production-ready script that translates only new/changed strings.

Arguments: `[--previous-tag TAG]` (defaults to latest release tag)

Flow:
1. Find previous release tag: `git describe --tags --abbrev=0 --match="*.*"` (or use provided tag)
2. Diff `strings.xml` between current and previous tag:
   ```bash
   # Extract keys from previous version
   git show "$PREV_TAG:WooCommerce/src/main/res/values/strings.xml" > /tmp/prev_strings.xml
   python3 ../experiment/parse_strings.py extract /tmp/prev_strings.xml > /tmp/prev.json
   python3 ../experiment/parse_strings.py extract WooCommerce/src/main/res/values/strings.xml > /tmp/current.json
   ```
3. Compute diff: new keys and changed values
4. For each new/changed string, get code usage context via `find_string_usage.sh`
5. Build prompt using contextual template
6. Call Claude CLI to translate to all 16 languages
7. Output per-language JSON files to a temp directory

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
2. Read existing `values-{locale}/strings.xml`
3. For each translated string:
   - If key exists: update the value
   - If key is new: insert at the same position as in the English file
4. Write the updated XML, preserving:
   - XML declaration and encoding
   - GlotPress comment headers (update Translation-Revision-Date)
   - Existing string ordering
   - `tools:` namespace attributes

Language code to Android locale mapping:
```python
LOCALE_MAP = {
    "ar": "ar", "de": "de", "es": "es", "fr": "fr",
    "he": "he", "id": "id", "it": "it", "ja": "ja",
    "ko": "ko", "nl": "nl", "pt-br": "pt-rBR", "ru": "ru",
    "sv": "sv", "tr": "tr", "zh-cn": "zh-rCN", "zh-tw": "zh-rTW"
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

Add after the existing `download_translations` lane (~line 475):

```ruby
#####################################################################################
# ai_translate_strings
# -----------------------------------------------------------------------------------
# This lane translates new/changed strings using AI and updates the strings.xml files
# -----------------------------------------------------------------------------------
# Usage:
# bundle exec fastlane ai_translate_strings
#####################################################################################
desc 'Translate new/changed strings using AI and update the strings.xml files'
lane :ai_translate_strings do
  ensure_git_status_clean
  ensure_git_branch_is_release_branch!

  UI.important("Translating strings using AI for release: #{release_version_current}")

  # Run the AI translation script
  sh('bash', '../scripts/ai-translations/integrate/translate.sh')

  # Merge translations into strings.xml files
  sh('python3', '../scripts/ai-translations/integrate/merge_translations.py',
     '--translations-dir', '/tmp/ai-translations/',
     '--res-dir', '../WooCommerce/src/main/res/')

  # Commit the changes
  git_add(path: 'WooCommerce/src/main/res/values-*/')
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

      echo '--- :snake: Setup Python'
      pip3 install -r scripts/ai-translations/requirements.txt

      echo '--- :globe_with_meridians: AI Translate Strings'
      bundle exec fastlane ai_translate_strings skip_confirm:true
    agents:
        queue: "mac-metal"
    retry:
      manual:
        allowed: false
```

- [ ] **Step 3: Commit**

```bash
git add fastlane/Fastfile .buildkite/release-pipelines/ai-translate-strings.yml
git commit -m "Add AI translation Fastlane lane and Buildkite pipeline"
```

---

### Task 14: Releases V2 Scenario Update (Proposal)

**Files:**
- Document proposed changes (not modifiable from this repo)

- [ ] **Step 1: Write proposed changes for wcandroid.php**

Create `scripts/ai-translations/docs/releases-v2-proposed-changes.md` documenting exactly what needs to change in `wpcom-trunk/wp-content/lib/a8c/releases-v2/config/scenarios/wcandroid.php`:

1. **Code Freeze milestone — add AI Translate step after Complete Code Freeze:**
   ```php
   Task::buildkite(
       'Press the button to translate new strings using AI.',
       'This will call <code>bundle exec fastlane ai_translate_strings</code> on CI, which will:<ul>'
       . '<li>Identify new/changed strings since the previous release</li>'
       . '<li>Translate them to all 16 supported languages using AI</li>'
       . '<li>Merge translations into the localized <code>strings.xml</code> files</li>'
       . '<li>Commit and push to the <code>release/%version%</code> branch</li>'
       . '</ul>',
       new Buildkite_Action(
           'woocommerce-android',
           'release/%version%',
           [
               'PIPELINE' => 'release-pipelines/ai-translate-strings.yml',
               'RELEASE_VERSION' => '%version%',
               'BUILDKITE_MESSAGE' => 'AI Translate Strings',
           ]
       )
   )->retryable(),
   ```

2. **Play Store Submission milestone — remove Download Translations + Finalize rebuild:**
   - Remove the "Download Release Translations" Buildkite task
   - Replace "Finalize Release" with a simpler task that just closes the milestone and creates GitHub release (no version rename, no rebuild)
   - The "Test and Submit Build" section would use the existing beta build

3. **Versioning — remove RC suffix:**
   - Update `start_code_freeze` Buildkite action description to reflect no `-rc-N` naming
   - Update all Slack messages to not reference `rc-1`
   - `new_beta_release` would only bump build code, not version name

- [ ] **Step 2: Commit**

```bash
git add scripts/ai-translations/docs/releases-v2-proposed-changes.md
git commit -m "Document proposed Releases V2 scenario changes"
```

---

## Phase 3: ROI Pitch

### Task 15: Pitch Document

**Files:**
- Create: `scripts/ai-translations/docs/ai-translations-pitch.md`

- [ ] **Step 1: Write the pitch document**

Structure:
1. **Problem** — Current release requires 2 builds minimum due to GlotPress translation delay. Each release cycle loses 1-2 days waiting for translations.

2. **Solution** — AI-translate strings at code freeze time. Use final version name from the start. Eliminate the guaranteed translation rebuild.

3. **Evidence** — Link to experiment report (`results/report.html`). Summarize headline metrics:
   - Average BLEU/chrF scores across all languages
   - LLM judge ratings
   - Best strategy + LLM combination
   - Any languages with notably lower quality

4. **Process comparison** — Current vs proposed release flow (embed diagrams or ASCII art).
   - Current: Code Freeze → Beta (no translations) → Wait 1-2 days → Download translations → Finalize (rebuild) → Submit
   - Proposed: Code Freeze → AI Translate (minutes) → Build (with translations) → Test → Promote → Submit

5. **Impact** — Quantify savings:
   - Time: ~1-2 days per release x 26 releases/year = 26-52 days of calendar time
   - Builds: eliminate 1 guaranteed build per release = 26 builds/year
   - Cost: GlotPress translation costs (document if available) vs LLM API costs
   - Engineering time: release manager time spent managing the extra build cycle

6. **Implementation status** — Link to branch `hack/ai-translations`, list PRs and files.

7. **Risks and mitigations**
   - Quality for specific languages → show per-language data
   - CLI availability on CI → can use API as fallback
   - Cross-team impact (version naming) → WCAndroid-specific change, no impact on other apps

8. **Next steps** — What needs to happen to ship this:
   - Apps Infra team review of Releases V2 changes
   - Release toolkit changes for version naming
   - CI setup (install Claude CLI on mac-metal agents)
   - One real release trial run

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