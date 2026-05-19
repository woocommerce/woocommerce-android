# frozen_string_literal: true

require 'fileutils'
require 'tmpdir'

module WooAiTranslation
  # Rollout shadow mode: generate AI translations into a throwaway dir and diff
  # them against the committed (human/GlotPress) `values-<locale>/strings.xml`
  # WITHOUT touching the repo. Produces a reviewable report so quality and
  # formatter drift can be assessed before cutover.
  class ShadowDiff
    def initialize(source_path:, res_dir:, translator:, context:, logger: nil)
      @source_path = source_path
      @res_dir = res_dir
      @translator = translator
      @context = context
      @logger = logger || ->(_m) {}
    end

    # Returns a Markdown report string.
    def run(locales:, sample: 10)
      Dir.mktmpdir('woo-shadow') do |tmp|
        Engine.new(
          source_path: @source_path, res_dir: tmp,
          manifest: Manifest.new, manifest_path: File.join(tmp, 'm.json'),
          translator: @translator, context: @context, logger: @logger
        ).run_strings(locales: locales, origin: 'ai')

        ["# Shadow-mode diff (AI vs committed)\n", *locales.map { |l| locale_section(tmp, l, sample) }].join("\n")
      end
    end

    private

    def locale_section(tmp, locale, sample)
      ai = parse(File.join(tmp, "values-#{locale}", 'strings.xml'))
      human = parse(File.join(@res_dir, "values-#{locale}", 'strings.xml'))

      added = (ai.keys - human.keys).sort
      removed = (human.keys - ai.keys).sort
      changed = (ai.keys & human.keys).select { |k| ai[k] != human[k] }.sort

      lines = ["## #{locale}",
               "- keys: ai=#{ai.size} committed=#{human.size}",
               "- added=#{added.size} removed=#{removed.size} changed=#{changed.size}\n"]
      changed.first(sample).each do |k|
        lines << "  - `#{k}`\n    - committed: #{human[k].inspect}\n    - ai:        #{ai[k].inspect}"
      end
      lines.join("\n")
    end

    def parse(path)
      return {} unless File.exist?(path)

      AndroidResources::Parser.parse_file(path).units.each_with_object({}) do |u, h|
        h[u.name] = u.entries.map { |e| e[:source] }
      end
    end
  end
end
