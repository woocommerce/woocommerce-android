# Proposal: AI Translations in the WooCommerce Android Release Pipeline

This is a follow-up to [the experiment post](https://peacockp2.wordpress.com/2026/03/31/hack-experiment-can-ai-replace-translators/) where I showed that AI translations are preferred over human ones in a blind evaluation across all 16 languages. Here I describe what changes are needed to integrate AI translations into the release pipeline and what that would give us.

## Related Work

After posting the experiment, I learned there is already active work in this area across teams:

- **@plokhoves** built an [in-PR AI translation workflow for Day One Android](https://dayoneandroidp2.wordpress.com/2026/03/26/rfc-moving-string-translations-from-glotpress-to-ai/) using Claude API that translates string deltas in 30-60 seconds
- **@iangmaia** is working on [AI-Powered Translation Context Extraction](https://fieldguide.automattic.com/...) - a gem/CLI tool that enriches string comments with code context to help both human and AI translators
- **@joshheald** and the AI enablement cohort are exploring AI translations with LLM-as-Judge evaluation in langfuse
- **@alexgrebenyuk** proposed [Continuous Translation](https://appsinfrap2.wordpress.com/2026/03/10/proposal-continuous-translation) - translating in trunk continuously instead of during release
- **@sorinnunca** shared data from GlotPress where AI translation suggestions (GPT-3.5/GPT-4o-mini) were accepted without edits 46-58% of the time

Given this existing momentum, I won't implement the release pipeline integration myself. But during the hack week I spent time thinking through the exact changes needed and put together a plan based on our release infrastructure. I think it can be useful as a reference for whoever picks this up, so here is what I propose.

## The Current Release Flow

Today a release goes through these steps (managed via Releases V2 + Buildkite):

1. **Start Code Freeze** - create `release/X.Y` branch, bump version to `X.Y-rc-1`
2. **Complete Code Freeze** - freeze strings, trigger **Build #1** (rc-1, no translations)
3. **Publish Beta** - promote to Play Store beta track, test
4. **Intermediate Beta** (if bugs found) - fix, bump to rc-2, **Build #2**
5. **Wait for GlotPress translations** (1-2 days)
6. **Download Translations** - pull from GlotPress, commit to release branch
7. **Finalize Release** - rename version from `X.Y-rc-N` to `X.Y`, bump build code, trigger **Build #3** (with translations)
8. **Test and Submit** - test the new build, submit to Play Store production track
9. **Publish + Rollout** - GitHub release, 10% rollout, check Sentry, go to 100%

The problem is in steps 5-7. Even if there are no bugs, the release always needs at least **two builds**: one without translations (rc-1) and one with (after finalize). The 1-2 day wait for GlotPress translators adds calendar time to every release.

## What AI Translation Changes

If we translate strings with AI at code freeze time (minutes instead of days), the first build already has translations. This opens the door to a simpler flow.

### Option A: Drop-in replacement (smallest change)

Replace the GlotPress download with an AI translation step. Everything else stays the same.

1. Start Code Freeze - same as today
2. **AI Translate Strings** (new) - translate new/changed strings, commit to release branch
3. Complete Code Freeze - same, but **Build #1 now has translations**
4. Publish Beta, test - same
5. Intermediate Beta if needed - same
6. ~~Download Translations~~ - not needed, already done at step 2
7. Finalize Release - same (version rename + final build)
8. Test, Submit, Publish - same

**What this saves:** The 1-2 day wait for GlotPress. The release manager no longer needs to check "are translations ready?" or coordinate with the i18n team. Everything else stays the same including the finalize rebuild.

**Changes needed:**
- New Fastlane lane `ai_translate_strings` (calls a Python script that uses the LLM API)
- New Buildkite pipeline `ai-translate-strings.yml`
- Add an "AI Translate" button in the Releases V2 scenario before "Complete Code Freeze"
- Mark "Download Translations" as skippable/optional in the scenario
- LLM API key in Buildkite secrets (already have `ANTHROPIC_API_KEY`, would need `OPENAI_API_KEY` for Codex)

**Trade-offs:** Minimal risk. The rest of the release flow is untouched. If AI translation fails for some reason, we can still fall back to the GlotPress download step.

### Option B: Eliminate the finalize rebuild (medium change)

On top of Option A, drop the RC naming convention and use the final version name from the start. This removes the need for the "Finalize Release" rebuild.

1. Start Code Freeze - create branch, version `X.Y` (no `-rc-1`), bump build code
2. AI Translate Strings - same as Option A
3. Complete Code Freeze - **Build #1** has translations + final version name
4. Publish to internal/beta track, test
5. Bug Fix if needed - only bump build code (`X.Y`, code 737), new build
6. **Promote to Production** - promote the tested build from beta to production track (same binary, no rebuild)
7. Finalize (admin only) - close milestone, remove branch protection, backmerge PR, GitHub release
8. Rollout - 10% → check Sentry → 100%

**What this saves:** On top of Option A, eliminates one guaranteed build per release. The "Finalize Release" step no longer triggers a build - it just does housekeeping. In the best case (no bugs found), the release ships with a single build.

**Changes needed (on top of Option A):**
- `start_code_freeze` sets final version name directly (no `-rc-1` suffix)
- `build_and_upload_google_play` routing decoupled from version name. Currently it checks for `-rc-` in the version to decide beta vs production track. Without RC, it needs an explicit track parameter.
- `new_beta_release` just increments build code (currently uses `RCNotationVersionFormatter` to parse rc-1 → rc-2)
- `finalize_release` split into: `promote_to_production` (Play Store track promotion) + `finalize_release` (admin tasks only, no build)
- Releases V2 scenario updated to reflect the new steps
- Changes to `fastlane-plugin-wpmreleasetoolkit` version handling

**Trade-offs:** More changes across multiple systems (Fastfile, release-toolkit gem, Releases V2 scenario). RC naming is a convention used by other mobile apps too, so this is a WCAndroid-specific change. The version name visible to users would be `X.Y` from the first beta - which is arguably better (users don't see "rc" in their app version).

### Option C: Translate in PR (like Day One Android)

@plokhoves built a [GitHub Actions workflow for Day One Android](https://dayoneandroidp2.wordpress.com/2026/03/26/rfc-moving-string-translations-from-glotpress-to-ai/) that translates strings in-PR. When a PR touches `strings.xml`, it translates the delta and commits the translations in the same PR. This means translations are always up to date in trunk and no separate translation step is needed during release.

**What this saves:** Everything from Option B, plus no dedicated translation step during release at all. Translations are always current.

**Trade-offs:** More CI compute (translation runs on every PR that touches strings). Needs careful handling of PRs that add many strings at once. Requires a different approach to the GlotPress workflow.

## My Recommendation

Start with **Option A** - it's the smallest change, lowest risk, and already removes the biggest pain point (the 1-2 day wait). It can be implemented with a new Fastlane lane and Buildkite pipeline without touching the existing release flow.

If Option A works well for a few releases, move to **Option B** to eliminate the finalize rebuild.

Option C is interesting but it's a bigger shift in how we think about translations - worth exploring separately, possibly in collaboration with @plokhoves and @iangmaia.

## Advantages of AI Translation in General

Regardless of which option we pick, AI translation has some structural advantages over the current GlotPress workflow:

- **Speed**: minutes instead of days. No release blocked on translations.
- **Consistency**: one prompt applies to all 16 languages. Want shorter translations? One line change. Want a glossary of e-commerce terms? Add it once.
- **Cost**: under $10/year for all 16 languages across 26 releases (see [experiment post](https://peacockp2.wordpress.com/2026/03/31/hack-experiment-can-ai-replace-translators/) for details).
- **Quality control**: the prompt can enforce constraints that are hard to communicate to human translators - length limits, domain terminology, tone. And these constraints apply uniformly across all languages.
- **Scalability**: adding a new language is a one-line change (add locale code to the list). No need to find and contract new translators.
- **Iteration**: if a translation is wrong, fix the prompt and re-run. The fix applies to all future translations automatically.

## What's Already Built

The experiment branch ([woocommerce/woocommerce-android#15588](https://github.com/woocommerce/woocommerce-android/pull/15588)) contains:

- `parse_strings.py` - XML parser for Android string resources (handles plurals, format placeholders, HTML entities)
- `translate_fast.py` - translation script with chunking, parallel execution, supports Claude and Codex CLIs
- `compute_metrics.py` - BLEU, chrF, Levenshtein similarity computation
- `blind_judge.py` - blind A/B evaluation using independent LLM judges
- `generate_report.py` - Plotly HTML report generator
- Design spec and implementation plan for release pipeline integration

For Option A, the remaining work is:
- Production translation script (diff-based, translates only new/changed strings)
- Merge script (writes translations back into `values-*/strings.xml`)
- Fastlane lane + Buildkite pipeline
- Releases V2 scenario update
