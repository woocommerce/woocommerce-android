# frozen_string_literal: true

module WooAiTranslation
  VERSION = '1.0.0'

  # Bumping PROMPT_VERSION or a model below changes every cache key, which is an
  # explicit, reviewable re-translation trigger (never silent). Record both in
  # translation-manifest.json so a model/prompt bump is auditable.
  PROMPT_VERSION = '2026-05-19.1'

  ANTHROPIC_VERSION = '2023-06-01'

  # Pinned model versions. Sonnet handles the bulk of context-rich UI strings at
  # the best quality/cost balance; Opus is escalated for marketing/store metadata
  # and validator-flagged low-confidence strings. Haiku is intentionally not used
  # for production copy. Overridable via the manifest or CLI.
  DEFAULT_MODEL = 'claude-sonnet-4-6'
  ESCALATION_MODEL = 'claude-opus-4-7'

  # Resource names matching these are marketing/store copy -> escalate to Opus.
  ESCALATION_NAME_PATTERNS = [
    /\Aplay_store_/,
    /\Aapp_store_/,
    /_promo(_|\z)/,
    /\Akeywords\z/,
    /\Asubtitle\z/,
    /\Atitle\z/
  ].freeze

  # Hard Play Store character caps (mirrors fastlane download_metadata_strings).
  # full_description has no hard Play cap (0 == unbounded here).
  PLAY_METADATA_CAPS = {
    'title.txt' => 50,
    'short_description.txt' => 80,
    'full_description.txt' => 0,
    'changelogs/default.txt' => 500
  }.freeze

  # Single source of truth for model selection, shared by the engine and the
  # baseline importer so reuse/delta decisions never diverge.
  def self.model_for(name)
    ESCALATION_NAME_PATTERNS.any? { |re| name =~ re } ? ESCALATION_MODEL : DEFAULT_MODEL
  end
end
