# Proposal: AI Translations in the WooCommerce Android Release Pipeline

This is a follow-up to [the experiment post](https://peacockp2.wordpress.com/2026/03/31/hack-experiment-can-ai-replace-translators/) where I evaluated AI translation quality against human translations across all 16 supported languages.

There is already active work towards AI translations across teams (see [Related Work](#related-work) - @plokhoves, @iangmaia, @joshheald, @alexgrebenyuk, and others). I think our release pipeline could benefit from these efforts, and during hack week I analyzed what changes would be needed specifically for WooCommerce Android. I won't implement these changes myself since the release tooling is owned by the Apps Infrastructure team, and there are open questions about the overall approach that the team and stakeholders should decide on together. But I wanted to share the analysis in case it's useful as a reference. I tried to make it specific enough to serve as a spec - the [detailed implementation plan](https://github.com/woocommerce/woocommerce-android/blob/hack/ai-translations/docs/superpowers/plans/2026-03-30-ai-translations.md) on the branch has exact file paths and line numbers that can be used directly by a person or an AI agent.

## The Problem

Every WooCommerce Android release requires at least **two builds**, even when there are no bugs to fix. The reason: translations arrive after the first build, and the version name changes from `X.Y-rc-N` to `X.Y` for the final build. Both changes require a new binary.

Here is how the current flow works (version **name** = what users see, e.g. "24.4"; version **code** = integer for Play Store ordering, e.g. 736):

**Milestone: Code Freeze**
- Start Code Freeze - create `release/X.Y` branch, set version name to `X.Y-rc-1`, version code 736
- Complete Code Freeze - freeze strings, merge library strings, trigger **Build #1** (name `X.Y-rc-1`, code 736). This build has **no translations**.

**Milestone: Beta Testing**
- Publish to Play Store beta track, test
- Bug fix betas if needed - bump to `X.Y-rc-2` (code 737), trigger **Build #2**

**Milestone: Play Store Submission**
- Wait for GlotPress translations (translations arrive after the strings are frozen and picked up by the weekly translation process)
- Download translations from GlotPress, commit to release branch
- Finalize Release - change version name from `X.Y-rc-N` to `X.Y`, bump code to 738, trigger **Build #3**. This is a **new binary** just to include translations and change the version name. This build always happens, even with zero bugs.
- Test the new build, submit to Play Store production track

**Milestone: Release + Rollout**
- Publish GitHub release, 10% rollout, Sentry check, 100%

The duplicate build adds work for the release manager every release. For CIAB (the upcoming launch), we had to coordinate with GlotPress to make sure AI-translated strings would be accepted and ready before the deadline - this kind of coordination is friction that slows things down.

## Proposed Flow

The idea: translate strings with AI right at code freeze time (takes about 2 minutes via API), and use the final version name from the start. The first build already has everything it needs and can be [promoted through Play Store release tracks](https://support.google.com/googleplay/android-developer/answer/9844679) (internal → beta → production) without rebuilding.

**Milestone: Code Freeze**
- Start Code Freeze - create `release/X.Y` branch, set version name to `X.Y`, version code 736. No `-rc-N` suffix - the final name from the start.
- **AI Translate Strings** (new step, ~2 minutes) - call the Anthropic API to translate new/changed strings to all 16 languages. `ANTHROPIC_API_KEY` is already available in Buildkite secrets. If we prefer OpenAI/Codex (slightly better quality in the experiment, but close), we'd add an `OPENAI_API_KEY`. The API call is a standard HTTP request from a Python script - nothing special to install on CI agents.
- Complete Code Freeze - freeze strings, merge library strings, trigger **Build #1** (name `X.Y`, code 736). This build **already has translations and the final version name**.

**Milestone: Beta Testing**
- Publish to internal/beta track, test
- Bug fix if needed - keep version name `X.Y`, bump code to 737, trigger **Build #2** (still has translations)

**Milestone: Play Store Submission**
- **Promote to Production** - promote the tested build from beta to production track. This is a standard Google Play operation ([track promotion](https://support.google.com/googleplay/android-developer/answer/9844679)) - same binary, no rebuild.
- Finalize (admin only) - close milestone, remove branch protection, backmerge PR, publish GitHub release. No build triggered.

**Milestone: Release + Rollout**
- 10% rollout, Sentry check, 100%

**Result:** minimum 1 build instead of 2. The release manager doesn't have to wait for translations, doesn't coordinate with GlotPress, and doesn't manage an extra build-test-submit cycle.

## About the `-rc` Version Naming

Today the version name goes through `X.Y-rc-1` → `X.Y-rc-2` → `X.Y`. The `-rc` suffix is what forces the finalize rebuild - you can't ship a version called "24.4-rc-1" to production users, so you need a new build with the name "24.4".

But there's something worth discussing: if a build is published to the beta channel in Google Play and testers are testing it, is it really an "rc" (release candidate)? It's already being tested as a candidate for release. The `-rc` in the version name is an internal convention, not a Google Play requirement. Play Store uses the version code (integer) for ordering and the version name is just a display string.

My proposal is to use the final version name `X.Y` from the start and use version codes to distinguish iterations (build 736, build 737, etc.). This way:
- Users never see "rc" in their app version
- No version rename needed before release
- The same binary can be promoted from beta to production

This does require changes to how the Fastfile handles versioning (see the implementation plan for details). The version code is already incremented for each build, so iteration tracking still works.

## What Needs to Change

The [detailed implementation plan](https://github.com/woocommerce/woocommerce-android/blob/hack/ai-translations/docs/superpowers/plans/2026-03-30-ai-translations.md) on the `hack/ai-translations` branch lists every file and function that needs to change, with line numbers. Here is a summary:

**New:**
- A Python script that diffs `strings.xml` against the previous release tag, translates new/changed strings via the Anthropic (or OpenAI) API, and writes translations back. The release-toolkit gem already has `LocalizeHelper` with XML parsing and merging logic (`android_download_translations` action) - some of this could be reused instead of writing from scratch.
- A Fastlane lane `ai_translate_strings` and a Buildkite pipeline `ai-translate-strings.yml` (same pattern as the existing `download-release-translations.yml`)

**Modified in `fastlane/Fastfile`:**
- `start_code_freeze` (line 132) - set version name to `X.Y` instead of `X.Y-rc-1`
- `build_and_upload_google_play` (line 648) - decouple track routing from version name. Currently checks for `-rc-` to decide beta vs production track. Needs an explicit `track` parameter instead.
- `new_beta_release` (line 316) - just increment version code, not version name
- `finalize_release` (line 519) - split into track promotion + admin tasks, remove build trigger

**Modified in `fastlane-plugin-wpmreleasetoolkit`:**
- `RCNotationVersionFormatter`, `beta_version?`, `beta_version_next` - need to support non-RC versioning. This should be additive (support both RC and non-RC products) since other apps use the same gem.

**Modified in Releases V2 scenario:**
- Add "AI Translate Strings" button to Code Freeze milestone
- Replace "Download Translations" + "Finalize Release" with "Promote to Production" + "Finalize (admin)"

## Advantages of AI Translation

- **No waiting**: translations ready in ~2 minutes via API call, not days
- **No extra build**: first build ships with translations, promote through Play Store tracks
- **Consistency**: one prompt applies to all 16 languages. Length constraints, glossary, tone - one change, all languages.
- **Cost**: under $10/year for all translations across 26 releases
- **Scalability**: adding a new language is a config change, no need to find translators
- **Iteration**: wrong translation? Fix the prompt. The fix applies to everything going forward.

## What's Already Built

The experiment branch ([woocommerce/woocommerce-android#15588](https://github.com/woocommerce/woocommerce-android/pull/15588)) has:

- `parse_strings.py` - Android `strings.xml` parser that can be reused or replaced by the existing `LocalizeHelper` from the release-toolkit gem
- `translate_fast.py` - translation script with chunking and parallel execution
- [Detailed implementation plan](https://github.com/woocommerce/woocommerce-android/blob/hack/ai-translations/docs/superpowers/plans/2026-03-30-ai-translations.md) with line-by-line analysis of all changes needed

## Related Work

- **@plokhoves** - [RFC: Moving String Translations from GlotPress to AI](https://dayoneandroidp2.wordpress.com/2026/03/26/rfc-moving-string-translations-from-glotpress-to-ai/) - built a GitHub Actions workflow for Day One Android that translates string deltas in-PR using Claude API in 30-60 seconds. Includes format specifier validation and a second AI review pass.
- **@iangmaia** - [RFC: AI-Powered Translation Context Extraction Pipeline](https://appsinfrap2.wordpress.com/2026/01/30/rfc-ai-powered-translation-context-extraction-pipeline/) - a Ruby gem/CLI tool ([i18n-context-generator](https://github.com/Automattic/i18n-context-generator)) that enriches string comments with code context using AI. Already tested on the WCAndroid repo. Complements AI translation by improving context for both human and AI translators.
- **@joshheald** and the AI enablement cohort 2 (NYC) - exploring AI translations across Beeper/Mesh with LLM-as-Judge evaluation in langfuse, comparing different models.
- **@alexgrebenyuk** - [Proposal: Continuous Translation](https://appsinfrap2.wordpress.com/2026/03/10/proposal-continuous-translation) - translating in trunk continuously instead of during release. The [discussion thread](https://appsinfrap2.wordpress.com/2026/03/10/proposal-continuous-translation/#comment-2962) covers GlotPress limitations and the potential to migrate to Crowdin.
- **@sorinnunca** - shared GlotPress data: AI translation suggestions (GPT-3.5/GPT-4o-mini) were accepted without edits by translators 46-58% of the time. For non-Mag-16 locales translated with OpenAI on wpcom, 4 out of 6 were launchable but 2 needed significant manual work.
- **@olivierhalligon** - detailed analysis of GlotPress limitations for mobile (no branch concept, WeeklyKit process constraints) and the potential to migrate to Crowdin which supports custom AI translation workflows.
