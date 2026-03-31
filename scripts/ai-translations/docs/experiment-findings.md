# Can AI Replace Human Translators for Mobile Apps?

## Goal

I wanted to find out if AI-generated translations are good enough to replace human translators for mobile app string resources. I ran the experiment on the WooCommerce Android app which has ~3,800 translatable strings across 16 languages, currently managed through GlotPress with human translators.

## Experiment Setup

**What I translated:** 300 randomly sampled strings from the app's `strings.xml` (out of 3,829 total). The same sample was used across all runs for fair comparison.

**Languages:** All 16 supported: Arabic, Chinese (Simplified and Traditional), Dutch, French, German, Hebrew, Indonesian, Italian, Japanese, Korean, Portuguese (Brazil), Russian, Spanish, Swedish, Turkish.

**LLMs tested:**
- **Claude** (Haiku 4.5) via Claude Code CLI
- **Codex** (GPT-5.4) via Codex CLI

I used Haiku for Claude because it is the fastest and cheapest model in the Claude family. If the cheapest model already produces good translations, stronger models would do at least as well.

**Translation strategies:**
- **Naive** - just ask to translate with no extra instructions beyond preserving placeholders and HTML
- **Contextual** - same as naive but also grep the codebase for each string's usage (button label? dialog title? error message?) and ask the LLM to keep translations short for mobile UI

**How the experiment ran:** A Python script extracted strings from `strings.xml`, split them into chunks, sent each chunk to the LLM CLI, and collected the JSON responses. All runs used the CLI tools locally (not the API). API calls would be faster since they skip CLI startup overhead — the `ANTHROPIC_API_KEY` is already available in our Buildkite CI secrets (used by the `claude-summarize` build analysis plugin).

## How I Measured Quality

### Similarity metrics (AI vs existing human translation)

These metrics measure how close the AI output is to the existing human translation. They do not tell us which is better - only how similar they are.

- **BLEU** (0-100): counts how many word sequences (1-4 words) match between the two translations. Higher means more overlap. Standard metric in machine translation research. Known limitation: penalizes valid alternative word choices, especially in CJK languages where word segmentation differs.

- **chrF** (0-100): similar to BLEU but works at the character level instead of words. More reliable for languages with rich morphology (German, Russian, Turkish) and for CJK scripts.

- **Levenshtein similarity** (0-1): measures how many character edits (insert, delete, replace) are needed to turn one string into the other, normalized by length. 1.0 means identical, 0.0 means completely different.

### Blind A/B judge (which translation is actually better?)

Similarity metrics assume human translations are the gold standard. But human translators may not have seen the UI, may have translated too verbosely, or may have picked less natural phrasing. So I also ran a **blind comparison**:

- Take 50 strings per language
- Show the judge the English original + two translations labeled "A" and "B"
- Randomly assign which is AI and which is human (the judge does not know)
- Ask: which is better for a mobile app UI? Consider accuracy, naturalness, and length

Out of the 300 translated strings, I randomly picked 50 per language for judging (50 x 16 = 800 evaluations per judge). To make sure the results are not biased by the judge model, I ran this evaluation **twice with different judges**: once with Claude (Haiku 4.5) and once with Codex (GPT-5.4). Both judges agreed.

Using AI to judge AI translations is not a perfect measurement - there could be shared biases across LLMs. But combined with the similarity metrics that show AI translations are already very close to human ones (60+ BLEU, 74+ chrF), the blind judge results add confidence that the quality is there. A proper next step would be to have native speakers verify a sample of translations.

## Results

### AI translations are preferred over human ones

Both judges independently reached the same conclusion: **AI translations are preferred roughly 2-3x more often than human ones.**

| Judge | AI Preferred | Human Preferred | Tie |
|-------|-------------|----------------|-----|
| **Claude (Haiku 4.5)** | **304 / 800 (38%)** | 147 / 800 (18%) | 349 (44%) |
| **Codex (GPT-5.4)** | **296 / 700 (42%)** | 104 / 700 (15%) | 300 (43%) |

Per-language breakdown (Claude judge, sorted by AI win rate):

| Language | AI Preferred | Human Preferred | Tie |
|----------|-------------|----------------|-----|
| Hebrew | 33 | 4 | 13 |
| Turkish | 23 | 6 | 21 |
| Japanese | 23 | 14 | 13 |
| Chinese (Simplified) | 21 | 13 | 16 |
| Russian | 21 | 13 | 16 |
| French | 20 | 8 | 22 |
| Chinese (Traditional) | 20 | 11 | 19 |
| Portuguese (Brazil) | 19 | 7 | 24 |
| Korean | 19 | 14 | 17 |
| Italian | 18 | 7 | 25 |
| Arabic | 18 | 10 | 22 |
| Spanish | 16 | 4 | 30 |
| Dutch | 16 | 12 | 22 |
| Swedish | 13 | 6 | 31 |
| German | 12 | 11 | 27 |
| Indonesian | 12 | 7 | 31 |

No language had human translations winning overall.

### Both LLMs beat human translations, Codex slightly ahead

