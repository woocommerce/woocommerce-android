# frozen_string_literal: true

module WooAiTranslation
  # Conservative post-LLM typography clean-up. The model occasionally emits the
  # ASCII three-dot ellipsis (`...`) or hyphen-minus (`-`) in places where
  # Android Lint expects the Unicode forms (U+2026 horizontal ellipsis `…` and
  # U+2013 en dash `–`). These warnings don't affect runtime behavior, but they
  # accumulate quickly across thousands of strings and pollute the PR review.
  #
  # The rules below are deliberately narrow:
  # - `...` → `…` only when the run is the literal three-dot sequence, NOT when
  #   it sits adjacent to another dot (`....`), and NOT inside placeholders.
  # - `-` → `–` only between two digits (a numeric range like `8-20%`) or
  #   between two words with a single space on each side (e.g. `12:00 - 13:00`).
  #   Compound words such as `plug-in` or `step-by-step` are NEVER rewritten.
  # - Placeholder spans (`%s`, `%d`, `%1$s`, `%%`) and escape sequences (`\n`,
  #   `\t`) are preserved verbatim, because Validators.placeholder_parity must
  #   continue to see them in the same shape as the source.
  #
  # Future extension point: locale-aware punctuation (Polish typographic quotes
  # `„…"`, French `« … »`, …) belongs here as additional rules keyed on
  # `locale`. Out of scope for the introduction commit.
  module TextNormalizer
    # Placeholder/escape spans to leave untouched. The regex is greedy on
    # purpose to match Validators::PLACEHOLDER.
    PROTECTED = /%(?:\d+\$)?[-#+ 0,(]?\d*(?:\.\d+)?[a-zA-Z]|%%|\\[ntr"']/

    module_function

    # Returns a normalized copy of `text`. Currently locale-independent; the
    # `locale:` arg is forwarded for forward-compatibility (see file comment).
    def normalize(text, locale: nil) # rubocop:disable Lint/UnusedMethodArgument
      return text if text.nil? || text.empty?

      # Tokenise around protected spans so we never touch placeholders.
      tokens = split_protecting(text.to_s)
      tokens.map.with_index do |tok, i|
        i.odd? ? tok : apply_rules(tok)
      end.join
    end

    # Split into alternating [free, protected, free, protected, ...] tokens.
    def split_protecting(text)
      out = []
      last = 0
      text.scan(PROTECTED) do
        m = Regexp.last_match
        out << text[last...m.begin(0)]
        out << m[0]
        last = m.end(0)
      end
      out << text[last..]
      out
    end

    def apply_rules(text)
      ellipsised = text.gsub(/(?<!\.)\.{3}(?!\.)/, "…")
      ranges    = ellipsised.gsub(/(\d)-(\d)/, "\\1–\\2")
      ranges.gsub(/(\w) - (\w)/) do
        # Only rewrite when both sides are clearly numeric-or-time fragments;
        # leave " - " in free prose alone.
        prev_word = Regexp.last_match(1)
        next_word = Regexp.last_match(2)
        if numeric_or_time?(prev_word) && numeric_or_time?(next_word)
          "#{prev_word}–#{next_word}"
        else
          "#{prev_word} - #{next_word}"
        end
      end
    end

    def numeric_or_time?(token)
      token.match?(/\A\d/)
    end
  end
end
