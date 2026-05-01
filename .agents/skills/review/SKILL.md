---
name: review
description: Review code changes against project architecture, style, and conventions
allowed-tools: Bash, Read, Grep, Glob
user-invocable: true
---

# Review Changes

Review the current changes against the project's conventions and architecture rules.

Load the appropriate docs based on which code area is being reviewed:
- Store code: `@docs/store-compose.md`, `@docs/store-viewmodel-patterns.md`, `@docs/store-tracking-events.md`, `@docs/store-testing.md`
- POS code: `@docs/pos-architecture.md`, `@docs/pos-tracking-events.md`, `@docs/pos-testing.md`
- Shared: `@docs/coding-style.md`

## Steps

1. **Determine the diff to review.** First check `git diff trunk...HEAD` for branch changes (most common: pre-PR review). If empty, fall back to `git diff --cached` for staged changes, then `git diff` for unstaged changes.

2. **Determine if the code is POS or store.** POS: `ui/woopos/`, `WooPos*` prefix. Everything else: store management. Read the relevant docs above.

3. **Read the changed files** in full to understand context, not just the diff hunks.

4. **Check against each category below.** Only report actual issues found — do not report categories with no issues.

### Architecture
- POS: ViewModels extend `ViewModel()`, use parent-child SharedFlow event bus, pure Compose, Compose Navigation
- Store: ViewModels extend `ScopedViewModel`, use `triggerEvent()` / `MultiLiveEvent`, Compose inside Fragments, XML nav graphs
- Both: `@HiltViewModel` + `@Inject constructor`, data flows through repositories, ViewModels never access Room/network directly

### Kotlin Style
- Max 120 character line length (test names excepted)
- No wildcard imports, no `FIXME` (use `TODO`), no `!!` force unwraps
- Constants: `UPPER_SNAKE_CASE`, companion objects at bottom

### Jetpack Compose
- Check against patterns in `docs/store-compose.md` (store) or `docs/pos-architecture.md` (POS)

### Testing
- Store: `BaseUnitTest`, `testBlocking`, `captureValues`, AssertJ
- POS: `WooPosCoroutineTestRule`, `runTest`, `advanceUntilIdle`, AssertJ

### Analytics
- Store: `AnalyticsEvent` enum, `AnalyticsTrackerWrapper`
- POS: `WooPosAnalyticsEvent` sealed class, `WooPosAnalyticsTracker`

## Output Format

```
## Blockers
- [file:line] Description of blocking issue

## Suggestions
- [file:line] Description of improvement suggestion

## Positives
- Description of what was done well
```
