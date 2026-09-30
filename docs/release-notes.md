# Release notes from PR titles

## Developer workflow

Feature PRs use a title and an impact label instead of editing `RELEASE-NOTES.txt`. Choose exactly one:

- `impact: user-visible`: use a descriptive title, e.g. "Fix checkout getting stuck after a declined payment".
- `impact: internal`: exclude refactoring, tooling, analytics, and other internal changes from the notes.

Danger requires one impact label; an `[Internal]` title prefix alone does not exclude a PR. `Releases` PRs are exempt. Feature-flagged changes are excluded; describe their effect in the PR that enables them.

Include `[*****]` in the title when broader smoke testing is needed, and `[WEAR]` for Wear changes. No priority marker is added automatically. Keep internal testing requirements in the PR's test plan.

## Release workflow

`start_code_freeze` generates the version section before extracting the Play Store draft. It compares the preceding release's tag with the trunk commit being frozen; that tag must exist. Other version sections are preserved.

The toolkit uses `get_prs_between_tags` with [developer-release-notes.yml](../.github/developer-release-notes.yml) to exclude internal, release-process, and feature-flagged PRs. It keeps the generated Markdown, including headings, author attribution, contributors, and links, under the existing version heading. Full GitHub release changelogs are unaffected.

Release managers edit the extracted Play Store text, separate Wear notes, and run `complete_code_freeze` as usual. Translation uses the same files. The existing backmerge brings `RELEASE-NOTES.txt` to trunk for Mission Control/ReleasesV2 summaries.

Regenerate only before editorial review: generation replaces the version's draft. Release managers still add late-fix and hotfix notes in the release PR.
