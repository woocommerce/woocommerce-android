---
name: pr
description: Create a pull request following project conventions. Triggers on any request to create, open, make, submit, file, send, push, spin up, put up, draft, raise, or prepare a PR/pull request.
allowed-tools: Bash, Read, Grep, Glob
user-invocable: true
---

# Create Pull Request

Create a pull request following the project's PR conventions and template.

## PR Title Format

- **Always prefix with `[WOOMOB-XYZ]`** where XYZ is the issue number
- Extract issue number from branch name (format: `issue/woomob-XYZ-...` or `woomob-XYZ-...`)
- If issue number not available in branch name, ask the user for it

## PR Description Format

- **Always start with `Fixes WOOMOB-XYZ`** on the first line of the Description section

## Steps

**IMPORTANT: Follow ALL steps in order. Do not skip any step, even if the PR seems simple.**

1. **Verify branch.** Confirm you are NOT on `trunk`. If on trunk, stop and ask the user to create a feature branch first.

2. **Check for uncommitted changes.** Run `git status`. If there are uncommitted changes, stop and ask the user whether to commit them first.

3. **Analyze the diff.** Run `git diff trunk...HEAD` to understand ALL changes that will be in the PR. Also run `git log --oneline trunk..HEAD` to see all commits.

4. **Check diff size.** Count non-test lines changed. If the diff is very large, mention it to the user.

5. **Review changes.** Scan for:
   - Architecture compliance (MVVM layers, Hilt DI, Compose patterns)
   - Missing tests for new logic
   - Any `FIXME`, `!!`, wildcard imports, or other violations
   - Whether `RELEASE-NOTES.txt` needs updating (user-facing changes)

6. **Check RELEASE-NOTES.txt.** If changes are user-facing, remind the user to update `RELEASE-NOTES.txt`. Use `[Internal]` for non-user-facing changes.

7. **Push the branch.** Run `git push -u origin <branch-name>`.

8. **Create the PR.** Use the template format from `.github/PULL_REQUEST_TEMPLATE.md`:

```bash
gh pr create --draft --title "[WOOMOB-XYZ] <concise title>" --body "$(cat <<'EOF'
### Description
Fixes WOOMOB-XYZ
<1-3 sentence summary of what and why>

### Test Steps
<numbered steps to verify the changes>

### Images/gif
<if applicable, otherwise "N/A">

- [ ] I have considered if this change warrants release notes and have added them to `RELEASE-NOTES.txt` if necessary. Use the "[Internal]" label for non-user-facing changes.
EOF
)"
```

9. **Add labels.** Add labels using `gh pr edit <number> --add-label "<label>"`. Pick labels from these categories:
   - **Type** (pick one): `type: bug`, `type: crash`, `type: enhancement`, `type: task`, `type: technical debt`, `type: documentation`, `type: question`
   - **Feature** (pick one if applicable): match the changed area to a `feature: *` label (e.g., `feature: order list`, `feature: point of sale`, `feature: product details`, `feature: login`, etc.)
   - **Category** (pick any that apply): `category: accessibility`, `category: design`, `category: performance`, `category: tracks`, `category: unit tests`, `category: ui tests`, `category: tooling`, `category: parity`, etc.
   - If the feature is behind a flag, also add `status: feature-flagged`
   - Infer labels from the diff and branch name. If unsure about feature label, ask the user.

10. **Set milestone.** Find the closest open milestone and assign it to the PR:
    - List open milestones (single-quote the URL to prevent shell `&` interpretation):
      ```
      gh api 'repos/{owner}/{repo}/milestones?state=open&sort=due_on&direction=asc'
      ```
    - Pick the one with the earliest `due_on` date
    - Assign it: `gh api repos/{owner}/{repo}/issues/{number} -X PATCH -F milestone={milestone_number}`
    - **If the milestone due date is less than 1 day away**, warn the user after assigning (e.g., "Heads up: milestone X closes in <N hours> — let me know if you'd prefer a different one.")

11. **Report the PR URL** to the user.

## Troubleshooting

If `gh pr edit` fails with a GraphQL `Projects (classic)` error, fall back to the REST API:
```bash
gh api repos/{owner}/{repo}/pulls/{number} -X PATCH -f body='...'
gh api repos/{owner}/{repo}/pulls/{number} -X PATCH -f title='...'
gh api repos/{owner}/{repo}/issues/{number}/labels -X POST --input - <<< '{"labels":["label1","label2"]}'
```
