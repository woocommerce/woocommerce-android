# frozen_string_literal: true

require 'rexml/document'
require 'set'

module WooAiTranslation
  # Hard, blocking gates. Human review is non-blocking, but these are not: a
  # broken translation must never ship.
  module Validators
    module_function

    # %1$s / %2$d positional, %s / %d non-positional, %% literal.
    PLACEHOLDER = /%(?:\d+\$)?[-#+ 0,(]?\d*(?:\.\d+)?[a-zA-Z]|%%/

    def placeholders(text)
      text.to_s.scan(PLACEHOLDER).sort
    end

    # Same placeholder multiset in source and translation.
    def placeholder_parity(source, translation)
      s = placeholders(source)
      t = placeholders(translation)
      return [] if s == t

      ["placeholder mismatch: source=#{s.inspect} translation=#{t.inspect}"]
    end

    # Brand/glossary safety check. For every preserve=true glossary term that
    # appears in the source, the exact same term must appear in the translation.
    # Longest-match-wins prevents WooCommerce from also requiring Woo.
    def glossary_preservation(source, translation, terms)
      src = source.to_s.dup
      tx = translation.to_s
      errors = []

      terms.map(&:to_s).reject(&:empty?).uniq.sort_by { |term| -term.length }.each do |term|
        term_pattern = glossary_term_pattern(term)
        next unless src.match?(term_pattern)

        src.gsub!(term_pattern, "\u0001" * term.length)
        errors << "glossary term not preserved: #{term}" unless tx.match?(term_pattern)
      end

      errors
    end

    def glossary_term_pattern(term)
      escaped = Regexp.escape(term)
      return /(?<![[:alnum:]])#{escaped}(?![[:alnum:]])/ if boundary_required_glossary_term?(term)

      /#{escaped}/
    end

    def boundary_required_glossary_term?(term)
      term.match?(/\A[A-Z0-9 -]+\z/)
    end

    def xml_well_formed(path)
      REXML::Document.new(File.read(path))
      []
    rescue REXML::ParseException => e
      ["malformed XML in #{path}: #{e.message}"]
    end

    # Localized files must not introduce keys that are not translatable in
    # source (extra), and should not silently lose keys (missing reported).
    def key_parity(source_names:, output_names:, require_all: false)
      errors = []
      extra = output_names - source_names
      missing = source_names - output_names
      errors << "extra keys not in source: #{extra.sort.inspect}" unless extra.empty?
      errors << "missing keys from output: #{missing.sort.inspect}" if require_all && !missing.empty?
      errors
    end

    def non_translatable_string_reference_integrity(source_names:, output_units:)
      source = source_names.to_set
      errors = []

      output_units.reject(&:translatable?).each do |unit|
        unit.entries.each do |entry|
          referenced = entry[:source].to_s[/\A@string\/([^\s\/]+)\z/, 1]
          next if referenced.nil? || source.include?(referenced)

          errors << "non-translatable #{unit.name} references missing source string: @string/#{referenced}"
        end
      end

      errors
    end

    # PR-time partial writes must never delete unrelated existing translations.
    # Selected keys may legitimately disappear when they were removed from source
    # or failed validation and should fall back to default resources.
    def no_unselected_key_loss(before_names:, output_names:, selected_names:)
      selected = selected_names.to_set
      lost = before_names - output_names - selected.to_a
      return [] if lost.empty?

      ["unselected keys dropped from output: #{lost.sort.inspect}"]
    end

    # The real, blocking plural gate. Only enforced for pairs that BOTH exist in
    # the source (a genuine pair): if one side ships, the other must too -- the
    # engine must never collapse a real `_single`/`_multiple` pair. Source-side
    # asymmetry is intentional in this codebase and is NOT a violation.
    def plural_pair_integrity(source_names:, output_names:)
      src = source_names.to_set
      out = output_names.to_set
      errors = []
      source_names.each do |n|
        next unless n.end_with?('_single')

        sib = "#{n[0...-'_single'.length]}_multiple"
        next unless src.include?(sib)

        errors << "plural pair collapsed in output: #{n} / #{sib}" if out.include?(n) ^ out.include?(sib)
      end
      errors
    end

    # Informational only (NOT a gate): source keys using one side of the manual
    # convention without the other. Surfaced in the spot-check artifact.
    def plural_pairs(names)
      set = names.to_set
      errors = []
      names.each do |n|
        if n.end_with?('_single')
          base = n.sub(/_single\z/, '')
          errors << "missing #{base}_multiple for #{n}" unless set.include?("#{base}_multiple")
        elsif n.end_with?('_multiple')
          base = n.sub(/_multiple\z/, '')
          errors << "missing #{base}_single for #{n}" unless set.include?("#{base}_single")
        end
      end
      errors
    end

    # Play/App Store hard character cap. cap == 0 means unbounded.
    def char_cap(field:, text:, cap:)
      return [] if cap.to_i.zero? || text.to_s.length <= cap

      ["#{field} exceeds cap #{cap} (#{text.length})"]
    end

    # Output `<plurals>` blocks must cover exactly the CLDR-required quantity
    # categories for the target locale. Missing categories are a hard Android
    # Lint error (e.g. Polish without `few`/`many`); categories the locale
    # doesn't use (e.g. `one` for Thai) are a warning. Both are real bugs and
    # we treat them as blocking gates so they cannot ship.
    #
    # `output_plurals_quantities`: Hash<String, Array<String>> mapping the
    # output unit's name to the list of `quantity` values it ships. Caller
    # restricts this to fully-translated units.
    # `required`: the locale's CLDR list (from CldrPlurals.quantities_for).
    # `nil`/empty means "no enforcement"; the gate is skipped.
    def plural_form_coverage(output_plurals_quantities:, required:)
      return [] if required.nil? || required.empty?

      req_set = required.to_set
      errors = []
      output_plurals_quantities.each do |name, quantities|
        have = quantities.to_set
        missing = req_set - have
        extra   = have - req_set
        errors << "plural #{name} missing CLDR quantities: #{missing.to_a.sort.inspect}" unless missing.empty?
        errors << "plural #{name} has irrelevant quantities: #{extra.to_a.sort.inspect}" unless extra.empty?
      end
      errors
    end
  end
end
