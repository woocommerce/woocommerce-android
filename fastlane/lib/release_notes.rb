# frozen_string_literal: true

require 'fastlane'
require 'fastlane/helper/sh_helper'

# Release-note comparisons and smoke-testing reports for code freeze.
module ReleaseNotes
  module_function

  def resolve_base_tag(previous_version:, new_version:)
    release_tags = remote_release_tags(version: previous_version)
    base_tag = if release_tags.include?(previous_version)
                 previous_version
               else
                 release_tags.find { |tag| tag.match?(/\A#{Regexp.escape(previous_version)}-rc-\d+\z/) }
               end
    FastlaneCore::UI.user_error!("No final or RC tag for #{previous_version} was found on origin. Cannot generate release notes for #{new_version}.") if base_tag.nil?
    FastlaneCore::UI.message("Using #{base_tag} as the base for release notes and the smoke-testing report.")
    if base_tag != previous_version
      FastlaneCore::UI.important("Release #{previous_version} is not tagged yet. Review the generated draft for fixes already shipped in that release before publishing the notes.")
    end
    base_tag
  end

  # Tags are returned in descending version order.
  def remote_release_tags(version:)
    Fastlane::Actions.sh(
      'git', 'ls-remote', '--tags', '--refs', '--sort=-version:refname', 'origin',
      "refs/tags/#{version}", "refs/tags/#{version}-rc-*", log: false
    ).lines.map { |line| line.split.last.delete_prefix('refs/tags/') }
  end

  def smoke_testing_message(release_testing)
    testing_details = if release_testing.to_s.match?(/^\* /)
                        "Review the test steps in any PRs listed below. Internal changes are included.\n\n#{release_testing}"
                      else
                        'No PRs currently labeled `testing: release smoke test` were found in this release range.'
                      end
    "## Release smoke testing\n\nSmoke test the final APK before release.\n\n#{testing_details}"
  end
end
