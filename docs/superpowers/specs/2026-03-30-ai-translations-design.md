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

### Hack Week Scope

Phase 2 has two scopes:
- **Build** the AI translation scripts and Fastlane/Buildkite integration (working code)
- **Document** all required changes to achieve the full single-build optimization

We do NOT need to implement all the version naming and build routing changes during
hack week. The goal is to prove the concept works and provide a thorough analysis of
what needs to change to ship it.

### Proposed Release Flow (Target State)

Use Play Store track promotion to eliminate rebuilds entirely:

1. **Start Code Freeze** - Create `release/X.Y` branch, version name `X.Y` from the start
   (no `-rc-N` suffix), bump build code
2. **AI Translate Strings** - Translate new/changed strings immediately (~minutes)
3. **Complete Code Freeze** - Freeze strings, trigger Build (`X.Y`, code 736)
   — uploaded to internal/beta Play Store track
4. **Internal Testing** - Smoke test from beta track
5. **Bug Fix** (only if needed) - Fix, bump build code (`X.Y`, code 737), new build to beta
6. **Promote to Production** - Promote the tested build from beta → production track
   (same binary, no rebuild)
7. **Finalize** (admin only) - Close milestone, remove branch protection, backmerge PR,
   publish GitHub release
8. **Increase Rollout** - Check Sentry, 10% → 100%

### What This Eliminates

- 1-2 day wait for GlotPress translators
- "Download Release Translations" step
- "Finalize Release" rebuild (currently triggers a new build just for version rename)
- RC naming convention (no more `-rc-1`, `-rc-2`)

### Required Changes (Documented, Not All Implemented in Hack Week)

**1. Version naming — drop RC suffix**

`start_code_freeze` currently sets version to `X.Y-rc-1`. Must set `X.Y` directly.

Affected code:
- `fastlane/Fastfile` `start_code_freeze` lane — version bump logic
- `Fastlane::Wpmreleasetoolkit::Versioning::RCNotationVersionFormatter` — no longer used
- `version.properties` format — no more `-rc-N` suffix

**2. Build routing — decouple from version name**

`build_and_upload_google_play` (Fastfile ~line 650) uses `beta_version?(version_name_current)`
which checks for `-rc-` in the version name to decide beta vs production track.
Without RC, this routes ALL builds to production. Must change to explicit track parameter.

Options:
- Pass `track` parameter through `trigger_release_build` → environment variable → lane
- Or use a `release_stage` property in `version.properties` separate from `versionName`

**3. Beta iteration — replace RC incrementing**

`new_beta_release` calls `beta_version_next` which uses `RCNotationVersionFormatter` to
parse `X.Y-rc-1` → `X.Y-rc-2`. Without RC, just increment build code.

Affected code:
- `fastlane/Fastfile` `new_beta_release` lane (~line 338)
- `beta_version_next` helper function
- `RCNotationVersionFormatter` in release-toolkit gem

**4. Finalize release — strip build trigger, keep admin tasks**

`finalize_release` currently does 5 things:
1. ~~Changes version from `X.Y-rc-N` to `X.Y`~~ (not needed — already `X.Y`)
2. ~~Bumps build code~~ (not needed — no version change)
3. ~~Triggers final build~~ (not needed — promote existing build instead)
4. Creates backmerge PR ← keep
5. Removes branch protection, closes milestone ← keep

Split into: `promote_to_production` (Play Store promotion) + `finalize_release` (admin only).

**5. Releases V2 scenario — update wcandroid.php**

Located at: `wpcom-trunk/wp-content/lib/a8c/releases-v2/config/scenarios/wcandroid.php`

Changes:
- Code Freeze milestone: add "AI Translate Strings" Buildkite button
- Play Store Submission: replace "Download Translations" + "Finalize Release" with
  "Promote to Production" + "Finalize (admin)"
- Update all Slack messages (remove `rc-1` references)
- Update descriptions to reflect new flow

**6. Play Store metadata translations (deferred)**

Current flow also translates Play Store listings (app title, description, release notes)
via GlotPress. These go to `fastlane/metadata/android/<locale>/` directories. Can be
added to AI translation as a separate batch but deferred from hack week.

**7. Wear OS translations (deferred)**

`WooCommerce-Wear/` currently has no translated `values-*` directories. Out of scope.

### What We Actually Build in Hack Week

**New files:**

