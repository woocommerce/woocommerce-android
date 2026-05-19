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
      strict: false
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

      # The glotpress-import origin is produced by the separate baseline import
      # tool (rollout phase), not by this AI path.
      engine = Engine.new(
        source_path: opts[:source],
        res_dir: opts[:res_dir],
        manifest: Manifest.load(opts[:manifest]),
        manifest_path: opts[:manifest],
        translator: Translator.new(client: client, batch_size: opts[:batch], logger: logger),
        context: ContextProvider.from_file(opts[:context]),
        logger: logger
      )

      reports = engine.run_strings(locales: opts[:locales], origin: 'ai')
      print_summary(reports, opts[:mode])
      problems = reports.sum { |r| r.failed.size + r.gate_errors.size }
      opts[:strict] && problems.positive? ? 1 : 0
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
        p.on('--mode MODE', 'prtime|ondemand|sweep|backfill') { |v| o[:mode] = v }
        p.on('--batch N', Integer) { |v| o[:batch] = v }
        p.on('--offline', 'Use the deterministic stub (no network/spend)') { o[:offline] = true }
        p.on('--strict', 'Exit non-zero if any key failed') { o[:strict] = true }
      end.parse!(argv)

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
