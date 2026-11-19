# frozen_string_literal: true

require 'minitest/autorun'
require 'fileutils'
require 'tmpdir'
require 'open3'
require_relative '../lib/release_notes'

# Exercises the helpers with local Git repositories and GitHub report bodies.
class ReleaseNotesTest < Minitest::Test
  def setup
    @directory = Dir.mktmpdir('release-notes-test-')
    @previous_directory = Dir.pwd
    Dir.chdir(@directory)
    git('init', '--bare', 'origin.git')
    git('init', 'checkout')
    Dir.chdir('checkout')
    git('remote', 'add', 'origin', File.join(@directory, 'origin.git'))
    git('commit', '--allow-empty', '-m', 'Initial commit')
  end

  def teardown
    Dir.chdir(@previous_directory)
    FileUtils.remove_entry(@directory)
  end

  def test_prefers_final_tag_over_rc_tags
    publish_tags('25.8', '25.8-rc-2', '25.8-rc-10')

    assert_equal '25.8', resolve_base_tag
  end

  def test_selects_highest_numbered_rc_of_the_previous_version
    publish_tags('25.8-rc-2', '25.8-rc-10', '25.8-rc-9', '25.8-rc-99w', '25.8-rc-100-extra', '26.0-rc-100')

    assert_equal '25.8-rc-10', resolve_base_tag
  end

  def test_accepts_annotated_rc_tags
    publish_tags('25.8-rc-2')
    git('tag', '-a', '25.8-rc-12', '-m', 'Release candidate')
    git('push', 'origin', '25.8-rc-12')

    assert_equal '25.8-rc-12', resolve_base_tag
  end

  def test_ignores_local_tags_not_on_origin
    publish_tags('25.8-rc-2')
    git('tag', '25.8')
    git('tag', '25.8-rc-99')

    assert_equal '25.8-rc-2', resolve_base_tag
  end

  def test_rejects_wear_and_other_version_tags
    publish_tags('25.7', '25.7-rc-99', '25.8w', '25.8-rc-10w', '25.8-rc-11-extra', '25.8-beta-1', '26.0-rc-1')

    assert_raises(FastlaneCore::Interface::FastlaneError) { resolve_base_tag }
  end

  def test_fails_when_origin_has_no_release_tags
    error = assert_raises(FastlaneCore::Interface::FastlaneError) { resolve_base_tag }

    assert_includes error.message, 'No final or RC tag for 25.8 was found on origin.'
  end

  def test_propagates_remote_lookup_failure
    git('remote', 'set-url', 'origin', File.join(@directory, 'missing.git'))

    assert_raises(FastlaneCore::Interface::FastlaneShellError) { resolve_base_tag }
  end

  def test_reports_no_prs_when_github_returns_only_boilerplate
    report = File.read(File.join(__dir__, 'fixtures', 'empty_release_testing.md'))
    message = ReleaseNotes.smoke_testing_message(report)

    assert_includes message, 'No PRs currently labeled `testing: release smoke test` were found in this release range.'
    assert_includes message, 'Smoke test the final APK before release.'
    refute_includes message, 'Review the test steps'
  end

  def test_preserves_the_report_when_it_contains_prs
    report = "## Release smoke testing\n* Test checkout by @developer in https://github.com/woocommerce/woocommerce-android/pull/123\n"
    message = ReleaseNotes.smoke_testing_message(report)

    assert_includes message, 'Review the test steps in any PRs listed below. Internal changes are included.'
    assert message.end_with?(report)
    refute_includes message, 'No PRs currently labeled'
  end

  def test_accepts_blank_reports
    [nil, '', " \n"].each do |report|
      assert_includes ReleaseNotes.smoke_testing_message(report), 'No PRs currently labeled'
    end
  end

  private

  def resolve_base_tag
    ReleaseNotes.resolve_base_tag(previous_version: '25.8', new_version: '25.9')
  end

  def publish_tags(*tags)
    tags.each { |tag| git('tag', tag) }
    git('push', 'origin', '--tags')
  end

  def git(*)
    output, status = Open3.capture2e(
      'git', '-c', 'user.name=Release test', '-c', 'user.email=test@example.invalid',
      '-c', 'commit.gpgsign=false', '-c', 'tag.gpgsign=false', '-c', 'core.hooksPath=/dev/null', *
    )
    raise output unless status.success?

    output
  end
end