`scripts/ai-translations/integrate/translate.sh` — Production translation script:
1. Diff `strings.xml` against previous release tag to find new/changed strings
2. Batch-grep codebase for usage context (not per-string, one pass)
3. Call Claude CLI to translate to all 16 languages
4. Output per-language JSON files

`scripts/ai-translations/integrate/merge_translations.py` — Merge AI translations:
1. Read existing `values-*/strings.xml` files
2. Insert/update translated strings, preserve formatting
3. Handle locale mapping (`pt-br` → `values-pt-rBR`) and legacy dirs (`values-in`/`values-iw`)

`.buildkite/release-pipelines/ai-translate-strings.yml` — Buildkite pipeline

`fastlane/Fastfile` — New `ai_translate_strings` lane (follows existing lane patterns:
accepts `skip_confirm`, calls `configure_apply`, commits and pushes)

`scripts/ai-translations/docs/release-changes-analysis.md` — Thorough analysis of all
required changes listed above, with code references, line numbers, and impact assessment

---

## Phase 3: ROI Pitch Document

### Data to Collect

**Time savings:**
- Current: 1-2 day wait for translations + ~30 min finalize build + ~30 min testing
- Proposed: ~5-10 minutes for AI translation at code freeze time
- Per release: ~1-2 days of calendar time saved
- Per year (biweekly releases, ~26/year): ~26-52 days of calendar time saved
- Build elimination: 1 guaranteed build per release = ~26 builds/year saved

**Cost savings:**
- GlotPress translation costs (if paid)
- Engineering time managing the extra build cycle
- CI/CD compute costs for eliminated builds

**Quality evidence:**
- Link to Phase 1 experiment report
- Summary of BLEU/chrF scores
- LLM judge ratings

### Output

`scripts/ai-translations/docs/ai-translations-pitch.md` containing:
- Problem statement with current flow diagram
- Proposed solution with new flow diagram (Phase 2a: AI translation, Phase 2b: single build)
- Quality evidence (embedded charts or links to report)
- Time and cost analysis
- Required changes analysis (what needs to change and where)
- Implementation status (links to branch, working code)
- Risks and mitigations

---

## Branch Strategy

All work lives on `hack/ai-translations` branch. Structure:
- Experiment scripts and evaluate scripts — committed (working code)
- Integration scripts — committed (working code)
- Results directory — gitignored (except `report.html` committed via `git add -f`)
- Release changes analysis — committed (documentation)
- Pitch document — committed (final deliverable)

---

## Parsing Requirements

The string parser (`parse_strings.py`) must handle:
- `<string>` elements — the bulk of translations
- `<plurals>` elements — language-specific plural forms (Arabic has 6, Russian has 3, etc.)
- Skip `translatable="false"` strings only (do NOT blanket-skip `content_override="true"` —
  some `content_override` strings are translatable)
- Skip `<string-array>` elements (currently all non-translatable in this project)
- Preserve format placeholders (`%1$s`, `%1$d`, `%s`, `%d`)
- Preserve HTML entities (`&lt;b&gt;`, `&lt;/b&gt;`, CDATA sections)

---

## Metrics Note

The implementation uses `Levenshtein.ratio()` which returns a similarity score (0-1,
higher = more similar to human translation). This is labeled "Levenshtein Similarity"
in the report. Not to be confused with raw Levenshtein distance (lower = closer).

---

## Risks and Open Questions

1. **AI translation quality for CJK languages** - Japanese, Korean, Chinese may have
   lower scores due to fundamentally different grammar. The experiment will surface this.

2. **Claude/Codex CLI availability on CI** - Buildkite `mac-metal` agents may not have
   these CLIs installed. May need to use API calls instead for production.

3. **Context window limits** - 3,800 strings may not fit in one prompt. Chunking needed.

4. **Plural forms** - Arabic has 6 plural forms, Russian has 3, etc. The AI must generate
   the correct number of variants per language. Post-translation validation needed.

5. **Format string safety** - Mistranslated format strings (`%1$s`, `%d`) cause runtime
   crashes. Validation must verify all placeholders are preserved.

6. **Initial full translation** - First run translates all ~3,800 strings (no previous
   baseline). Subsequent runs are incremental (only new/changed strings).

7. **LLM output validation** - LLM may return malformed JSON, skip strings, or duplicate
   entries. Merge script must validate: same keys in/out, XML well-formedness.

8. **Releases V2 changes require wpcom deploy** - Scenario file changes need coordination
   with Apps Infrastructure team.