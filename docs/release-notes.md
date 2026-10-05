# Release notes from PR titles

## Developer workflow

Feature PRs use a title and an impact label instead of editing `RELEASE-NOTES.txt`. Choose exactly one:

- `impact: user-visible`: use a descriptive title, e.g. "Fix checkout getting stuck after a declined payment".
- `impact: internal`: exclude refactoring, tooling, analytics, and other internal changes from the notes.

Danger requires one impact label; `Releases` PRs are exempt. Feature-flagged changes are excluded; describe their effect in the PR that enables them.

Also add these labels when applicable:

- `feature: android wear`: group Wear changes under **Android Wear** in the generated notes.
- `testing: release smoke test`: request smoke testing of the final APK, including for internal changes. Describe the required checks in the PR's test steps.

## Release workflow

`start_code_freeze` generates the version section before extracting the Play Store draft. It compares the preceding release's tag with the trunk commit being frozen; that tag must exist. Other version sections are preserved.

The toolkit uses `get_prs_between_tags` with [developer-release-notes.yml](../.github/developer-release-notes.yml) to exclude internal, release-process, and feature-flagged PRs. It keeps the generated Markdown, including headings, author attribution, contributors, and links, under the existing version heading. Full GitHub release changelogs are unaffected.

The same lane uses [release-testing.yml](../.github/release-testing.yml) to list PRs labeled for release smoke testing over the same commit range. This separate report includes internal and feature-flagged changes. Review the **Release smoke testing** annotation on the code-freeze build and complete the requested checks before release. The report is also printed in the lane output; it is not added to store notes.

Release managers edit the extracted Play Store text, use the **Android Wear** section for Wear notes, and run `complete_code_freeze` as usual. Translation uses the same files. The existing backmerge brings `RELEASE-NOTES.txt` to trunk for Mission Control/ReleasesV2 summaries.

Regenerate only before editorial review: generation replaces the version's draft. Release managers still add late-fix and hotfix notes and testing requirements in the release PR; the code-freeze testing report is a snapshot.
