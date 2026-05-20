# frozen_string_literal: true

module WooAiTranslation
  VERSION = '1.0.0'

  # Bumping PROMPT_VERSION or a model below changes every cache key, which is an
  # explicit, reviewable re-translation trigger (never silent). Record both in
  # translation-manifest.json so a model/prompt bump is auditable.
  PROMPT_VERSION = '2026-05-19.1'

  ANTHROPIC_VERSION = '2023-06-01'

  # Pinned model versions. Haiku is the default for UI strings: the Peacock P2
  # hack-week experiment ran a blind A/B over 16 locales × 300 strings and AI
  # translations from Haiku 4.5 (and GPT-5.4) were preferred over the existing
  # human translations 2-3x more often than the reverse, with backfill costs in
  # cents per locale -- using a stronger model for the bulk of strings is not
  # cost-effective. Opus is reserved for marketing / store-metadata copy and any
  # validator-flagged low-confidence retries. CI should override with a
  # date-pinned variant (e.g. claude-haiku-4-5-20251001) for full determinism.
  DEFAULT_MODEL = 'claude-haiku-4-5'
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
