# AI-Powered Translation Automation for WooCommerce Android

## Overview

Replace GlotPress human translations with AI translations during the release process.
This eliminates the 1-2 day translation wait and the guaranteed "translation rebuild",
reducing the minimum number of builds per release from 2 to 1.

## Goals

1. **Evaluate** AI translation quality against existing human translations
2. **Integrate** AI translation into the release pipeline, removing the GlotPress dependency
3. **Document** time and cost savings to pitch the change

## Context

### Current Translation Setup

- ~3,800 translatable strings in `WooCommerce/src/main/res/values/strings.xml`
- 16 supported languages (ar, de, es, fr, he, id, it, ja, ko, nl, pt-br, ru, sv, tr, zh-cn, zh-tw)
- Translations managed via GlotPress at `translate.wordpress.com/projects/woocommerce/woocommerce-android/`
- Downloaded via `fastlane-plugin-wpmreleasetoolkit` (~13.8)

### Current Release Flow (from Releases V2 scenario)

The release is managed through `mc.a8c.com/releases-v2` which triggers Buildkite pipelines.
The scenario config lives at `wpcom-trunk/wp-content/lib/a8c/releases-v2/config/scenarios/wcandroid.php`.

**Milestones:**

1. **Code Freeze** - Create `release/X.Y` branch, bump to `X.Y-rc-1`, freeze strings,
   trigger Build #1 (rc-1 without translations)
