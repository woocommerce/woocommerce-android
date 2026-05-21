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

    def initialize(source_path:, res_dir:, manifest:, manifest_path:, translator:, context:, force_model: nil, baseline_dir: nil, logger: nil)
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
      # Root of fastlane/ai_translation/baseline/. When set, the writer emits
      # raw XML from baseline/values-<locale>/strings.xml for keys whose
      # manifest origin is still glotpress-import. nil disables the preserved-
      # line path (sweep still works, but everything goes through render_unit).
      @baseline_dir = baseline_dir
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
      # Read the sidecar baseline once per locale; the writer attaches preserved
      # blocks for keys whose manifest origin is still glotpress-import. Empty
      # hash when no sidecar exists for this locale (the 15 AI-backfilled
      # locales and any future-added locales hit this path).
      baseline_raw = load_baseline_raw(locale)

      plan = build_plan(source_units, locale, existing)
      results = run_translation(locale, plan[:pending])

      translated = []
      failed = []
      plan[:pending].each do |st|
        apply_and_validate(st, results, locale, origin, translated, failed)
      end

      ordered = source_units.map { |u| plan[:units][u.name] }
      attach_preserved_xml(ordered, locale, baseline_raw)
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
    # queue it for (re)translation. The decision is driven entirely by the
    # source: a key is queued only when its English text changed (source_sha
    # diverges from the manifest) or when no translation exists yet. Model
    # bumps, prompt rewrites, and context-comment edits never invalidate.
    def build_plan(source_units, locale, existing)
      units = {}
      pending = []
      reused = []

      target_quantities = CldrPlurals.quantities_for(locale)
      source_units.each do |u|
        default_model = model_for(u)
        source_sha = Digest::SHA256.hexdigest(u.source_signature)
        # For <plurals>, reshape the output shell to match the target locale's
        # CLDR-required quantity categories so the model is asked for exactly
        # the forms Android expects. Non-plural units are unaffected.
        shell = u.dup_shell_for_locale(target_quantities)
        units[u.name] = shell

        if !@manifest.stale?(name: u.name, locale: locale, expected_source_sha: source_sha) && reuse(shell, existing)
          reused << u.name
        else
          # Force-model overrides per-key default; reused units keep their
          # originally recorded model (we never re-translate just to apply a
          # different model -- that's an on-demand --keys invalidation, not
          # an automatic one).
          actual_model = @force_model || default_model
          pending << { unit: shell, source: u, source_sha: source_sha, model: actual_model }
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
          #
          # The entry id ALREADY encodes the unit name for arrays and plurals
          # (`unit_name[0]`, `unit_name{one}`); for plain strings it equals
          # `unit_name`. Sending `"unit_name::entry_id"` was therefore both
          # redundant and -- empirically -- a structural hazard for Sonnet:
          # the long, repetitive `name::name` shape caused the model to emit
          # `,` instead of `:` between key and value, breaking JSON parse.
          # Using the entry id directly cuts output size ~30% AND fixes the
          # malformation. See repro at /tmp/probe_sonnet_clean_ids.rb.
          st[:unit].translation_requests.map do |r|
            { id: r[:id], source: r[:source], context: ctx }
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
        # Lookup matches run_translation's id format: just the entry id (which
        # already encodes the unit name for arrays/plurals; equals the unit
        # name for plain strings).
        mapped[e[:id]] = results[e[:id]] if results.key?(e[:id])
      end
      shell.apply!(mapped)

      unless shell.fully_translated?
        failed << "#{source.name} (incomplete translation)"
        return
      end

      errs = translation_errors(source, shell)
      unless errs.empty?
        @logger.call("validation failed #{source.name} [#{locale}]: #{errs.join('; ')}")
        failed << "#{source.name} (#{errs.first})"
        clear(shell)
        return
      end

      @manifest.record(
        name: source.name, locale: locale,
        model: state[:model], origin: origin,
        source_sha: state[:source_sha]
      )
      translated << source.name
    end

    def translation_errors(source, shell)
      preserve_terms = @context.respond_to?(:preserved_terms) ? @context.preserved_terms : []
      # `formatted="false"` declares "do not treat % as a format specifier" -- a
      # literal "%" character. Our placeholder regex is intentionally greedy
      # (covers "% d", "%1$s", etc.), so it must NOT run on these units or it
      # would flag false positives on every literal "50% off" / "5 % rate".
      validate_placeholders = source.attributes['formatted'] != 'false'
      # Compare each output entry's placeholders against its OWN recorded source
      # (not against `source.entries` by index): plural shells expanded for a
      # locale's CLDR rules may carry more entries than the English source has,
      # and a zip-by-index would compare a real translation to a nil source.
      shell.entries.flat_map do |e|
        placeholder_errors = validate_placeholders ? Validators.placeholder_parity(e[:source], e[:value]) : []
        placeholder_errors + Validators.glossary_preservation(e[:source], e[:value], preserve_terms)
      end
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

    def load_baseline_raw(locale)
      return {} unless @baseline_dir

      path = File.join(@baseline_dir, "values-#{locale}", 'strings.xml')
      AndroidResources::BaselineReader.read(path)
    end

    # Attach `preserved_xml` only to units that (a) are still recorded as
    # origin=glotpress-import in the manifest for this locale and (b) have a
    # block in the sidecar baseline. The manifest's origin can flip from
    # glotpress-import to ai mid-run (when an AI-translated key replaces a
    # human one because its English source changed); in that case the manifest
    # has just been written via apply_and_validate, so the read here sees the
    # correct, current origin.
    def attach_preserved_xml(units, locale, baseline_raw)
      return if baseline_raw.empty?

      units.each do |u|
        next unless @manifest.origin(name: u.name, locale: locale) == 'glotpress-import'

        raw = baseline_raw[u.name]
        u.preserved_xml = raw if raw
      end
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
