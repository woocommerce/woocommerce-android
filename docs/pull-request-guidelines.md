# Pull Request Guidelines

## Branch Naming

Feature or fix branch names should use the pattern `issue/ISSUEID-description` where the `ISSUEID` is the Linear issue identifier (e.g., `WOOMOB-1234`). For example, `issue/woomob-1234-order-list-error`.

If there is no Linear issue, you can use prefixes like `feature/` or `fix/`.

## Commits

Don't tag the issue number in individual commit messages — it pollutes the PR. Attach the issue number to the final pull request. Before submitting, make sure your branch is up to date with `trunk`.

## PR Title Format

Prefix with `[WOOMOB-XYZ]` where XYZ is the Linear issue number (extracted from branch name `issue/woomob-XYZ-...`):

```
[WOOMOB-1234] Add order refund confirmation dialog
```

## PR Description Format

Start the Description section with `Fixes WOOMOB-XYZ` on the first line.

## PR Template

The project uses a PR template (`.github/PULL_REQUEST_TEMPLATE.md`) — follow it for the structure and examples of a good PR. Key sections: Description, Test Steps, Images/gif, and RELEASE-NOTES.txt checkbox.

## Labels

A minimum of one label should be assigned, but we strongly encourage labels from two or more categories:

- **Type** (pick one): `type: bug`, `type: crash`, `type: enhancement`, `type: task`, `type: technical debt`, `type: documentation`, `type: question`
- **Feature** (pick one if applicable): match the changed area to a `feature: *` label (e.g., `feature: order list`, `feature: point of sale`, `feature: product details`, `feature: login`)
- **Category** (pick any): `category: accessibility`, `category: design`, `category: performance`, `category: tracks`, `category: unit tests`, `category: ui tests`, `category: tooling`, `category: parity`
- **Status**: `status: do not merge`, `status: on-hold`, `status: blocked`, `status: feature-flagged`
- **Priority**: `priority: low`, `priority: high`, `priority: critical`
