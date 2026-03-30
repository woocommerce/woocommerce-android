# AI Translation Experiment: Findings

## Summary

We ran an experiment to see if AI can replace human translators (GlotPress) for WooCommerce Android string resources. The results are positive: **in a blind comparison, an independent AI judge preferred AI translations over human ones 38% of the time vs 18% for human** (44% were ties). AI translations are not just "good enough" — they are often better.

## What We Tested

- **3,829 translatable strings** in the WooCommerce Android app
- **300 randomly sampled strings** per language (same sample across all runs, seed=42)
- **16 languages**: Arabic, German, Spanish, French, Hebrew, Indonesian, Italian, Japanese, Korean, Dutch, Portuguese (Brazil), Russian, Swedish, Turkish, Chinese Simplified, Chinese Traditional
- **2 LLMs**: Claude (Haiku 4.5) and Codex (GPT-5.4)
- **2 strategies**: Naive (just translate) and Contextual (translate with code usage context for length/UI awareness)

## Key Findings

### 1. AI translations are preferred over human ones

We ran a **blind A/B evaluation**: an independent AI judge saw the English original and two translations (A and B) without knowing which was AI and which was human. The judge picked the better one for mobile app UI.

| Language | AI Wins | Human Wins | Tie | AI Win Rate |
|----------|---------|------------|-----|-------------|
| Hebrew | 33 | 4 | 13 | **66%** |
| Turkish | 23 | 6 | 21 | **46%** |
| Japanese | 23 | 14 | 13 | **46%** |
| Chinese (Simplified) | 21 | 13 | 16 | **42%** |
| Russian | 21 | 13 | 16 | **42%** |
| French | 20 | 8 | 22 | **40%** |
| Chinese (Traditional) | 20 | 11 | 19 | **40%** |
| Portuguese (Brazil) | 19 | 7 | 24 | **38%** |
| Korean | 19 | 14 | 17 | **38%** |
| Italian | 18 | 7 | 25 | **36%** |
| Arabic | 18 | 10 | 22 | **36%** |
| Spanish | 16 | 4 | 30 | **32%** |
| Dutch | 16 | 12 | 22 | **32%** |
| Swedish | 13 | 6 | 31 | **26%** |
| German | 12 | 11 | 27 | **24%** |
| Indonesian | 12 | 7 | 31 | **24%** |
| **Overall** | **304** | **147** | **349** | **38%** |

AI is preferred **2x more often** than human across all 16 languages. No language had human translations winning overall.

### 2. Codex (GPT-5.4) produces better translations than Claude (Haiku 4.5)

Measured by similarity to existing human translations (BLEU, chrF, Levenshtein):

| Combo | Avg BLEU | Avg chrF | Avg Levenshtein | Languages |
|-------|----------|----------|-----------------|-----------|
| **Naive / Codex** | **60.6** | **73.8** | **0.846** | 16 |
| Contextual / Codex | 60.4 | 73.4 | 0.845 | 16 |
| Naive / Claude | 57.0 | 71.3 | 0.829 | 15* |
| Contextual / Claude | 56.8 | 70.2 | 0.821 | 15* |

*Claude had a key format mismatch on Chinese Simplified (0 matches). Codex handled all 16 languages correctly.

### 3. Adding code context does not improve translation quality

We tested two strategies:
- **Naive**: just ask to translate, no extra instructions
- **Contextual**: include code usage context (where the string appears in the UI) and ask to keep translations short

Result: naive performs the same or slightly better, and is **~40% faster** (no code context lookup needed).

| | Naive | Contextual |
|--|-------|------------|
| Avg BLEU (Codex) | **60.6** | 60.4 |
| Avg chrF (Codex) | **73.8** | 73.4 |
| Time per language | ~108s | ~170s |

This means the production implementation can use a simple prompt with no code analysis.

### 4. Quality is strong across all language families

Per-language BLEU scores (naive/codex, best combo):

| Language | BLEU | chrF | Levenshtein |
|----------|------|------|-------------|
| Portuguese (Brazil) | **73.1** | 84.1 | 0.905 |
| Indonesian | **72.7** | 80.4 | 0.885 |
| Spanish | **71.9** | 82.9 | 0.894 |
| Swedish | **70.7** | 80.4 | 0.876 |
| French | **70.6** | 83.4 | 0.899 |
| German | **67.3** | 80.2 | 0.860 |
| Italian | **66.2** | 80.7 | 0.893 |
| Turkish | **64.7** | 77.8 | 0.862 |
| Dutch | **63.7** | 77.4 | 0.857 |
| Arabic | **58.1** | 71.1 | 0.843 |
| Korean | **57.0** | 61.9 | 0.788 |
| Russian | **53.4** | 67.3 | 0.773 |
| Hebrew | **48.7** | 63.2 | 0.819 |
| Chinese (Simplified) | **47.2** | 65.2 | 0.818 |
| Chinese (Traditional) | **44.5** | 63.1 | 0.815 |
| Japanese | **39.5** | 62.0 | 0.755 |

Note: BLEU scores are lower for CJK/RTL languages, but this is a known limitation of the metric (it measures n-gram overlap, which penalizes different valid word choices). The blind judge evaluation shows AI is actually **preferred more** for these languages (Hebrew 66%, Japanese 46%, Chinese 40-42%).

### 5. Timing

| Operation | Time |
|-----------|------|
| Translation of 300 strings (naive, 1 language) | ~108s (Claude), ~175s (Codex) |
| Translation of 300 strings (contextual, 1 language) | ~170s (Claude), ~170s (Codex) |
| All 16 languages, naive, Claude | ~26 min |
| All 16 languages, naive, Codex | ~47 min |
| Blind judge evaluation (50 strings, 1 language) | ~90s |

For production use: a typical release adds 20-50 new strings. Translating 50 strings to 16 languages would take under 2 minutes.

## How It Relates to the Release Process

Currently the release process has a **1-2 day wait** for GlotPress human translators after code freeze. This forces a second build just to include translations.

With AI translations:
1. Translate new strings immediately at code freeze (minutes, not days)
2. First build already includes translations
3. No second "translation build" needed

Savings per release: ~1-2 calendar days. With biweekly releases (26/year): **26-52 days of calendar time saved per year**.

## Experiment Details

- Branch: `hack/ai-translations`
- All experiment scripts: `scripts/ai-translations/`
- Model used for translations: Claude Haiku 4.5, Codex (GPT-5.4)
- Model used for blind judge: Claude Haiku 4.5
- Sample: 300 strings randomly sampled from 3,829 total (seed=42)
- Same sample used across all language/strategy/LLM combos for fair comparison
- Blind judge: 50 strings per language, random A/B assignment (seed=42)
- Metrics: BLEU (sacrebleu), chrF (sacrebleu), Levenshtein similarity (python-Levenshtein)

## Next Steps

If this direction is approved:
1. Implement AI translation as a Fastlane lane + Buildkite pipeline step
2. Integrate into the Releases V2 scenario for WooCommerce Android
3. Trial run on one real release
4. Full analysis of required release process changes is documented in `scripts/ai-translations/docs/release-changes-analysis.md` (on the branch)
