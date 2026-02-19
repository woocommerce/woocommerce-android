---
name: review
description: Review code changes against project architecture, style, and conventions
allowed-tools: Bash, Read, Grep, Glob
user-invocable: true
---

# Review Changes

Review the current uncommitted or staged changes against the project's conventions and architecture rules.

## Steps

1. **Determine the diff to review.** Run `git diff` for unstaged changes or `git diff --cached` for staged changes. If there are no changes, check `git diff trunk...HEAD` for branch changes.

2. **Read the changed files** in full to understand context, not just the diff hunks.

3. **Check against each category below.** Only report actual issues found — do not report categories with no issues.

### Architecture
- ViewModels extend `ScopedViewModel` and do NOT import Android framework classes (`Context`, `View`, etc.)
- Compose screens live inside Fragments via `ComposeView` with `DisposeOnViewTreeLifecycleDestroyed`
- ViewModels use `@HiltViewModel` + `@Inject constructor` with `SavedStateHandle` as the last parameter
- Data flows through repositories — ViewModels never access Room or network directly
- Navigation uses `NavController` via XML nav graphs

### Kotlin Style
- Max 120 character line length (test names excepted)
- No wildcard imports
- No `FIXME` (use `TODO`)
- Comments should be rare — only when they explain business logic not clearly captured in the code
- Constants use `UPPER_SNAKE_CASE`
- Companion objects at the bottom of the class
- No `!!` force unwraps — use safe calls or `requireNotNull()`
- Prefer `val`, immutable collections, sealed classes, data classes

### Jetpack Compose
- `@Composable` functions returning Unit use PascalCase noun names
- `Modifier` is the first optional parameter, named `modifier`
- State hoisting: state up, events down via lambdas
- No ViewModel acquisition inside composables
- Content wrapped in a container (`Column`, `Row`, `Box`)
- `remember {}` around all `mutableStateOf` / `derivedStateOf`
- Only immutable types as parameters

### Testing
- Tests extend `BaseUnitTest`
- BDD naming: `` `given X, when Y, then Z` `` (backtick-wrapped)
- Body has `// GIVEN`, `// WHEN`, `// THEN` comment sections
- AssertJ assertions, mockito-kotlin mocks
- Compose tests use `waitUntil`, never `Thread.sleep`

### Analytics
- New events added to `AnalyticsEvent` enum
- Tracking uses injected `AnalyticsTrackerWrapper`, not the singleton
- Event names use `UPPER_SNAKE_CASE` with proper token suffixes

## Output Format

```
## Blockers
- [file:line] Description of blocking issue

## Suggestions
- [file:line] Description of improvement suggestion

## Positives
- Description of what was done well
```
