# frozen_string_literal: true

require 'json'

module WooAiTranslation
  # Per-key code-derived context (screen/feature, surrounding usage, developer
  # notes) is the main translation-quality lever. This is the integration seam
  # for the shared AINFRA-1707 strings context-extraction pipeline.
  #
  # Three input shapes, all optional:
  # - strings_context.json: { "<string-name>": "context string", ... }
  # - glossary.json:        { "terms": [{ "term": "...", "rule": "..." }, ...] }
  # - style/<locale>.md:    per-locale style notes
  #
  # The Peacock P2 experiment showed that broad codebase grepping did not help
  # quality; targeted, dev-authored signals (per-string XML comments handled in
  # the parser, plus a short glossary and per-locale style guide) are the
  # cost-effective levers and slot in here as cache-flagged prompt prefixes.
  class ContextProvider
    def self.from_file(path, glossary_path: nil, style_dir: nil)
      data = path && File.exist?(path) ? safe_json(path) : {}
      gloss = glossary_path && File.exist?(glossary_path) ? safe_json(glossary_path) : nil
      new(data, glossary: gloss, style_dir: style_dir)
    end

    def self.safe_json(path)
      JSON.parse(File.read(path))
    rescue JSON::ParserError
      {}
    end

    def initialize(map = {}, glossary: nil, style_dir: nil)
      @map = map || {}
      @glossary = glossary
      @style_dir = style_dir
    end

    def context_for(name)
      @map[name].to_s
    end

    def any?
      !@map.empty?
    end

    # Stable, prompt-cache-friendly text rendering of the glossary. Empty string
    # when no glossary file is configured (no token cost).
    def glossary_text
      return '' unless @glossary.is_a?(Hash) && @glossary['terms'].is_a?(Array)

      lines = @glossary['terms'].map do |t|
        "- #{t['term']}: #{t['rule']}"
      end
      return '' if lines.empty?

      "Brand & domain glossary (applies to every locale):\n#{lines.join("\n")}"
    end

    # Per-locale style guide -- returns the contents of style/<locale>.md or ''.
    def style_for(locale)
      return '' if @style_dir.to_s.empty?

      path = File.join(@style_dir, "#{locale}.md")
      File.exist?(path) ? File.read(path).strip : ''
    end
  end
end
