# Release notes from PR titles

## Developer workflow

Do not edit `RELEASE-NOTES.txt` in feature PRs. Before review, select exactly one label:

- `impact: user-visible`: the PR title describes the change for the release notes, for example, "Fix checkout getting stuck after a declined payment".
- `impact: internal`: refactoring, tooling, analytics, or other changes that do not belong in user-facing notes.

Danger checks the classification. Titles still need human review. An `[Internal]` title prefix alone does not exclude a PR. Release-process PRs (`Releases`) are exempt; feature-flagged changes are excluded until a follow-up PR enables them and describes their user-visible effect.

Entries default to `[*]`. If a user-visible change needs broader smoke testing, start the title with e.g. `[*****]`. Include `[WEAR]` for Wear changes. Internal testing requirements remain in the PR's test plan; internal entries are no longer part of this file.

## Release workflow

The existing `start_code_freeze` lane generates the new version's section before extracting the draft Play Store notes. It compares the preceding release version (read before the version bump) with the published trunk commit used for the freeze. The preceding version's tag must exist; generation fails if it does not, rather than silently using an older release or a beta/Wear tag.

The toolkit calls `get_prs_between_tags`, using `.github/developer-release-notes.yml` for exclusions, then writes `- [*] Title [PR URL]` entries. The dedicated configuration leaves full GitHub release changelogs unchanged. Historical version sections are preserved. No empty next-version section is needed now that feature PRs no longer append entries.

Release managers still review and rewrite the extracted Play Store text, separate Wear notes, and run `complete_code_freeze` as before. Translation and publication use the same files. The existing backmerge brings `RELEASE-NOTES.txt` to trunk for Mission Control/ReleasesV2 summaries. There is no new GitHub workflow.

Generation replaces that version's draft, so regenerate only before editorial review. Late fixes or hotfix notes after that point still need release-manager edits in the release PR; this proposal automates the regular code-freeze draft, not every release step.

## Trying the proposal

With GitHub credentials configured as for existing release lanes, preview a known range without modifying files or creating a release:

```sh
bundle exec fastlane run generate_release_notes_file \
  repository:woocommerce/woocommerce-android \
  version:25.8 tag_name:ainfra-3084-preview previous_tag:25.6 \
  target_commitish:iangmaia/generate-release-notes-from-prs \
  configuration_file_path:.github/developer-release-notes.yml \
  release_notes_file_path:RELEASE-NOTES.txt dry_run:true
```

This preview uses a wider historical range to work before the preceding release is published. For a real freeze, use the preceding version's tag and the trunk commit being frozen. The target must be published and contain the configuration file. If `tag_name` exists, GitHub uses that tag instead of `target_commitish`; the preview tag name above is not created. Review the generated titles against the current handwritten draft before adopting the change.

Before merging this proposal, create the two impact labels, classify PRs already merged for the first generated release, and replace the temporary toolkit Git revision with a released gem version. Existing open PRs must drop their handwritten release-note entries. Decide with QA whether title-based priority markers and internal test plans are sufficient for the pilot.
