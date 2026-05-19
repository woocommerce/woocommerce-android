# AI translation engine (GlotPress replacement — Android)

Self-contained, **stdlib-only** Ruby engine that translates Android
`strings.xml` with Claude instead of GlotPress. No Bundler/Nokogiri at
runtime, so it runs on any stock Ruby and is straightforward to extract for
woocommerce-ios later.

## Run it

```bash
# Offline dry-run (deterministic stub, no network/spend) — safe anywhere:
ruby fastlane/ai_translation/bin/woo-ai-translate \
  --offline --locales pl,cs,da \
  --source WooCommerce/src/main/res/values/strings.xml \
  --res-dir WooCommerce/src/main/res \
  --manifest fastlane/ai_translation/translation-manifest.json

# Real run (needs ANTHROPIC_API_KEY, or WOO_AI_TRANSLATION_BASE_URL for the
# Automattic AI gateway):
bundle exec fastlane ai_translate mode:prtime
bundle exec fastlane ai_translate mode:sweep locales:"pl,cs,da" strict:true
```

Modes (`prtime | ondemand | sweep | backfill`) select operational policy
(which locales/branch, real-time vs batch). The per-key delta algorithm is
identical for all of them — that is why the PR-time job is a safe partial of
the code-freeze sweep safety net.

## How it works

1. Parse source `strings.xml` (order-preserving; `<string>`, `<string-array>`,
   `<plurals>`; `translatable="false"` excluded).
2. For each key×locale compute `cache_key = sha(source + context + locale +
   model + prompt_version)`. Unchanged ⇒ reused from the existing localized
   file (no model call). Changed/missing ⇒ queued.
3. Attach per-key context (AINFRA-1707 seam — see `context_provider.rb`).
4. Batch the queued keys into structured JSON in/out calls. Sonnet by default,
   Opus escalation for marketing/store-metadata keys. `temperature: 0`,
   pinned model, prompt-cached system prefix.
5. **Blocking gates**: placeholder parity, XML well-formedness, key parity,
   plural-pair *output* integrity. A key that fails is left untranslated
   (Android falls back to English) and reported — never shipped broken.
6. Write `values-<locale>/strings.xml`; update `translation-manifest.json`.

## Tests

```bash
ruby fastlane/ai_translation/spec/woo_ai_translation_test.rb
```

## Findings / accepted limitations (v1)

- **Manual plural pairs are looser than `_single`+`_multiple`.** The real
  `strings.xml` intentionally has unpaired keys. The hard gate therefore checks
  *output* integrity (never collapse a pair that exists in source), not
  source-side symmetry; source asymmetry is reported, non-blocking.
- **Formatter fidelity vs the toolkit** is intentionally not byte-identical.
  Localized files only need to be valid Android XML with correct escaping; the
  GlotPress baseline import is a *file copy*, not a re-serialization. Drift is
  caught in the rollout shadow-mode diff.
- **Inline child elements** (e.g. `xliff:g`) round-trip untranslated (rare
  here). Real CLDR plurals remain the deferred cross-platform follow-up.

## Blockers (cannot run for real here)

- Funded `ANTHROPIC_API_KEY` / Automattic AI gateway + budget sign-off.
- One-time backfill and the GlotPress baseline export/import (rollout phase).
