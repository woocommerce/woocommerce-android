# Can AI Replace Human Translators for WooCommerce Android?

## Goal

I wanted to find out if AI-generated translations are good enough to replace human translators for WooCommerce Android app strings. The app currently has ~3,800 translatable strings across 16 languages, all managed through GlotPress with human translators.

## Experiment Setup

**What I translated:** 300 randomly sampled strings from the app's `strings.xml` (out of 3,829 total). The same sample was used across all runs for fair comparison.

**Languages:** All 16 supported: Arabic, Chinese (Simplified and Traditional), Dutch, French, German, Hebrew, Indonesian, Italian, Japanese, Korean, Portuguese (Brazil), Russian, Spanish, Swedish, Turkish.

**LLMs tested:**
- **Claude** (Haiku 4.5) via Claude Code CLI
- **Codex** (GPT-5.4) via Codex CLI

**Translation strategies:**
- **Naive** -- just ask to translate with no extra instructions beyond preserving placeholders and HTML
- **Contextual** -- same as naive but also grep the codebase for each string's usage (button label? dialog title? error message?) and ask the LLM to keep translations short for mobile UI

**How the experiment ran:** A Python script extracted strings from `strings.xml`, split them into chunks, sent each chunk to the LLM CLI, and collected the JSON responses. All runs used the CLI tools locally (not the API). API calls would be faster since they skip CLI startup overhead.

## How I Measured Quality

### Similarity metrics (AI translation vs existing human translation)

These metrics measure how close the AI output is to the existing human translation. They do not tell us which is better -- only how similar they are.

- **BLEU** (0-100): counts how many word sequences (1-4 words) match between the two translations. Higher means more overlap. Standard metric in machine translation research. Limitation: penalizes valid alternative word choices, especially in CJK languages where word segmentation differs.

- **chrF** (0-100): similar idea but works at the character level instead of words. More reliable for languages with rich morphology (German, Russian, Turkish) and for CJK scripts.

- **Levenshtein similarity** (0-1): measures how many character edits (insert, delete, replace) are needed to turn one string into the other, normalized by length. 1.0 means identical, 0.0 means completely different.

### Blind A/B judge (which translation is actually better?)

Similarity metrics assume human translations are the gold standard. But human translators may not have seen the UI, may have translated too verbosely, or may have picked less natural phrasing. So I also ran a **blind comparison**:

- Take 50 strings per language
- Show the judge the English original + two translations labeled "A" and "B"
- Randomly assign which is AI and which is human (the judge does not know)
- Ask: which is better for a mobile app UI? Consider accuracy, naturalness, and length
- The judge was Claude Haiku 4.5

## Results

### AI translations are preferred over human ones

In the blind A/B evaluation, **AI was preferred 2x more often than human** across all 16 languages. Not a single language had human translations winning overall.

| Language | AI Preferred | Human Preferred | Tie | AI Win Rate |
|----------|-------------|----------------|-----|-------------|
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
| | | | | |
| **Total (800 judged)** | **304 (38%)** | **147 (18%)** | **349 (44%)** | |

### Codex produces better translations than Claude

When measured by similarity to existing human translations:

| LLM + Strategy | Avg BLEU | Avg chrF | Avg Levenshtein |
|----------------|----------|----------|-----------------|
| **Codex, naive** | **60.6** | **73.8** | **0.846** |
| Codex, contextual | 60.4 | 73.4 | 0.845 |
| Claude, naive | 56.4 | 70.8 | 0.828 |
| Claude, contextual | 56.3 | 69.8 | 0.821 |

All 16 languages across all 4 combos. Claude needed smaller chunks for Chinese Simplified (50 strings per chunk instead of 300) to avoid output truncation.

### Adding code context does not help

I expected that giving the LLM information about where each string appears in the code (button, title, error message) would improve translations. It did not. Naive and contextual strategies scored almost identically, but contextual was ~40% slower because of the codebase grep.

| | Naive | Contextual |
|--|-------|------------|
| Avg BLEU (Codex) | **60.6** | 60.4 |
| Avg chrF (Codex) | **73.8** | 73.4 |
| Time per language (Claude CLI) | ~108s | ~170s |

### Per-language quality (best combo: naive/Codex)

| Language | BLEU | chrF | Levenshtein Sim | Blind Judge AI Win Rate |
|----------|------|------|-----------------|------------------------|
| Portuguese (Brazil) | 73.1 | 84.1 | 0.905 | 38% |
| Indonesian | 72.7 | 80.4 | 0.885 | 24% |
| Spanish | 71.9 | 82.9 | 0.894 | 32% |
| Swedish | 70.7 | 80.4 | 0.876 | 26% |
| French | 70.6 | 83.4 | 0.899 | 40% |
| German | 67.3 | 80.2 | 0.860 | 24% |
| Italian | 66.2 | 80.7 | 0.893 | 36% |
| Turkish | 64.7 | 77.8 | 0.862 | 46% |
| Dutch | 63.7 | 77.4 | 0.857 | 32% |
| Arabic | 58.1 | 71.1 | 0.843 | 36% |
| Korean | 57.0 | 61.9 | 0.788 | 38% |
| Russian | 53.4 | 67.3 | 0.773 | 42% |
| Hebrew | 48.7 | 63.2 | 0.819 | 66% |
| Chinese (Simplified) | 47.2 | 65.2 | 0.818 | 42% |
| Chinese (Traditional) | 44.5 | 63.1 | 0.815 | 40% |
| Japanese | 39.5 | 62.0 | 0.755 | 46% |

BLEU scores are lower for CJK and RTL languages. This is a known limitation of the metric -- it penalizes valid alternative word choices, and word segmentation differs. The blind judge results tell a different story: AI is preferred **even more** for these languages (Hebrew 66%, Japanese 46%, Chinese 40-42%). The likely reason is that human translators produced more literal or verbose translations, while the AI picked more natural phrasing.

### Timing

All times below are using local CLI tools. API calls would be faster (no CLI process startup overhead per call).

| What | Time |
|------|------|
| 300 strings, 1 language, naive, Claude CLI | ~108s |
| 300 strings, 1 language, naive, Codex CLI | ~175s |
| 300 strings, 1 language, contextual, Claude CLI | ~170s |
| All 16 languages, naive, Claude CLI | ~26 min |
| All 16 languages, naive, Codex CLI | ~47 min |
| Blind judge, 50 strings, 1 language | ~90s |

A typical release adds 20-50 new strings. Translating 50 strings to all 16 languages via API would take under 2 minutes.

## Experiment Details

- **Branch:** `hack/ai-translations` in `woocommerce/woocommerce-android`
- **Scripts:** `scripts/ai-translations/` (parser, translation scripts, metrics, judge, report generator)
- **Sample:** 300 strings randomly selected from 3,829 translatable strings (deterministic seed=42)
- **Blind judge:** 50 strings per language, random A/B assignment (seed=42), judged by Claude Haiku 4.5
- **Metrics libraries:** sacrebleu (BLEU, chrF), python-Levenshtein (similarity)
- **Total experiment runs:** 64 (2 strategies x 2 LLMs x 16 languages)
- **Total blind judge evaluations:** 800 (50 strings x 16 languages)
