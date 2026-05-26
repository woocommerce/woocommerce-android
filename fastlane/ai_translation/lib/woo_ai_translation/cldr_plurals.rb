# frozen_string_literal: true

require 'json'

module WooAiTranslation
  # Required CLDR plural categories per Android locale qualifier.
  #
  # Why this exists: Android Lint enforces CLDR plural completeness. A Polish
  # <plurals> block that only ships `one` + `other` is a hard Lint error (Polish
  # also needs `few` and `many`); a Thai <plurals> block that ships `one` at all
  # is a Lint warning (Thai uses only `other`). The English source typically
  # only carries `one` + `other`, so the engine must know which target quantities
  # to synthesize -- and which to drop -- when generating localized output.
  #
  # Locales not present in the data file fall back to pass-through (source
  # quantities verbatim), which is the engine's pre-existing behavior. Adding a
  # locale to the data is the only way to opt it into the enforced shape.
  module CldrPlurals
    DEFAULT_PATH = File.expand_path(
      '../../context/cldr_plurals.json', File.dirname(__FILE__)
    )

    module_function

    # Returns a Hash<String, Array<String>> of locale → required quantities.
    # Keys starting with "_" (e.g. "_comment") are dropped silently.
    def load_data(path = DEFAULT_PATH)
      return {} unless File.exist?(path)

      raw = JSON.parse(File.read(path))
      raw.reject { |k, _| k.to_s.start_with?('_') }
    rescue JSON::ParserError
      {}
    end

    # Required quantities for `locale`, or `nil` when the locale isn't listed
    # (callers must treat `nil` as "fall back to source quantities").
    def quantities_for(locale, data: nil)
      table = data || @data ||= load_data
      table[locale.to_s]
    end

    # For prompts: a short, deterministic line we can embed in the system block.
    # Empty when the locale isn't listed.
    def prompt_line_for(locale, data: nil)
      qs = quantities_for(locale, data: data)
      return '' if qs.nil? || qs.empty?

      "For locale `#{locale}` the Android CLDR plural categories you MUST cover " \
        "in any <plurals> block are exactly: #{qs.join(', ')}. " \
        'Categories not listed must NOT appear.'
    end
  end
end
