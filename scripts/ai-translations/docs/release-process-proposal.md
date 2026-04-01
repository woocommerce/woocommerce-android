# Proposal: AI Translations in the WooCommerce Android Release Pipeline

This is a follow-up to [the experiment post](https://peacockp2.wordpress.com/2026/03/31/hack-experiment-can-ai-replace-translators/) where I showed that AI translations are preferred over human ones in a blind evaluation across all 16 languages.

There is already [active work towards AI translations across teams](#related-work). I won't implement the release pipeline integration myself, but during hack week I spent time analyzing the exact changes needed for WooCommerce Android. I tried to make this document specific enough that it can be used as a spec - either by a person or by an AI agent - to implement the changes.

## The Current Release Flow

Today a WooCommerce Android release goes through these steps (managed via Releases V2 + Buildkite). Version **name** is what users see (e.g., "24.4"). Version **code** is an integer that Play Store uses for ordering (e.g., 736).

```
┌─────────────────────────────────────────────────────────────────────┐
│ MILESTONE: Code Freeze                                             │
│                                                                    │
│ 1. Start Code Freeze           [Buildkite: start-code-freeze.yml] │
│    Create release/X.Y branch                                       │
│    Version name: X.Y-rc-1    Version code: 736                     │
│                                                                    │
│ 2. Complete Code Freeze    [Buildkite: complete-code-freeze.yml]   │
│    Freeze strings, merge library strings                           │
│    🔨 BUILD #1 (name "X.Y-rc-1", code 736)                        │
│    ❌ No translations in this build                                │
├─────────────────────────────────────────────────────────────────────┤
│ MILESTONE: Publish Beta                                            │
│                                                                    │
│ 3. Promote draft to Play Store beta track, test APK                │
├─────────────────────────────────────────────────────────────────────┤
│ MILESTONE: Intermediate Beta (repeatable)                          │
│                                                                    │
│ 4. Bug Fix Beta (if needed)    [Buildkite: new-beta-release.yml]   │
│    Version name: X.Y-rc-2    Version code: 737                     │
│    🔨 BUILD #2 (optional, still no translations)                   │
├─────────────────────────────────────────────────────────────────────┤
│ MILESTONE: Play Store Submission                                   │
│                                                                    │
│ 5. ⏳ Wait 1-2 days for GlotPress human translations              │
│                                                                    │
│ 6. Download Translations  [Buildkite: download-release-translations│
│    Pull from GlotPress, commit to release branch        .yml]      │
│                                                                    │
│ 7. Finalize Release        [Buildkite: finalize-release.yml]       │
│    Version name: X.Y-rc-N → X.Y    Version code: 738              │
│    🔨 BUILD #3 (with translations + final version name)            │
│    ⚠️  This build is ALWAYS needed, even with zero bugs            │
│                                                                    │
│ 8. Test final APK, submit to Play Store production track           │
├─────────────────────────────────────────────────────────────────────┤
│ MILESTONE: Release                                                 │
│                                                                    │
│ 9. Publish GitHub release      [Buildkite: publish-release.yml]    │
├─────────────────────────────────────────────────────────────────────┤
│ MILESTONE: Increase Rollout                                        │
│                                                                    │
│ 10. 10% rollout → Sentry check → 100%                             │
└─────────────────────────────────────────────────────────────────────┘

Minimum builds: 2 (always)
Typical builds: 3+
```

Two things force the second build:
1. **Translations arrive late** - GlotPress translators need 1-2 days after code freeze
2. **Version name changes** - `finalize_release` renames `X.Y-rc-N` → `X.Y`, requiring a new binary

## Proposed Flow

Translate strings with AI at code freeze time and use the final version name from the start. This way the first build can be promoted to production as-is.

```
┌─────────────────────────────────────────────────────────────────────┐
│ MILESTONE: Code Freeze                                             │
│                                                                    │
│ 1. Start Code Freeze           [Buildkite: start-code-freeze.yml] │
│    Create release/X.Y branch                                       │
│    Version name: X.Y         Version code: 736                     │
│    (final name from the start, no -rc-N suffix)                    │
│                                                                    │
│ 2. ⚡ AI Translate Strings  [Buildkite: ai-translate-strings.yml]  │
│    Translate new/changed strings to 16 languages (~2 min)          │
│    Commit translations to release branch                           │
│                                                                    │
│ 3. Complete Code Freeze    [Buildkite: complete-code-freeze.yml]   │
│    Freeze strings, merge library strings                           │
│    🔨 BUILD #1 (name "X.Y", code 736)                             │
│    ✅ Has translations + final version name                        │
├─────────────────────────────────────────────────────────────────────┤
│ MILESTONE: Publish Beta                                            │
│                                                                    │
│ 4. Promote to internal/beta track, test                            │
├─────────────────────────────────────────────────────────────────────┤
│ MILESTONE: Intermediate Beta (repeatable, only if bugs found)      │
│                                                                    │
│ 5. Bug Fix                     [Buildkite: new-beta-release.yml]   │
│    Version name: X.Y (same)  Version code: 737                     │
│    🔨 BUILD #2 (optional, still has translations)                  │
├─────────────────────────────────────────────────────────────────────┤
│ MILESTONE: Play Store Submission                                   │
│                                                                    │
│ 6. ✅ Promote to Production                                        │
│    Promote tested build: beta → production track                   │
│    Same binary, no rebuild                                         │
│                                                                    │
│ 7. Finalize (admin only)                                           │
│    Close milestone, remove branch protection,                      │
│    backmerge PR, publish GitHub release                            │
│    No build triggered                                              │
├─────────────────────────────────────────────────────────────────────┤
│ MILESTONE: Release + Increase Rollout                              │
│                                                                    │
│ 8. 10% rollout → Sentry check → 100%                              │
└─────────────────────────────────────────────────────────────────────┘

Minimum builds: 1
Typical builds: 1-2
```

### What this saves

- 1-2 day GlotPress translation wait (every release)
- 1 guaranteed build per release (the finalize rebuild)
- Release manager coordination with i18n team
- ~26-52 calendar days per year across 26 biweekly releases

## Specific Changes Needed

This section lists every file and function that needs to change, with line numbers. The goal is that this can be used directly as an implementation spec.

### 1. New: AI translation script

Create `scripts/ai-translations/integrate/translate.py`:
- Import `parse_strings.py` (already exists on the `hack/ai-translations` branch) to parse `strings.xml`
- Diff strings against previous release tag (`git describe --tags --abbrev=0`) to find new/changed keys
- Use naive prompt (experiment showed code context doesn't improve quality)
- Call Anthropic API via Python SDK. `ANTHROPIC_API_KEY` already in Buildkite secrets.
- Translate to all 16 languages in parallel, one API call per language
- Output per-language JSON files

Create `scripts/ai-translations/integrate/merge_translations.py`:
- Read AI translation JSON files
- Validate: check format placeholders preserved, XML well-formedness
- Merge into `WooCommerce/src/main/res/values-{locale}/strings.xml`
- Handle locale mapping (`pt-br` → `values-pt-rBR`) and legacy dirs (`values-iw`, `values-in`)

### 2. New: Fastlane lane

Add to `fastlane/Fastfile` (after `download_translations` lane, ~line 475):

```ruby
lane :ai_translate_strings do |skip_confirm: false|
  ensure_git_status_clean
  ensure_git_branch_is_release_branch!
  configure_apply(force: is_ci)  # sets up secrets

  # venv setup + run translate.py + merge_translations.py
  # commit and push translated strings.xml files
end
```

Must follow existing lane patterns: accept `skip_confirm`, call `configure_apply`, commit and push.

### 3. New: Buildkite pipeline

Create `.buildkite/release-pipelines/ai-translate-strings.yml` (same pattern as `download-release-translations.yml`):
- Agent queue: `mac-metal`
- Checkout release branch, setup Ruby, run `bundle exec fastlane ai_translate_strings skip_confirm:true`
- Include `reason` in retry block (matches existing pipeline pattern)

### 4. Modify: `start_code_freeze` (Fastfile line 132)

Currently at line 141:
```ruby
new_beta_version = beta_version_next(version_name: new_version)
```
This sets the version to `X.Y-rc-1`.

**Change:** Set version name to `X.Y` directly (the final release name) and bump version code. Do not call `beta_version_next`.

### 5. Modify: `build_and_upload_google_play` (Fastfile line 648)

Currently at line 650:
```ruby
if beta_version?(version_name_current)
```
This checks for `-rc-` in the version name to decide beta vs production track. Without RC suffix, all builds route to production.

**Change:** Pass an explicit `track` parameter instead of inferring from version name. Add `TRACK` to the environment variables in `trigger_release_build` (line 777):
```ruby
environment = {
  INCLUDE_WEAR_APP: include_wear_app,
  RELEASE_VERSION: release_version_current,
  TRACK: track  # "beta" or "production"
}
```
Then in `build_and_upload_google_play`, read from env instead of parsing version name.

### 6. Modify: `new_beta_release` (Fastfile line 316)

Currently at line 338:
```ruby
VERSION_FILE.write_version(
  version_name: beta_version_next,
  version_code: build_code_next
)
```
`beta_version_next` (line 1638) uses `RCNotationVersionFormatter` to parse `X.Y-rc-1` → `X.Y-rc-2`. Without RC suffix this breaks.

**Change:** Keep version name unchanged, only increment version code:
```ruby
VERSION_FILE.write_version(
  version_name: release_version_current,  # stays "X.Y"
  version_code: build_code_next
)
```

### 7. Modify: `finalize_release` (Fastfile line 519)

Currently does 5 things:
- Line 532: Writes final version name (`X.Y`) and bumps build code
- Line 547: Triggers build via `trigger_release_build`
- Line 549: Creates backmerge PR
- Line 551: Removes branch protection
- Line 563: Closes milestone

**Change:** Split into two lanes:
- `promote_to_production` - promotes the existing build from beta to production track in Play Store (new)
- `finalize_release` - keeps only: backmerge PR, remove branch protection, close milestone. No version change, no build trigger.

### 8. Modify: `beta_version?` (Fastfile line 1538)

Currently:
```ruby
def beta_version?(version)
  version.include? '-rc-'
end
```

**Change:** Remove or replace. With explicit track routing (change #5), this function is no longer needed for track decisions. Other usages in the Fastfile should be audited.

### 9. Modify: Releases V2 scenario (`wcandroid.php`)

Current Buildkite buttons in the scenario and what changes:

| Milestone | Current Button | Change |
|-----------|---------------|--------|
| Code Freeze | "Start Code Freeze" (line 48) | Update description: version is `X.Y` not `X.Y-rc-1` |
| Code Freeze | "Complete Code Freeze" (line 103) | Keep, but add **"AI Translate Strings"** button before it (new, triggers `ai-translate-strings.yml`) |
| Intermediate Beta | "New Beta Release" (line 172) | Update description: only bumps version code, not version name |
| Play Store Submission | "Download Translations" (line 236) | **Remove** (translations already done at code freeze) |
| Play Store Submission | "Finalize Release" (line 255) | **Replace** with "Promote to Production" (no build) + "Finalize" (admin only) |
| Release | "Publish GitHub Release" (line 341) | Keep as-is |

Also update Slack messages (lines 156-159, 203-209, 366-373) to remove `rc-1` references.

### 10. Modify: release-toolkit gem

`fastlane-plugin-wpmreleasetoolkit` (github.com/wordpress-mobile/release-toolkit):
- `RCNotationVersionFormatter` - no longer used for WCAndroid
- `beta_version?` / `beta_version_next` helpers - need an alternative path for products that don't use RC naming

This is the change with the widest impact since the gem is shared across mobile apps. The change should be additive (support both RC and non-RC products), not breaking.

### The version naming question

Dropping `-rc-N` from the version name is the key to avoiding the finalize rebuild. A few things to figure out:

- **Internal communication**: use version code to identify iterations ("X.Y build 736", "X.Y build 737"). The `-rc-N` label can still be used informally in Slack/Releases V2 without being in the actual `versionName`.
- **Play Store console**: beta builds show as "X.Y" with different version codes. This is fine - Play Store uses version codes for ordering. Users would see "X.Y" in app settings, which is better than "X.Y-rc-1".
- **GitHub releases**: drafts created during beta can use tags like `X.Y-beta.1`, `X.Y-beta.2` to distinguish iterations, with the final release tagged as `X.Y`.

### Alternative: in-PR translation

Instead of translating at code freeze, translate in each PR that touches `strings.xml`. @plokhoves [built this for Day One Android](https://dayoneandroidp2.wordpress.com/2026/03/26/rfc-moving-string-translations-from-glotpress-to-ai/). Translations are always current in trunk, no translation step needed during release at all. More CI compute but even simpler release flow. Could be combined with the proposed flow.

## Advantages of AI Translation

Regardless of the approach:

- **Speed**: minutes instead of days. No release blocked on translations.
- **Consistency**: one prompt applies to all 16 languages. Want shorter translations? One line change. Want a glossary of e-commerce terms? Add it once.
- **Cost**: under $10/year for all 16 languages across 26 releases ([details](https://peacockp2.wordpress.com/2026/03/31/hack-experiment-can-ai-replace-translators/)).
- **Quality control**: the prompt can enforce length limits, domain terminology, and tone uniformly.
- **Scalability**: adding a new language is a one-line change.
- **Iteration**: fix the prompt, re-run. The fix applies to all future translations.

## What's Already Built

The experiment branch ([woocommerce/woocommerce-android#15588](https://github.com/woocommerce/woocommerce-android/pull/15588)) has reusable scripts:

- `parse_strings.py` - Android `strings.xml` parser (handles plurals, format placeholders, HTML entities)
- `translate_fast.py` - translation with chunking and parallel execution
- Implementation plan with the analysis above in more detail (`docs/superpowers/plans/2026-03-30-ai-translations.md`)

## Related Work

- **@plokhoves** - [in-PR AI translation for Day One Android](https://dayoneandroidp2.wordpress.com/2026/03/26/rfc-moving-string-translations-from-glotpress-to-ai/) using Claude API
- **@iangmaia** - AI-Powered Translation Context Extraction - gem/CLI tool that enriches string comments with code context
- **@joshheald** and the AI enablement cohort - AI translations with LLM-as-Judge evaluation in langfuse
- **@alexgrebenyuk** - [Continuous Translation proposal](https://appsinfrap2.wordpress.com/2026/03/10/proposal-continuous-translation) - translating in trunk continuously
- **@sorinnunca** - GlotPress data showing AI suggestions (GPT-3.5/GPT-4o-mini) accepted without edits 46-58% of the time
