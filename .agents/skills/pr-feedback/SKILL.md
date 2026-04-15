---
name: pr-feedback
description: >
  Address PR reviewer feedback by evaluating comments, proposing fixes, and executing after user
  approval. Use when the user wants to handle review comments on an existing PR, address PR feedback,
  fix PR comments, respond to reviewer suggestions, or process code review feedback. Triggers on
  "address PR feedback", "handle review comments", "fix PR comments", "respond to PR review",
  "process reviewer feedback", or when given a PR number/URL with intent to address its comments.
allowed-tools: Bash, Read, Grep, Glob, Edit, Write
user-invocable: true
---

# Address PR Feedback

Fetch reviewer comments from a PR, evaluate each, present a plan, and after approval make fixes (one commit per comment) and post replies.

**Input:** PR number, URL, or omit to auto-detect from the current branch.

## Workflow

### 0. Ask About Device Verification

Ask: "Would you like to verify the app on device after applying fixes?" Store the answer for later.

### 1. Resolve PR and Ensure Local Branch

```bash
# Get PR metadata (use number if provided, otherwise detect from branch)
gh pr view [NUMBER] --json number,title,author,headRefName,baseRefName,url
gh repo view --json owner,name
```

Check out the head branch and pull latest if not already on it.

### 2. Fetch Review Comments

Use **GraphQL** for inline comment threads (required for `isResolved` status):

```bash
gh api graphql -f query='
  query($owner:String!, $repo:String!, $number:Int!) {
    repository(owner:$owner, name:$repo) {
      pullRequest(number:$number) {
        author { login }
        reviewThreads(first:100) {
          nodes {
            isResolved
            isOutdated
            path
            line
            comments(first:50) {
              nodes { id databaseId author { login } body url path line originalLine diffHunk createdAt }
            }
          }
        }
      }
    }
  }
' -F owner='OWNER' -F repo='REPO' -F number=NUMBER
```

Also fetch review-level body comments and general PR comments via REST:

```bash
gh api 'repos/OWNER/REPO/pulls/NUMBER/reviews' --jq '.[] | select(.body != "" and .body != null)'
gh api 'repos/OWNER/REPO/issues/NUMBER/comments' --paginate
```

### 3. Filter

Skip these:
- **Resolved threads** (`isResolved == true`)
- **Bots** (login contains `[bot]` or matches `wpmobilebot`, `codecov-commenter`, `dependabot`, `github-actions`)
- **PR author's own comments**
- **Empty bodies**
- **Pure approvals** (state `APPROVED` with only praise like "LGTM")

For threaded comments, evaluate the root comment. Use replies as context only.

If nothing remains, report "No actionable reviewer feedback found." and stop.

### 4. Read Code and Evaluate

For each comment:
1. Read the file at `path` around the referenced `line` (use `originalLine` + `diffHunk` for outdated comments)
2. Evaluate against project conventions (AGENTS.md) and code quality
3. Assign an action:
   - **FIX** — feedback is valid; draft the code change
   - **REPLY** — disagree or no code change needed; draft a 1-2 sentence reply
   - **SKIP** — already addressed or not actionable

### 5. Present Plan

```
## PR Feedback Plan — #<number>: <title>

N comment(s) to address.

---

### 1 of N
**Reviewer:** @username | **File:** `path/File.kt:42`
**Link:** <url>
> Original comment text

**Action:** FIX — Description of proposed change
```

Repeat for each comment. Omit **File:** for general comments. End with:

```
**Summary:** Fixing X. Replying to Y. Skipping Z.
```

### 6. Confirm

Wait for user to say "go" or request changes:
- "skip 2" / "change 3 to reply: text" / "change 1 to fix: description"
- Re-present if modified

### 7. Execute

**For each FIX (one commit per comment):**
1. Make the change (prefer `Edit` over `Write`)
2. `git add <specific-file>` — never `git add -A`
3. `git commit -m "<concise description>"` (under 100 chars, no Co-Authored-By, no AI mentions)
4. Reply to the comment:
   ```bash
   gh api --method POST 'repos/OWNER/REPO/pulls/NUMBER/comments/COMMENT_DB_ID/replies' \
     -f body='Fixed in <short-hash>'
   ```

**For each REPLY:**
```bash
# Inline comments
gh api --method POST 'repos/OWNER/REPO/pulls/NUMBER/comments/COMMENT_DB_ID/replies' -f body='TEXT'
# General comments
gh api --method POST 'repos/OWNER/REPO/issues/NUMBER/comments' -f body='TEXT'
```

Process fixes in file order to minimize conflicts.

### 8. Push and Finish

```bash
git push
```

If user opted for device verification in step 0, invoke the `verify-on-device` skill.

Print summary:

```
## Done

**Fixes:** X commits pushed to `branch-name`
- `hash` commit message

**Replies:** Y posted
**Skipped:** Z comments
```

## Reply Style

- Fixes: `Fixed in abc1234` — nothing more
- Disagreements: 1-2 direct sentences, no AI/automation mentions

## Edge Cases

- **Outdated comments** (`line` is null): use `originalLine` + `diffHunk` to locate current code; flag if significantly changed
- **Deleted files**: suggest REPLY explaining the file was removed
- **Already-replied threads**: SKIP if author replied and reviewer didn't follow up
- **General review comments**: include as REPLY-only items
- **>15 comments**: suggest batching
- **Conflicting comments on same lines**: group by file, flag conflicts
