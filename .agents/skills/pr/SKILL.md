---
name: pr
description: Create a pull request following project conventions. Triggers on any request to create, open, make, submit, file, send, push, spin up, put up, draft, raise, or prepare a PR/pull request.
allowed-tools: Bash, Read, Grep, Glob
user-invocable: true
---

# Create Pull Request

Create a pull request following the project's PR conventions.

@docs/pull-request-guidelines.md

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

8. **Create the PR.** Read `.github/PULL_REQUEST_TEMPLATE.md` and use it as the body. Strip the HTML comments and fill in each section, following the conventions from the guidelines doc above:
   - **Description**: start with `Fixes WOOMOB-XYZ` on its own line, then a short summary of what changed and why.
     - Keep it to what a reviewer needs: what was wrong, what this changes, and anything they can't see in the diff. A couple of sentences of plain prose is usually right — no section headers beyond the template's. If the PR fixes several distinct things, one line each is fine.
     - The conversation that produced the PR is not the PR. Don't retell the investigation — no debugging path, no narrative of how you got there, no measurements unless the measurement is the reason for the change.
     - Do pre-empt what a reviewer would actually raise: a decision they'd disagree with, or an obvious alternative they'd ask "why not X?" about. A sentence or a clause each, in prose. The bar is "they'd bring this up in review", not "this was interesting to figure out". Most PRs have nothing that clears it; if you end up with a list, you're padding.
     - Write so a reviewer who never saw this branch can follow it — carry the context, not the derivation.
     - References the reviewer can follow (a Linear issue, a linked Slack thread, a prior PR) are fine. What's not fine is referencing things the reviewer has no access to: the Claude session, findings-by-number from a private review ("HIGH-#2"), or severity labels from a one-off discussion. If you write "as discussed," make sure "discussed" is a link the reviewer can open.
   - **Test Steps**: numbered manual verification steps — a reviewer tapping through the app or reproducing a scenario. Do NOT include "run the unit tests" (or any `./gradlew test*` invocation) as a step — CI already runs them, so they add noise without helping the reviewer.
   - **Images/gif**: include if applicable, otherwise `N/A`.
   - Keep the release-notes checkbox line as-is.

   Pass the filled template via a HEREDOC:

   ```bash
   gh pr create --draft --title "[WOOMOB-XYZ] <concise title>" --body "$(cat <<'EOF'
   <filled template content here>
   EOF
   )"
   ```

9. **Add labels.** Add labels using `gh pr edit <number> --add-label "<label>"`. Infer labels from the diff and branch name using the categories in the guidelines. If unsure about feature label, ask the user.

10. **Set milestone.** Find the closest open milestone and assign it:
    - List open milestones (single-quote the URL to prevent shell `&` interpretation):
      ```
      gh api 'repos/{owner}/{repo}/milestones?state=open&sort=due_on&direction=asc'
      ```
    - Pick the one with the earliest `due_on` date that is still in the future
    - Assign it: `gh api repos/{owner}/{repo}/issues/{number} -X PATCH -F milestone={milestone_number}`
    - **If the milestone due date is less than 1 day away**, warn the user (e.g., "Heads up: milestone X closes in <N hours> — let me know if you'd prefer a different one.")

11. **Report the PR URL** to the user.

## Image Formatting in PR Body

- **Before/after pair**: Use a comparison table with `| Before | After |` headers
- **Multiple images**: Use a table with appropriate column headers
- **Single image or video**: Embed directly (e.g., `![Screenshot](url-or-path)`)
- Constrain image width to 400px using HTML: `<img src="url" width="400" />`
- If no images provided, use "N/A"

## Troubleshooting

If `gh pr edit` fails with a GraphQL `Projects (classic)` error, fall back to the REST API:
```bash
gh api repos/{owner}/{repo}/pulls/{number} -X PATCH -f body='...'
gh api repos/{owner}/{repo}/pulls/{number} -X PATCH -f title='...'
gh api repos/{owner}/{repo}/issues/{number}/labels -X POST --input - <<< '{"labels":["label1","label2"]}'
```
