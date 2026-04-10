# Pull Request Guidelines

## Branch Naming

Feature or fix branch names should use the pattern `issue/ISSUEID-description` where the `ISSUEID` is the GitHub issue number. For example, `issue/1000-order-list-error`.

If there is no GitHub issue, you can use prefixes like `feature/` or `fix/`.

> NOTE: Features are typically large PRs that contain smaller PRs that build a feature and are labeled as an `epic`.

## Commits

Don't tag the issue number in individual commit messages — it pollutes the PR. Attach the issue number to the final pull request. Before submitting, make sure your branch is up to date with `trunk`.

## PR Title Format

Prefix with `[WOOMOB-XYZ]` where XYZ is the issue number (extracted from branch name `issue/woomob-XYZ-...`):

```
[WOOMOB-1234] Add order refund confirmation dialog
```

## PR Description Format

Start the Description section with `Fixes WOOMOB-XYZ` on the first line.

## Anatomy of a Good Pull Request

Spend time crafting a good PR — it impacts reviewers, release managers, and testers.

**Title:** A good descriptive title, prefixed with `[WOOMOB-XYZ]`.

**Issue:** Link to the GitHub/Linear issue this PR addresses.

**Description:** Take the time to write a good summary. Why is it needed? What does it do? When fixing bugs, clarify what the problem was and how you fixed it — avoid just writing "See original issue."

**Testing instructions:** Step-by-step testing instructions. Break out individual scenarios when necessary, and consider including a checklist.

**Images and gif:** Include before/after images or gifs when appropriate.
- **Before/after pair**: Use a comparison table with `| Before | After |` headers
- **Multiple images**: Use a table with appropriate column headers
- **Single image or video**: Embed directly
- Constrain image width to 400px using HTML: `<img src="url" width="400" />`

**RELEASE-NOTES.txt:** Check whether changes are user-facing. If so, update `RELEASE-NOTES.txt`. Use `[Internal]` for non-user-facing changes.

### PR Template

The project uses a PR template (`.github/PULL_REQUEST_TEMPLATE.md`) with these sections:
- Description
- Test Steps
- Images/gif
- Release notes checkbox: `[ ] I have considered if this change warrants release notes and have added them to RELEASE-NOTES.txt if necessary.`

## Labels

A minimum of one label should be assigned, but we strongly encourage labels from two or more categories:

- **Type** (pick one): `type: bug`, `type: crash`, `type: enhancement`, `type: task`, `type: technical debt`, `type: documentation`, `type: question`
- **Feature** (pick one if applicable): match the changed area to a `feature: *` label (e.g., `feature: order list`, `feature: point of sale`, `feature: product details`, `feature: login`)
- **Category** (pick any): `category: accessibility`, `category: design`, `category: performance`, `category: tracks`, `category: unit tests`, `category: ui tests`, `category: tooling`, `category: parity`
- **Status**: `status: do not merge`, `status: on-hold`, `status: blocked`, `status: feature-flagged`
- **Priority**: `priority: low`, `priority: high`, `priority: critical`

## Milestones

Assign the closest open milestone with a future `due_on` date. If the milestone due date is less than 1 day away, consider whether a later one is more appropriate.