2. **Publish Beta** - Promote draft to Play Store beta track
3. **Intermediate Beta** (repeatable) - Fix bugs, bump to rc-2/rc-3, trigger new builds
4. **Play Store Submission** - Download translations from GlotPress, then Finalize Release
   (change version from `X.Y-rc-N` to `X.Y`, trigger Build #N+1 with translations)
5. **Release** - Publish to 10% rollout
6. **Increase Rollout** - Go to 100%

**The problem:** Two things force a rebuild in step 4:
- Translations arrive 1-2 days after code freeze (GlotPress human translators)
- Version name changes from `X.Y-rc-N` to `X.Y` (requires new build)

Even with zero bugs, the minimum is always 2 builds.

### Key Files

| File | Purpose |
|------|---------|
| `fastlane/Fastfile` | All release lanes including `download_release_translations`, `finalize_release` |
| `.buildkite/release-pipelines/*.yml` | Buildkite pipelines triggered by Releases V2 |
| `.buildkite/release-builds.yml` | Actual build pipeline (calls `build_and_upload_google_play`) |
| `version.properties` | Version name and build code |
| `WooCommerce/src/main/res/values/strings.xml` | English source strings |
| `WooCommerce/src/main/res/values-*/strings.xml` | Translated strings per language |
| `wpcom-trunk/.../releases-v2/config/scenarios/wcandroid.php` | Releases V2 scenario |

---

## Phase 1: Translation Quality Evaluation

### Experiment Design

**Matrix:** 2 strategies x 2 LLMs x 16 languages = 64 translation runs

| Dimension | Values |
|-----------|--------|
| Strategies | **Naive** (plain "translate this") vs **Contextual** (length-aware + code usage context) |
| LLMs | **Claude CLI** vs **Codex CLI** (both installed locally) |
| Languages | All 16 supported languages |
| Strings | ~3,800 per language |
| Ground truth | Existing human translations in `values-*/strings.xml` |

### Translation Strategies

**Naive:** Send strings with target language, no extra instructions.

**Contextual:** For each string:
1. Instruct the LLM to keep translation length similar to the English original
2. Grep the codebase for usages of the string key (e.g., `R.string.order_detail_title`)
   to provide UI context (is it a button label? a dialog message? a title?)
3. Include this context in the prompt so the LLM understands how the string is used

### Batching

Sending 3,800 strings one-by-one would be ~60K LLM calls. Instead:
- Batch all strings for one language in a single prompt (preserving XML format)
- If the batch exceeds context window, split into chunks of ~200 strings
- Run multiple languages concurrently
- Estimated: ~20 chunks x 16 languages x 2 strategies x 2 LLMs = ~1,280 LLM calls

### Metrics

**Automated (computed per string, aggregated per language):**

| Metric | What it measures | Range |
|--------|-----------------|-------|
| BLEU | N-gram overlap with human translation | 0-100, higher = better |
| chrF | Character n-gram F-score (better for morphologically rich languages) | 0-100, higher = better |
| Normalized Levenshtein | Edit distance normalized by length | 0-1, lower = better |

**LLM-as-Judge (sampled ~100 strings per language):**

| Dimension | Scale | What it measures |
|-----------|-------|-----------------|
| Accuracy | 1-5 | Does the translation convey the same meaning? |
| Naturalness | 1-5 | Does it sound natural to a native speaker? |
| Length fit | 1-5 | Is the length appropriate for the UI context? |

Both Claude and Codex serve as judges (cross-evaluation).

### Report Output

Self-contained HTML report with interactive Plotly charts:
- **Heatmap:** Languages x Metrics grid, color-coded per strategy/LLM
- **Bar chart:** Average BLEU/chrF per strategy, grouped by LLM
- **Box plots:** Score distributions per language (variance and outliers)
- **Scatter plot:** String length vs metric score
- **Radar chart:** LLM judge dimensions per strategy
- **Table:** Worst-performing strings with examples

### File Structure

```
scripts/ai-translations/
├── experiment/
│   ├── run_experiment.sh          # Orchestrator
│   ├── translate_naive.sh         # Strategy 1: plain prompt
│   ├── translate_contextual.sh    # Strategy 2: length + context
│   ├── find_string_usage.sh       # Grep codebase for string key usage
│   ├── parse_strings.py           # strings.xml <-> JSON conversion
│   └── prompts/
│       ├── naive.txt
│       └── contextual.txt
├── evaluate/
│   ├── compute_metrics.py         # BLEU, chrF, Levenshtein (uses sacrebleu)
│   ├── llm_judge.sh              # LLM-as-judge rating
│   └── generate_report.py         # Plotly -> HTML report
└── results/                       # .gitignore'd
    ├── translations/              # Raw AI translations per strategy/LLM/lang
    ├── metrics/                   # Computed metrics JSON
    └── report.html
```

---

## Phase 2: Release Pipeline Integration

### Proposed Release Flow

1. **Code Freeze** - Create `release/X.Y` branch, use final version name `X.Y` from the start
   (no `-rc-N` suffix), bump build code
2. **AI Translate** - Translate new/changed strings immediately (minutes, not days)
3. **Complete Code Freeze** - Freeze strings, trigger Build (version `X.Y`, code 736)
   — this build already has translations and final version name
4. **Internal Testing** - Publish to internal/beta Play Store track, smoke test
5. **Bug Fix** (only if needed) - Fix, bump build code only (`X.Y`, code 737), new build
6. **Play Store Submission** - Promote existing build to production track (no rebuild)
7. **Release** - Publish GitHub release, 10% rollout
8. **Increase Rollout** - Check Sentry, go to 100%

### What Changes

**Eliminated steps:**
- "Download Release Translations" Buildkite pipeline and Fastlane lane
- "Finalize Release" rebuild (version rename + translation inclusion)
- 1-2 day wait for GlotPress
- Manual "check GlotPress translation progress" verification

**Versioning change:**
- Current: `X.Y-rc-1` → `X.Y-rc-2` → ... → `X.Y` (name changes, forces rebuild)
- Proposed: `X.Y` from the start, only build code increments (736, 737, ...)
- RC tracking becomes informal (build code identifies iteration)

### New Files

**`scripts/ai-translations/translate.sh`** - Production translation script:
1. Diff `strings.xml` against previous release tag to find new/changed strings
2. For each new string, grep codebase for usage context
3. Call Claude CLI to translate to all 16 languages (contextual strategy)
4. Output translated strings as XML fragments

**`scripts/ai-translations/merge_translations.py`** - Merge AI translations:
1. Read existing `values-*/strings.xml` files
2. Insert/update translated strings
3. Preserve XML formatting and non-translatable strings

**`.buildkite/release-pipelines/ai-translate-strings.yml`** - Buildkite pipeline:
- Same pattern as `download-release-translations.yml`
- Agent queue: `mac-metal`
- Calls new Fastlane lane

### Modified Files

**`fastlane/Fastfile`:**
- New lane `ai_translate_strings` - calls `translate.sh` + `merge_translations.py`,
  commits and pushes
- Modify `start_code_freeze` - use final version name (no rc suffix)
- Remove or simplify `finalize_release` - no version rename, just close milestone
  and create GitHub release
- Remove `download_release_translations` lane (or keep as fallback)

**`wpcom-trunk/.../scenarios/wcandroid.php`** (Releases V2):
- Add "AI Translate Strings" Buildkite button to Code Freeze milestone
- Remove "Download Release Translations" button from Play Store Submission
- Replace "Finalize Release" button with a simpler "Promote Build" task
- Update descriptions and Slack messages (no more rc-N references)

**Release toolkit considerations:**
- `RCNotationVersionFormatter` would no longer be used for this product
- `start_code_freeze` needs to set final version name directly
- `MarketingVersionCalculator` may need adjustment

---

## Phase 3: ROI Pitch Document

### Data to Collect

**Time savings:**
- Current: 1-2 day wait for translations + ~30 min for finalize build + ~30 min testing the new build
- Proposed: ~5-10 minutes for AI translation (runs during code freeze)
- Per release: ~1-2 days saved
- Per year (biweekly releases, ~26/year): ~26-52 days of calendar time saved

**Cost savings:**
- GlotPress translation costs (if paid per-string or per-project)
- Engineering time spent waiting and managing the extra build
- CI/CD costs for the eliminated build

**Quality evidence:**
- Link to Phase 1 experiment report
- Summary of BLEU/chrF scores
- LLM judge ratings

### Output

`scripts/ai-translations/docs/ai-translations-pitch.md` containing:
- Problem statement with current flow diagram
- Proposed solution with new flow diagram
- Quality evidence (embedded charts or links to report)
- Time and cost analysis
- Implementation status (links to branch, PRs)
- Risks and mitigations

---

## Branch Strategy

All work lives on `hack/ai-translations` branch. Structure:
- Experiment scripts and evaluate scripts are committed to the branch
- Results directory is gitignored
- Generated report HTML is committed for easy sharing
- Integration changes (Fastlane, Buildkite) are committed as working proposals
- Pitch document is the final deliverable

---

## Risks and Open Questions

1. **AI translation quality for CJK languages** - Japanese, Korean, Chinese may have
   lower scores due to fundamentally different grammar. The experiment will surface this.

2. **Claude/Codex CLI availability on CI** - Buildkite `mac-metal` agents may not have
   these CLIs installed. May need to use API calls instead for the production integration.

3. **Context window limits** - 3,800 strings may not fit in one prompt. Chunking strategy
   handles this but needs testing.

4. **Releases V2 changes** - Modifying the scenario file requires a deploy to wpcom.
   Need to coordinate with Apps Infrastructure team.

5. **Version naming convention** - Dropping RC suffix is a cross-team convention change.
   Other WP mobile apps use the same pattern. This proposal is WCAndroid-specific but
   could set precedent.

6. **Play Store metadata translations** - Current flow also translates Play Store listings
   (title, description, release notes). AI translation should cover these too.