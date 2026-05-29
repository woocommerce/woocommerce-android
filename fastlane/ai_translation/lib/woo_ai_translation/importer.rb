# frozen_string_literal: true

require 'digest'
require 'fileutils'
require 'set'

module WooAiTranslation
  # One-time baseline seed for the existing locales.
  #
  # The committed `values-<locale>/strings.xml` files ARE the final GlotPress
  # export (the last download). This records every key those files already
  # translate into the manifest with origin `glotpress-import`, WITHOUT
  # re-serializing or re-translating them -- years of human-reviewed quality is
  # preserved byte-for-byte at near-zero cost.
  #
  # After import, the engine's delta logic treats imported keys as up-to-date
  # (reused from the existing file); only the keys GlotPress never translated
  # (pre-existing gaps) fall through to AI. New locales have no existing file,
  # so they are a full AI backfill.
  class Importer
    Report = Struct.new(:locale, :imported, :gaps, keyword_init: true)

    def initialize(source_path:, res_dir:, manifest:, context:, baseline_dir: nil, logger: nil)
      @source_path = source_path
      @res_dir = res_dir
      @manifest = manifest
      @context = context
      @baseline_dir = baseline_dir
      @logger = logger || ->(_m) {}
    end

    def import(locales:)
      doc = AndroidResources::Parser.parse_file(@source_path)
      source_units = doc.translatable_units
      locales.map { |loc| import_locale(source_units, loc) }
    end

    private

    def import_locale(source_units, locale)
      path = File.join(@res_dir, "values-#{locale}", 'strings.xml')
      existing = existing_names(path)
      copy_baseline(path, locale) if @baseline_dir && File.exist?(path) && !existing.empty?
      imported = 0
      gaps = 0

      source_units.each do |u|
        model = WooAiTranslation.model_for(u.name)
        if existing.include?(u.name)
          @manifest.record(
            name: u.name, locale: locale,
            model: model, origin: 'glotpress-import',
            source_sha: Digest::SHA256.hexdigest(u.source_signature)
          )
          imported += 1
        else
          gaps += 1
        end
      end

      @logger.call("import #{locale}: #{imported} human keys preserved, #{gaps} gaps to AI-fill")
      Report.new(locale: locale, imported: imported, gaps: gaps)
    end

    def copy_baseline(path, locale)
      dest = File.join(@baseline_dir, "values-#{locale}", 'strings.xml')
      FileUtils.mkdir_p(File.dirname(dest))
      FileUtils.cp(path, dest)
    end

    # Names fully present (every entry) in the existing localized file.
    def existing_names(path)
      return [] unless File.exist?(path)

      AndroidResources::Parser.parse_file(path).units
                              .select { |u| u.entries.all? { |e| !e[:source].to_s.empty? } }
                              .map(&:name).to_set
    rescue StandardError => e
      @logger.call("could not parse #{path}: #{e.message}; treating as no baseline")
      [].to_set
    end
  end
end
