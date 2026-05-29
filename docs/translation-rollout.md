# Translation rollout runbook (GlotPress → AI)

Phased, low-risk cutover for **Android**. Each phase is a separate PR (see the
`issue/woomob-translations-p*` branches). The engine lives in
[`fastlane/ai_translation`](../fastlane/ai_translation/README.md).

## Prerequisites / blockers to clear first

- **Funded `ANTHROPIC_API_KEY`** (or Automattic AI gateway via
  `WOO_AI_TRANSLATION_BASE_URL`) + **budget sign-off** for the one-time
  backfill (largest cost) and steady-state PR runs. The same secret already
  used by Claude Build Analysis can be reused on CI.
- **Apps Infra handoff:** retire the WPCOM cron
  `wpcom/bin/i18n/import-github-originals.php` (it imported the frozen
  `strings.xml` into GlotPress). Do not leave it importing a stale file.
- **Confirm Play Console codes** for the 15 new locales (esp. `ms`/`bg`/`uk`)
  and adjust `SUPPORTED_LOCALES[*][:google_play]` in `fastlane/Fastfile`.

## Phase 1 — Engine + validation offline (no spend)

```bash
ruby fastlane/ai_translation/spec/woo_ai_translation_test.rb
bundle exec fastlane ai_translate mode:prtime   # with a key, or:
ruby fastlane/ai_translation/bin/woo-ai-translate --offline --locales pl,th \
  --source WooCommerce/src/main/res/values/strings.xml --res-dir /tmp/x --manifest /tmp/x/m.json
```

Quality sanity check (AI vs current human translations, no repo changes):

```bash
bundle exec fastlane ai_translate_shadow            # writes fastlane/ai_translation/shadow-diff.md
```

## Phase 2 — Baseline (one-time, dedicated PR)

```bash
# No-spend dry run: only seeds the manifest from the committed human files.
bundle exec fastlane ai_translate_backfill mode:import

# Real backfill: import human strings as-is (origin glotpress-import, no
# re-translation) + AI-fill only the gaps + full backfill the 15 new locales.
bundle exec fastlane ai_translate_backfill
```

After this, `translation-manifest.json` is a fully-translated baseline with
per-key origin (`glotpress-import` vs `ai`) — auditable and shadow-comparable.

## Phase 3 — Shadow mode (one release, no user-facing change)

Run `ai_translate_shadow` on the release branch; review `shadow-diff.md` with
native speakers. This is also where writer/formatter drift vs the toolkit is
assessed (the engine targets valid Android XML, not byte-identical output).

## Phase 4 — Cutover

The code-freeze sweep pipeline (`download-release-translations.yml`) already
calls the repurposed `download_release_translations` (AI sweep + metadata).
Ship the 15 new locales (Phase 1 config PR). GlotPress is no longer consulted.

## Phase 5 — Keep PR-time translation required, then retire GlotPress

- The PR-time check (`.buildkite/commands/ai-translate-pr.sh`) is required.
  Use the `skip-ai-translation` label only for intentional operator bypasses;
  fork/no-secret builds still skip before doing any translation work.
- Retire GlotPress + the WPCOM cron + remaining toolkit GlotPress actions.
  Keep the final GlotPress export archived as the baseline seed (the committed
  `values-*/strings.xml` already is that export).

## Rollback

Every phase is an isolated PR/commit. Revert the cutover PR to fall back to
GlotPress (its toolkit gem is still present until the final retirement step).
The manifest's `origin` field makes it clear which strings are human vs AI.

## Known v1 limitations (accepted, tracked separately)

- Manual `_single`/`_multiple` plurals are linguistically incomplete for
  Slavic locales (`pl`, `cs`, `uk`, `bg`) and `ru` — real CLDR `<plurals>` is
  a deferred cross-platform follow-up.
- Screenshots stay English for all locales — localized screenshots (assets +
  overlay text) are a deferred content-ops follow-up.
- Real-time API is used everywhere; the Batch API (≈50% cheaper) for the
  one-time backfill is a follow-up optimization.
- iOS is a parallel, separate workstream (same engine core; not in this repo).
