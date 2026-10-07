# frozen_string_literal: true

{
  repository: 'woocommerce/woocommerce-android',
  versioning: { platform: :android, scheme: :marketing, file: 'version.properties' },
  hotfix: { version_option: :version_name, base_policy: :tag_or_release_branch },
  milestones: { annotation_context: 'start-code-freeze' },
  build: {
    pipeline: 'woocommerce-android', pipeline_file: 'release-builds.yml',
    boolean_options: { include_wear_app: 'INCLUDE_WEAR_APP' }
  }
}
