# frozen_string_literal: true

require 'json'

module WooAiTranslation
  # Builds cached prompts, batches keys into structured JSON in/out calls, and
  # parses the response. On a parse/coverage failure it splits the batch and
  # retries; a key that still fails is returned untranslated so the engine can
  # report it rather than ship garbage.
  class Translator
    DEFAULT_BATCH = 40

    SYSTEM_RULES = <<~RULES
      You are a professional software localizer for the WooCommerce Android app.
      Translate UI strings from English into the requested locale.

      Hard rules:
      - Preserve every placeholder EXACTLY: %s, %d, %1$s, %2$d, %% etc. Keep the
        same count and the same positional indexes. Never translate or reorder
        the placeholder tokens themselves.
      - Preserve any inline markup/HTML tags (<b>, <a href="...">, etc.) and
        their attributes unchanged; translate only the human-readable text.
      - Keep escaping sequences (\\n, \\t) intact.
      - Do NOT merge or "fix" singular/plural variants. Each item is translated
        independently and literally for its grammatical number as given.
      - Keep brand names (WooCommerce, Woo, WordPress.com, Jetpack) untranslated.
      - Match the tone of concise mobile UI copy: prefer the shortest natural
        phrasing that fits a phone screen. Do not add notes, quotes, or trailing
        punctuation that the source does not have.
      - If an item carries a non-empty "context" field, treat it as ground truth
        about where/how the string is used and translate accordingly.

      Respond with ONLY a single minified JSON object mapping each input "id" to
      its translated string. No prose, no code fences.
    RULES

    def initialize(client:, glossary: '', batch_size: DEFAULT_BATCH, logger: nil)
      @client = client
      @glossary = glossary.to_s
      @batch_size = batch_size
      @logger = logger
    end

    # items: [{ id:, source:, context: }] ; returns { id => translation }.
    def translate(locale:, items:, model:, style: nil)
      result = {}
      items.each_slice(@batch_size) do |slice|
        result.merge!(translate_slice(locale: locale, items: slice, model: model, style: style))
      end
      result
    end

    private

    def translate_slice(locale:, items:, model:, style:)
      raw = @client.complete(
        model: model,
        system_blocks: system_blocks(locale, style),
        user_content: user_content(locale, items)
      )
      parsed = parse(raw)
      # Conservative typography normalization (ASCII … and –, range dashes)
      # so Android Lint doesn't flag the output. Placeholders are preserved by
      # design; see TextNormalizer.
      parsed = parsed.transform_values { |v| TextNormalizer.normalize(v, locale: locale) }
      covered = items.select { |i| parsed.key?(i[:id]) }.size

      return parsed if covered == items.size
      return split_retry(locale, items, model, style) if items.size > 1

      log("unparseable translation for #{items.first[:id]} (#{locale}); left untranslated")
      {}
    rescue JSON::ParserError, AnthropicClient::Error, ClaudeCliClient::Error => e
      # Persistent client failures (CLI / API) and parser failures must not
      # crash the whole run -- "required from day one" means a single
      # transient or single-key issue cannot block every PR. Split-retry on
      # larger slices; at size 1, leave the key untranslated (engine falls
      # back to default at runtime) and let the engine report it in the
      # spot-check / failed list. Truly unexpected exception types
      # (NoMethodError, SystemCallError, ...) are NOT in this rescue list
      # and still propagate -- those indicate real engine bugs.
      log("recoverable error in slice (#{items.size} items, #{locale}, #{e.class}): #{e.message[0, 200]}")
      items.size > 1 ? split_retry(locale, items, model, style) : {}
    end

    def split_retry(locale, items, model, style)
      mid = items.size / 2
      translate_slice(locale: locale, items: items[0...mid], model: model, style: style)
        .merge(translate_slice(locale: locale, items: items[mid..], model: model, style: style))
    end

    def system_blocks(locale, style)
      # Constant rules + (optional) brand/domain glossary + per-locale style
      # guide (with CLDR plural categories appended when known). The client
      # cache-flags the last block; Anthropic prompt-caching treats that marker
      # as a breakpoint covering the entire prefix, so the whole constant
      # prologue is cached across every batched call.
      blocks = [SYSTEM_RULES]
      blocks << @glossary unless @glossary.empty?

      style_block = "Target locale: #{locale}.\n#{style.to_s.empty? ? default_style(locale) : style}"
      cldr_line = CldrPlurals.prompt_line_for(locale)
      style_block += "\n\n#{cldr_line}" unless cldr_line.empty?

      blocks << style_block
      blocks
    end

    def default_style(locale)
      "Use natural, idiomatic #{locale} as used in modern mobile commerce apps. " \
        'Prefer concise phrasing that fits small screens.'
    end

    def user_content(locale, items)
      payload = items.map { |i| { id: i[:id], source: i[:source], context: i[:context].to_s } }
      "locale: #{locale}\nTranslate every item; respond with the JSON object only.\n" \
        "#{JSON.generate(payload)}"
    end

    def parse(raw)
      text = raw.to_s.strip
      text = text.gsub(/\A```(?:json)?/, '').gsub(/```\z/, '').strip
      obj = JSON.parse(text)
      raise JSON::ParserError, 'expected object' unless obj.is_a?(Hash)

      obj
    end

    def log(msg)
      @logger&.call(msg)
    end
  end
end
