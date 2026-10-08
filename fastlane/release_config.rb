# frozen_string_literal: true

{
  repository: 'woocommerce/woocommerce-android',
  versioning: { platform: :android, scheme: :marketing, file: 'version.properties' },
  hotfix: { version_option: :version_name, base_policy: :tag_or_release_branch },
  code_freeze: { branch_protection: :copy_default },
  milestones: { annotation_context: 'start-code-freeze' },
  build: {
    pipeline: 'woocommerce-android', pipeline_file: 'release-builds.yml',
    boolean_options: { include_wear_app: 'INCLUDE_WEAR_APP' }
  },
  publication: { branch_protection: :remove, optional_targets: { wear: { suffix: 'w', option: :include_wear_app } } },
  preparation: {
    freeze_after_version: :prepare_freeze_notes,
    freeze_completion: :prepare_code_freeze, finalization_before_version: :prepare_final_configuration
  },
  check_toolkit_updates: true
}
