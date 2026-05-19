# frozen_string_literal: true

require 'json'

module WooAiTranslation
  # Per-key code-derived context (screen/feature, surrounding usage, developer
  # notes) is the main translation-quality lever. This is the integration seam
  # for the shared AINFRA-1707 strings context-extraction pipeline.
  #
  # v1 consumes a simple committed JSON map:
  #
  #   { "<string-name>": "context string", ... }
  #
  # at fastlane/ai_translation/context/strings_context.json. When the AINFRA-1707
  # pipeline output format is finalized, only #context_for needs to change --
  # the engine and prompt are agnostic to where context comes from.
  class ContextProvider
    def self.from_file(path)
      data = path && File.exist?(path) ? JSON.parse(File.read(path)) : {}
      new(data)
    rescue JSON::ParserError
      new({})
    end

    def initialize(map = {})
      @map = map || {}
    end

    def context_for(name)
      @map[name].to_s
    end

    def any?
      !@map.empty?
    end
  end
end
