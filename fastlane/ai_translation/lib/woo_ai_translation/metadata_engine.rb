# frozen_string_literal: true

require 'json'
require 'digest'
require 'fileutils'

module WooAiTranslation
  # Play Store metadata translation -- the GlotPress PlayStoreStrings.pot /
  # gp_downloadmetadata replacement (workstream 3c).
  #
  # Two cadences, both handled here:
  # - Static listing (title/short/full description): translated when the
  #   English source changes. Eligible at PR-time and sweep.
  # - Per-release notes (changelogs/default.txt): rewritten every release;
  #   translated ONLY when include_release_notes is set (code-freeze sweep),
  #   never at PR-time, and tracked per version in the manifest.
  #
  # Hard per-field character cap: on overflow, re-prompt once with a tighter
  # budget; if still over, keep a cap-safe existing value or fall back to
  # English only when the English source itself is within cap.
  class MetadataEngine
    Report = Struct.new(:locale, :translated, :reused, :fallback, keyword_init: true)

    # field: { name:, rel:, cap:, release_note:, model: }
    FIELDS = [
      { name: 'title',             rel: 'title.txt',              cap: 50, release_note: false, marketing: true },
      { name: 'short_description', rel: 'short_description.txt',   cap: 80, release_note: false, marketing: true },
      { name: 'full_description',  rel: 'full_description.txt',    cap: 0,  release_note: false, marketing: false },
      { name: 'release_notes',     rel: 'changelogs/default.txt',  cap: 500, release_note: true, marketing: false }
    ].freeze

    def initialize(translator:, manifest:, manifest_path:, logger: nil)
      @translator = translator
      @manifest = manifest
      @manifest_path = manifest_path
      @logger = logger || ->(_m) {}
    end

    # locale_dirs: { '<out-locale-dir>' => '<unused>' } -- we pass a list of
    # output locale directory names (Play codes), translating from source_dir.
    def run(source_dir:, out_base:, locales:, include_release_notes:, version: nil, fields: FIELDS)
      reports = locales.map do |loc|
        translate_locale(source_dir, out_base, loc, include_release_notes, version, fields)
      end
      @manifest.save(@manifest_path)
      reports
    end

    private

    def translate_locale(source_dir, out_base, locale, include_release_notes, version, fields)
      translated = 0
      reused = 0
      fallback = []

      fields.each do |f|
        next if f[:release_note] && !include_release_notes

        src = File.join(source_dir, f[:rel])
        next unless File.exist?(src)

        source_text = File.read(src)
        ver = f[:release_note] ? version : nil
        model = f[:marketing] ? ESCALATION_MODEL : DEFAULT_MODEL
        ck = @manifest.cache_key(source: source_text, context: f[:name], locale: locale, model: model)

        if !@manifest.metadata_stale?(field: f[:name], locale: locale, expected_cache_key: ck, version: ver)
          reused += 1
          next
        end

        value = translate_field(f, source_text, locale, model)
        record = true
        if value.nil?
          fallback << f[:name]
          value, record = fallback_field_value(out_base, locale, f, source_text)
          next if value.nil?
        else
          translated += 1
        end

        write_field(out_base, locale, f, value)
        if record
          @manifest.record_metadata(field: f[:name], locale: locale, cache_key: ck,
                                    model: model, origin: 'ai', version: ver)
        end
      end

      Report.new(locale: locale, translated: translated, reused: reused, fallback: fallback)
    end

    # Up to two attempts: normal, then a tighter length budget. Returns nil if
    # it still overflows (caller falls back to English).
    def translate_field(field, source_text, locale, model)
      [nil, "Hard limit: at most #{field[:cap]} characters. Be more concise."].each do |tighten|
        next if tighten && field[:cap].to_i.zero?

        out = @translator.translate(
          locale: locale, model: model, style: tighten, raw_text: true,
          items: [{ id: field[:name], source: source_text, context: field[:name] }]
        )
        text = out[field[:name]]
        next if text.nil?

        cap_err = Validators.char_cap(field: field[:name], text: text, cap: field[:cap])
        return text if cap_err.empty?

        @logger.call("metadata #{field[:name]} [#{locale}] over cap, retrying tighter") if tighten.nil?
      end
      @logger.call("metadata #{field[:name]} [#{locale}] still over cap -> English fallback")
      nil
    end

    def fallback_field_value(out_base, locale, field, source_text)
      existing = read_field(out_base, locale, field)
      if cap_safe?(field, existing)
        @logger.call("metadata #{field[:name]} [#{locale}] keeping existing cap-safe value")
        return [existing, false]
      end

      if cap_safe?(field, source_text)
        @logger.call("metadata #{field[:name]} [#{locale}] using English fallback")
        return [source_text, true]
      end

      @logger.call("metadata #{field[:name]} [#{locale}] has no cap-safe fallback; leaving unchanged")
      [nil, false]
    end

    def read_field(out_base, locale, field)
      path = File.join(out_base, locale, field[:rel])
      return nil unless File.exist?(path)

      File.read(path)
    end

    def cap_safe?(field, text)
      return false if text.nil?

      Validators.char_cap(field: field[:name], text: text, cap: field[:cap]).empty?
    end

    def write_field(out_base, locale, field, value)
      dest = File.join(out_base, locale, field[:rel])
      FileUtils.mkdir_p(File.dirname(dest))
      File.write(dest, value)
    end
  end
end
