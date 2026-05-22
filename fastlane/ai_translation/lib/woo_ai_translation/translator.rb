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

    # Raw-text variant for single long-form fields (Play Store metadata:
    # full_description, release_notes, title, short_description). No JSON
    # envelope -- multi-paragraph marketing copy routinely makes the model emit
    # unescaped quotes/newlines that break JSON parsing. Plain text in, plain
    # text out, nothing to mis-escape.
    RAW_SYSTEM_RULES = <<~RULES
      You are a professional software localizer for the WooCommerce Android app.
      Translate the given app-store listing text from English into the requested locale.

      Hard rules:
      - Preserve any placeholders exactly: %s, %d, %1$s, %% etc.
      - Preserve the paragraph / line-break structure of the source.
      - Keep brand names (WooCommerce, Woo, WordPress.com, Jetpack) untranslated.
      - Keep any URLs unchanged.
      - Use natural, concise marketing tone appropriate for an app-store listing.
      - Respond with ONLY the translated text. No quotes around it, no JSON, no
        code fences, no commentary, no preamble.
    RULES

    def initialize(client:, glossary: '', batch_size: DEFAULT_BATCH, logger: nil)
      @client = client
      @glossary = glossary.to_s
      @batch_size = batch_size
      @logger = logger
    end

    # items: [{ id:, source:, context: }] ; returns { id => translation }.
    # raw_text: true bypasses the JSON envelope (one model call per item) for
    # robust long-form metadata translation.
    def translate(locale:, items:, model:, style: nil, raw_text: false)
      return translate_raw(locale: locale, items: items, model: model, style: style) if raw_text

      result = {}
      items.each_slice(@batch_size) do |slice|
        result.merge!(translate_slice(locale: locale, items: slice, model: model, style: style))
      end
      result
    end

    private

    def translate_raw(locale:, items:, model:, style:)
      result = {}
      items.each do |item|
        raw = @client.complete(
          model: model,
          system_blocks: raw_system_blocks(locale, style),
          user_content: "Locale: #{locale}. Translate the following text into #{locale}. " \
                        "Output ONLY the translation as plain text.\n\n#{item[:source]}"
        )
        text = strip_fences(raw)
        result[item[:id]] = TextNormalizer.normalize(text, locale: locale)
      rescue AnthropicClient::Error, ClaudeCliClient::Error => e
        log("raw translate failed #{item[:id]} [#{locale}] (#{e.class}): #{e.message[0, 150]}")
      end
      result
    end

    def raw_system_blocks(locale, style)
      blocks = [RAW_SYSTEM_RULES]
      blocks << @glossary unless @glossary.empty?
      style_block = "Target locale: #{locale}.\n#{style.to_s.empty? ? default_style(locale) : style}"
      blocks << style_block
      blocks
    end

    def strip_fences(raw)
      raw.to_s.strip.gsub(/\A```[\w-]*\n?/, '').gsub(/\n?```\z/, '').strip
    end

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
      obj = begin
        JSON.parse(text)
      rescue JSON::ParserError
        # Models routinely emit RAW newlines/tabs inside JSON string values for
        # long-form, multi-paragraph content (full_description, release_notes),
        # which is invalid JSON. Repair by escaping control chars that sit
        # inside string literals, then parse again.
        JSON.parse(escape_unescaped_control_chars(text))
      end
      raise JSON::ParserError, 'expected object' unless obj.is_a?(Hash)
      raise JSON::ParserError, 'expected string values' unless obj.values.all? { |v| v.is_a?(String) }

      obj
    end

    # Walk the text tracking whether we're inside a JSON string literal, and
    # escape any raw \n, \r, \t found there. Characters outside strings (and
    # already-escaped sequences) are left untouched.
    def escape_unescaped_control_chars(text)
      out = +''
      in_string = false
      escaped = false
      text.each_char do |ch|
        if in_string && !escaped && (repl = { "\n" => '\\n', "\r" => '\\r', "\t" => '\\t' }[ch])
          out << repl
          next
        end
        if escaped
          escaped = false
        elsif ch == '\\'
          escaped = true
        elsif ch == '"'
          in_string = !in_string
        end
        out << ch
      end
      out
    end

    def log(msg)
      @logger&.call(msg)
    end
  end
end