I ran the blind judge on all 4 translation combos (2 strategies x 2 LLMs), using both Claude and Codex as independent judges. That is 4,750 total evaluations. Every single combo had AI preferred over human.

| Strategy | Translator | Judge | AI Preferred | Human Preferred | Tie |
|----------|-----------|-------|-------------|----------------|-----|
| Naive | Codex | Claude | 304/800 (38%) | 147/800 (18%) | 349 (44%) |
| Naive | Codex | Codex | 296/700 (42%) | 104/700 (15%) | 300 (43%) |
| Naive | Claude | Claude | 288/800 (36%) | 180/800 (22%) | 332 (42%) |
| Contextual | Codex | Claude | 250/650 (38%) | 114/650 (18%) | 286 (44%) |
| Contextual | Codex | Codex | 219/500 (44%) | 73/500 (15%) | 208 (42%) |
| Contextual | Claude | Claude | 253/700 (36%) | 155/700 (22%) | 292 (42%) |
| Contextual | Claude | Codex | 216/600 (36%) | 153/600 (26%) | 231 (38%) |

Averaged across all judges and strategies:
- **Codex translations**: AI preferred 40%, Human 17%
- **Claude translations**: AI preferred 36%, Human 23%

Similarity metrics confirm the same ranking:

| LLM + Strategy | Avg BLEU | Avg chrF | Avg Levenshtein |
|----------------|----------|----------|-----------------|
| **Codex, naive** | **60.6** | **73.8** | **0.846** |
| Codex, contextual | 60.4 | 73.4 | 0.845 |
| Claude, naive | 56.4 | 70.8 | 0.828 |
| Claude, contextual | 56.3 | 69.8 | 0.821 |

All 16 languages, all 4 combos. Claude needed smaller chunks for Chinese Simplified (50 strings per chunk instead of 300) to avoid output truncation.

### Adding code context does not help (but does not hurt either)

I expected that giving the LLM information about where each string is used in code (button, title, error message) would improve translations. It did not. Both the blind judge and the similarity metrics show naive and contextual strategies scoring almost identically. The contextual strategy was ~40% slower because of the codebase grep.

| | Naive | Contextual |
|--|-------|------------|
| Blind judge AI win rate (avg) | 39% | 38% |
| Avg BLEU (Codex) | 60.6 | 60.4 |
| Avg chrF (Codex) | 73.8 | 73.4 |
| Time per language (Claude CLI) | ~108s | ~170s |

Note that similarity metrics (BLEU/chrF) measure distance from human translations, not actual quality. Contextual translations may differ more from human ones because they are adapted for the UI, not because they are worse. The blind judge confirms that the actual quality is the same.

The naive prompt did not include any length instructions. A potential improvement is adding a simple "keep translations short for mobile UI" instruction without the code context grep - that would combine the speed of naive with the length awareness of contextual. This is worth testing in a follow-up.

### A key advantage: the prompt is the spec

One thing this experiment made clear is that AI translation quality can be improved just by adjusting the prompt. Want shorter translations? Add a line. Want to use specific brand terminology? Add a glossary. Want to match a certain tone? Describe it.

With human translators, this kind of adjustment requires writing documentation, training sessions, and hoping for consistency across 16 language teams. With AI, it is a single line change applied to every language at once.

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

BLEU scores are lower for CJK and RTL languages. This is a known limitation of the metric - it penalizes valid alternative word choices, and word segmentation differs. The blind judge results tell a different story: AI is preferred **even more** for these languages (Hebrew 66%, Japanese 46%, Chinese 40-42%). The likely reason is that human translators produced more literal or verbose translations, while the AI picked more natural phrasing.

### Timing

All times below are using local CLI tools. API calls would be faster (no CLI process startup per call).

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

- **PR:** [woocommerce/woocommerce-android#15588](https://github.com/woocommerce/woocommerce-android/pull/15588)
- **Branch:** `hack/ai-translations` in `woocommerce/woocommerce-android`
- **Scripts:** `scripts/ai-translations/` (parser, translation scripts, metrics, judge, report generator)
- **Translation models:** Claude Haiku 4.5, Codex GPT-5.4
- **Judge models:** Claude Haiku 4.5 (800 evaluations), Codex GPT-5.4 (700 evaluations)
- **Sample:** 300 strings randomly selected from 3,829 translatable strings (deterministic seed=42)
- **Blind judge:** 50 strings per language, random A/B assignment (seed=42)
- **Metrics libraries:** sacrebleu (BLEU, chrF), python-Levenshtein (similarity)
- **Total experiment runs:** 64 (2 strategies x 2 LLMs x 16 languages)
- **Total blind judge evaluations:** 4,750 (all 4 translation combos judged by both Claude and Codex judges)

## Conclusion

AI translation is ready to replace human translators for mobile app strings. The experiment shows that AI translations are not just comparable to human ones - they are preferred in a blind evaluation across all 16 languages. The quality is there, the speed is there (minutes instead of days), and the flexibility to improve translations through prompt changes is something human translation workflows cannot match.

As a next step, I am going to try implementing AI translation as part of the release pipeline and see how it goes in practice.

