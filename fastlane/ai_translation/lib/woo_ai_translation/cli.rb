# frozen_string_literal: true

require 'optparse'

module WooAiTranslation
  # Entry point used by the `ai_translate` Fastlane lane and for local runs.
  # Modes (prtime|ondemand|sweep|backfill) select operational policy; the
  # per-key delta algorithm is identical for all of them.
  module CLI
    DEFAULTS = {
      source: 'WooCommerce/src/main/res/values/strings.xml',
      res_dir: 'WooCommerce/src/main/res',
      manifest: 'fastlane/ai_translation/translation-manifest.json',
      context: 'fastlane/ai_translation/context/strings_context.json',
      mode: 'prtime',
      batch: Translator::DEFAULT_BATCH,
      offline: false,
      strict: false,
      metadata_source: 'fastlane/metadata/android/en-US',
      metadata_out: 'fastlane/metadata/android',
      gp_locales: [],
      include_release_notes: false,
      version: nil
    }.freeze

    module_function

    def run(argv)
      opts = parse(argv)
      logger = ->(m) { warn("[ai_translate] #{m}") }

      client = opts[:offline] ? StubClient.new : AnthropicClient.from_env
      unless client.available?
        warn('[ai_translate] ANTHROPIC_API_KEY not set and not --offline; nothing to do.')
        return 0
      end

      translator = Translator.new(client: client, batch_size: opts[:batch], logger: logger)
      manifest = Manifest.load(opts[:manifest])

      return run_metadata(opts, translator, manifest, logger) if opts[:mode] == 'metadata'

      # The glotpress-import origin is produced by the separate baseline import
      # tool (rollout phase), not by this AI path.
      engine = Engine.new(
        source_path: opts[:source],
        res_dir: opts[:res_dir],
        manifest: manifest,
        manifest_path: opts[:manifest],
        translator: translator,
        context: ContextProvider.from_file(opts[:context]),
        logger: logger
      )

      reports = engine.run_strings(locales: opts[:locales], origin: 'ai')
      print_summary(reports, opts[:mode])
      problems = reports.sum { |r| r.failed.size + r.gate_errors.size }
      opts[:strict] && problems.positive? ? 1 : 0
    end

    def run_metadata(opts, translator, manifest, logger)
      reports = MetadataEngine.new(
        translator: translator, manifest: manifest,
        manifest_path: opts[:manifest], logger: logger
      ).run(
        source_dir: opts[:metadata_source],
        out_base: opts[:metadata_out],
        locales: opts[:gp_locales],
        include_release_notes: opts[:include_release_notes],
        version: opts[:version]
      )
      reports.each do |r|
        warn(format('[ai_translate] meta %-8s translated=%-3d reused=%-3d fallback=%s',
                    r.locale, r.translated, r.reused, r.fallback.inspect))
      end
      fb = reports.sum { |r| r.fallback.size }
      opts[:strict] && fb.positive? ? 1 : 0
    end

    def parse(argv)
      o = DEFAULTS.dup
      OptionParser.new do |p|
        p.banner = 'Usage: woo-ai-translate --locales pl,cs,da [options]'
        p.on('--source PATH') { |v| o[:source] = v }
        p.on('--res-dir PATH') { |v| o[:res_dir] = v }
        p.on('--manifest PATH') { |v| o[:manifest] = v }
        p.on('--context PATH') { |v| o[:context] = v }
        p.on('--locales LIST', 'Comma-separated Android locale qualifiers') { |v| o[:locales] = v }
        p.on('--locales-file PATH') { |v| o[:locales] = File.read(v).split }
        p.on('--mode MODE', 'prtime|ondemand|sweep|backfill|metadata') { |v| o[:mode] = v }
        p.on('--batch N', Integer) { |v| o[:batch] = v }
        p.on('--offline', 'Use the deterministic stub (no network/spend)') { o[:offline] = true }
        p.on('--strict', 'Exit non-zero if any key failed') { o[:strict] = true }
        # Metadata mode (workstream 3c)
        p.on('--metadata-source DIR') { |v| o[:metadata_source] = v }
        p.on('--metadata-out DIR') { |v| o[:metadata_out] = v }
        p.on('--gp-locales LIST', 'Comma-separated Play Store locale dir names') { |v| o[:gp_locales] = v }
        p.on('--include-release-notes', 'Translate changelogs (code-freeze only)') { o[:include_release_notes] = true }
        p.on('--version VER') { |v| o[:version] = v }
      end.parse!(argv)

      o[:gp_locales] = Array(o[:gp_locales]).flat_map { |x| x.to_s.split(',') }.map(&:strip).reject(&:empty?)
      return o if o[:mode] == 'metadata'

      o[:locales] = Array(o[:locales]).flat_map { |x| x.split(',') }.map(&:strip).reject(&:empty?)
      raise OptionParser::MissingArgument, '--locales is required' if o[:locales].empty?

      o
    end

    def print_summary(reports, mode)
      warn("[ai_translate] mode=#{mode}")
      reports.each do |r|
        warn(format('[ai_translate] %-8s translated=%-4d reused=%-4d written=%-4d failed=%d gate=%d',
                    r.locale, r.translated, r.reused, r.written, r.failed.size, r.gate_errors.size))
        r.failed.first(10).each { |f| warn("    - #{f}") }
        r.gate_errors.first(10).each { |g| warn("    ! #{g}") }
      end
    end
  end
end
