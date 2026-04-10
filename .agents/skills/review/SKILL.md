---
name: review
description: Review code changes against project architecture, style, and conventions
allowed-tools: Bash, Read, Grep, Glob
user-invocable: true
---

# Review Changes

Review the current changes against the project's conventions and architecture rules.

@docs/compose.md
@docs/viewmodel-patterns.md
@docs/tracking-events.md
@docs/testing.md
@docs/coding-style.md

## Steps

1. **Determine the diff to review.** First check `git diff trunk...HEAD` for branch changes (most common: pre-PR review). If empty, fall back to `git diff --cached` for staged changes, then `git diff` for unstaged changes.

2. **Read the changed files** in full to understand context, not just the diff hunks.

3. **Check against each category below.** Only report actual issues found — do not report categories with no issues.

### Architecture
- Determine if the code is POS (`ui/woopos/`, `WooPos*` prefix) or store management (everything else)
- POS: ViewModels extend `ViewModel()`, use parent-child SharedFlow event bus, pure Compose, Compose Navigation
- Store: ViewModels extend `ScopedViewModel`, use `triggerEvent()` / `MultiLiveEvent`, Compose inside Fragments, XML nav graphs
- Both: `@HiltViewModel` + `@Inject constructor`, data flows through repositories, ViewModels never access Room/network directly

### Kotlin Style
- Refer to `docs/coding-style.md` for detekt rules and conventions
- Max 120 character line length (test names excepted)
- No wildcard imports, no `FIXME` (use `TODO`), no `!!` force unwraps
- Constants: `UPPER_SNAKE_CASE`, companion objects at bottom

### Jetpack Compose
- Refer to `docs/compose.md` (store) or `docs/pos-architecture.md` (POS) for patterns
- `@Composable` functions returning Unit use PascalCase noun names
- `Modifier` is the first optional parameter, named `modifier`
- State hoisting, containers, `remember {}`, immutable params

### Testing
- Refer to `docs/testing.md` for conventions
- Store: `BaseUnitTest`, `testBlocking`, `captureValues`, AssertJ
- POS: `WooPosCoroutineTestRule`, `runTest`, `advanceUntilIdle`, AssertJ

### Analytics
- Refer to `docs/tracking-events.md` for conventions
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
