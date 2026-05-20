# frozen_string_literal: true

require_relative 'woo_ai_translation/version'
require_relative 'woo_ai_translation/android_resources'
require_relative 'woo_ai_translation/manifest'
require_relative 'woo_ai_translation/context_provider'
require_relative 'woo_ai_translation/anthropic_client'
require_relative 'woo_ai_translation/claude_cli_client'
require_relative 'woo_ai_translation/translator'
require_relative 'woo_ai_translation/validators'
require_relative 'woo_ai_translation/engine'
require_relative 'woo_ai_translation/metadata_engine'
require_relative 'woo_ai_translation/importer'
require_relative 'woo_ai_translation/shadow_diff'
require_relative 'woo_ai_translation/cli'

# Self-contained, cross-platform-extractable AI translation engine that replaces
# the GlotPress download path. See fastlane/ai_translation/README.md.
module WooAiTranslation
end
