# frozen_string_literal: true

require 'json'

module WooAiTranslation
  # Orchestrates: detect delta vs the manifest -> attach context -> translate
  # only missing/changed keys -> validate (hard gate) -> write
  # values-<locale>/strings.xml -> update the manifest.
  #
  # prtime / ondemand / sweep share this exact algorithm; they differ only
  # operationally (which locales/branch, real-time vs batch API). "sweep" is
  # just "run over every locale" -- the per-key delta logic is identical, which
  # is why the PR-time job is a safe partial of the code-freeze safety net.
  class Engine
    Report = Struct.new(:locale, :translated, :reused, :failed, :written, :gate_errors, keyword_init: true)

    def initialize(source_path:, res_dir:, manifest:, manifest_path:, translator:, context:, force_model: nil, logger: nil)
      @source_path = source_path
      @res_dir = res_dir
      @manifest = manifest
      @manifest_path = manifest_path
      @translator = translator
      @context = context
      # When set, overrides per-unit model selection for keys that need to be
      # (re)translated. Useful for quality retries: re-run with a stronger
      # model only against the keys that previously failed validation; reused
      # keys keep their original recorded model.
      @force_model = force_model
      @logger = logger || ->(_m) {}
    end

    def run_strings(locales:, origin: 'ai')
      doc = AndroidResources::Parser.parse_file(@source_path)
      source_units = doc.translatable_units

      # Source-side _single/_multiple asymmetry is intentional here -- report it
      # for the spot-check artifact, never block on it.
      asymmetry = Validators.plural_pairs(doc.translatable_names)
      @logger.call("source has #{asymmetry.size} unpaired manual-plural keys (informational)") unless asymmetry.empty?

      reports = locales.map do |loc|
        r = translate_locale(source_units, loc, origin)
        # Save after every locale so a long-running backfill survives Ctrl-C /
        # a CLI hiccup: next run resumes from where we stopped.
        @manifest.save(@manifest_path)
        r
      end
      reports
    end

    private

    def translate_locale(source_units, locale, origin)
      out_path = File.join(@res_dir, "values-#{locale}", 'strings.xml')
      existing = load_existing(out_path)

      plan = build_plan(source_units, locale, existing)
      results = run_translation(locale, plan[:pending])

      translated = []
      failed = []
      plan[:pending].each do |st|
        apply_and_validate(st, results, locale, origin, translated, failed)
      end

      ordered = source_units.map { |u| plan[:units][u.name] }
      AndroidResources::Writer.write(out_path, ordered, locale)

      malformed = Validators.xml_well_formed(out_path)
      raise "Generated #{out_path} is not well-formed: #{malformed.first}" unless malformed.empty?

      source_names = source_units.map(&:name)
      output_names = ordered.select(&:fully_translated?).map(&:name)
      output_plurals_qs = ordered.select { |u| u.fully_translated? && u.type == :plurals }
                                 .to_h { |u| [u.name, u.entries.map { |e| e[:quantity] }] }
      gate_errors = Validators.key_parity(source_names: source_names, output_names: output_names) +
                    Validators.plural_pair_integrity(source_names: source_names, output_names: output_names) +
                    Validators.plural_form_coverage(output_plurals_quantities: output_plurals_qs,
                                                    required: CldrPlurals.quantities_for(locale))
      gate_errors.each { |e| @logger.call("GATE [#{locale}] #{e}") }

      Report.new(
        locale: locale,
        translated: translated.size,
        reused: plan[:reused].size,
        failed: failed,
        written: output_names.size,
        gate_errors: gate_errors
      )
    end

    # Decide, per source unit, whether to reuse the existing translation or
    # queue it for (re)translation.
    def build_plan(source_units, locale, existing)
      units = {}
      pending = []
      reused = []

      target_quantities = CldrPlurals.quantities_for(locale)
      source_units.each do |u|
        default_model = model_for(u)
        ctx = context_for_unit(u)
        ck = @manifest.cache_key(source: u.source_signature, context: ctx, locale: locale, model: default_model)
        # For <plurals>, reshape the output shell to match the target locale's
        # CLDR-required quantity categories so the model is asked for exactly
        # the forms Android expects. Non-plural units are unaffected.
        shell = u.dup_shell_for_locale(target_quantities)
        units[u.name] = shell

        if !@manifest.stale?(name: u.name, locale: locale, expected_cache_key: ck) && reuse(shell, existing)
          reused << u.name
        else
          # Force-model only affects NEW translations; reused units keep their
          # originally recorded model. Cache key is recorded under the model
          # that actually produced the translation, for auditability.
          actual_model = @force_model || default_model
          actual_ck = @force_model ? @manifest.cache_key(source: u.source_signature, context: ctx, locale: locale, model: actual_model) : ck
          pending << { unit: shell, source: u, ck: actual_ck, model: actual_model }
        end
      end

      { units: units, pending: pending, reused: reused }
    end

    def run_translation(locale, pending)
      results = {}
      style = @context.respond_to?(:style_for) ? @context.style_for(locale) : ''
      pending.group_by { |st| st[:model] }.each do |model, group|
        items = group.flat_map do |st|
          ctx = context_for_unit(st[:source])
          # Use the SHELL's translation_requests, not the source's: for plural
          # units the shell may carry extra quantities synthesized from the
          # target locale's CLDR rules, and we need a translation for each.
          st[:unit].translation_requests.map do |r|
            { id: "#{st[:source].name}::#{r[:id]}", source: r[:source], context: ctx }
          end
        end
        next if items.empty?

        results.merge!(@translator.translate(locale: locale, items: items, model: model, style: style))
      end
      results
    end

    # Combined per-key context: the immediately-preceding XML comment in the
    # source file (sticky to the section) plus any AINFRA-1707 entry. Both are
    # dev-authored and cheap; included in both the cache key and the prompt so
    # the manifest and the model see the same input.
    def context_for_unit(unit)
      [unit.respond_to?(:comment) ? unit.comment.to_s : '',
       @context.context_for(unit.name).to_s].reject(&:empty?).join("\n").strip
    end

    def apply_and_validate(state, results, locale, origin, translated, failed)
      source = state[:source]
      shell = state[:unit]
      mapped = {}
      shell.entries.each do |e|
        gid = "#{source.name}::#{e[:id]}"
        mapped[e[:id]] = results[gid] if results.key?(gid)
      end
      shell.apply!(mapped)

      unless shell.fully_translated?
        failed << "#{source.name} (incomplete translation)"
        return
      end

      errs = placeholder_errors(source, shell)
      unless errs.empty?
        @logger.call("validation failed #{source.name} [#{locale}]: #{errs.join('; ')}")
        failed << "#{source.name} (#{errs.first})"
        clear(shell)
        return
      end

      @manifest.record(
        name: source.name, locale: locale, cache_key: state[:ck],
        model: state[:model], origin: origin,
        source_sha: Digest::SHA256.hexdigest(source.source_signature)
      )
      translated << source.name
    end

    def placeholder_errors(source, shell)
      # `formatted="false"` declares "do not treat % as a format specifier" -- a
      # literal "%" character. Our placeholder regex is intentionally greedy
      # (covers "% d", "%1$s", etc.), so it must NOT run on these units or it
      # would flag false positives on every literal "50% off" / "5 % rate".
      return [] if source.attributes['formatted'] == 'false'

      # Compare each output entry's placeholders against its OWN recorded source
      # (not against `source.entries` by index): plural shells expanded for a
      # locale's CLDR rules may carry more entries than the English source has,
      # and a zip-by-index would compare a real translation to a nil source.
      shell.entries.flat_map { |e| Validators.placeholder_parity(e[:source], e[:value]) }
    end

    def clear(shell)
      shell.entries.each { |e| e[:value] = nil }
    end

    # Copy a previously translated value out of the existing localized file.
    def reuse(shell, existing)
      prev = existing[shell.name]
      return false if prev.nil?

      by_id = prev.entries.to_h { |e| [e[:id], e[:source]] }
      return false unless shell.entries.all? { |e| by_id.key?(e[:id]) }

      shell.entries.each { |e| e[:value] = by_id[e[:id]] }
      true
    end

    def load_existing(path)
      return {} unless File.exist?(path)

      AndroidResources::Parser.parse_file(path).units.to_h { |u| [u.name, u] }
    rescue StandardError => e
      @logger.call("could not parse existing #{path}: #{e.message}; retranslating all")
      {}
    end

    def model_for(unit)
      WooAiTranslation.model_for(unit.name)
    end
  end
end
