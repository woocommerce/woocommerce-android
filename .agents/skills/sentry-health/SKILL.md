---
name: sentry-health
description: Perform a comprehensive Sentry health check for WooCommerce Android. Checks release health, crash trends, error alerts (ParseError), top crashes grouped by theme, and provides triage recommendations with Linear cross-referencing. Use when asked to "check sentry", "sentry health", "release health", "crash report", "error triage", "how are crashes looking", or any question about app stability.
user-invocable: true
allowed-tools: mcp__sentry__find_releases, mcp__sentry__search_issues, mcp__sentry__search_events, mcp__sentry__get_issue_details, mcp__sentry__get_issue_tag_values, mcp__sentry__analyze_issue_with_seer, mcp__context-a8c__context-a8c-load-provider, mcp__context-a8c__context-a8c-execute-tool
---

# Sentry Health Check

Perform a comprehensive health check of the WooCommerce Android app in Sentry.

**Sentry Configuration:**
- Organization: `a8c`
- Region URL: `https://us.sentry.io`
- Project: `woocommerce-android`

## Known Crash Families

These are persistent, high-impact crash patterns in the app. When you encounter them, group all
variants together rather than listing each one individually. This saves report space and gives a
clearer picture of impact.

- **SelectedSite exceptions** — The single biggest crash family. Includes `SelectedSiteResetException`,
  `SelectedSiteUninitializedException`, and `InvocationTargetException` wrapping SelectedSite errors.
  These occur when the user's selected WooCommerce store becomes invalid (logged out, site removed,
  race condition during startup). There are typically 10+ separate Sentry issues for variants of this;
  combine their user counts to show total impact.

- **Disk space / ENOSPC** — `IOException: No space left on device`, `SQLiteFullException`,
  `FileNotFoundException: ENOSPC`. These are environmental (user's device is full) and not
  code-actionable, but worth tracking for volume trends.

- **Startup ANRs** — Background ANRs in Hilt DI initialization (`DaggerWooCommerceRelease_HiltComponents`,
  `SwitchingProvider.get0`, various `<clinit>` methods). Common on low-end devices during cold start.
  Group these together as "DI startup ANRs."

## Steps

Maximize parallelism — launch all independent queries in the same turn to minimize wall-clock time.

### Batch 1: Launch all queries in parallel

Fire these 4 calls simultaneously, plus load the Linear provider:

1. **Releases** — `find_releases` for the project (returns latest releases)

2. **Fatal crashes (last 7 days):**
```
search_issues(organizationSlug='a8c', regionUrl='https://us.sentry.io',
  projectSlugOrId='woocommerce-android',
  naturalLanguageQuery='unresolved crashes and fatal errors in the last 7 days',
  limit=25)
```

3. **Top issues by frequency (last 7 days):**
```
search_issues(organizationSlug='a8c', regionUrl='https://us.sentry.io',
  projectSlugOrId='woocommerce-android',
  naturalLanguageQuery='unresolved issues sorted by frequency in the last 7 days',
  limit=25)
```

4. **ParseError issues (last 7 days):**
```
search_issues(organizationSlug='a8c', regionUrl='https://us.sentry.io',
  projectSlugOrId='woocommerce-android',
  naturalLanguageQuery='unresolved ParseError issues in the last 7 days',
  limit=10)
```

5. **Load Linear provider** (for cross-referencing later):
```
mcp__context-a8c__context-a8c-load-provider(provider='linear')
```

### Batch 2: Deep dives (after Batch 1 returns)

Once the initial results come back, identify the **top 3 non-ANR crashes by user count** from the
combined results of queries 2 and 3 and fetch `get_issue_details` for each. Also search Linear for
any existing triage issues related to the top crashes.

Run these in parallel:
- `get_issue_details` for each of the top 3 crashes
- Linear search for the top ParseError issue ID
- Linear search for SelectedSite-related triage issues

## Analysis Guide

### Release Health

From the `find_releases` results, identify the **production releases** (format:
`com.woocommerce.android@XX.Y+NNN` or `com.woocommerce.android@XX.Y.Z+NNN`).
Ignore standalone version numbers like `7.9`, `10.4-rc-1` — those are often from
older or unrelated release sources.

For the last 3-5 production releases, note:
- Version name and date
- New issues count (the `New Issues` field from the API)

**Assessment:** Compare new-issue counts across releases. A release with significantly more new
issues than its predecessors suggests a regression. RC releases with 0 new issues are healthy.
The crash-free session rate is not directly available from the API, so rely on new-issue trends
and the crash data from the other queries.

### Crash Themes

When processing the crash results, group issues into known families before listing them.
The goal is a summary like "SelectedSite family: ~1000 users across 10+ variants" rather than
10 separate line items.

**Noise filtering:** Skip issues with 1 user AND 1 event — these are one-off ANRs that add
clutter without actionable signal. Focus the report on issues with 3+ users or 10+ events.

**Grouping order:**
1. SelectedSite exceptions (combine all variants, sum users)
2. Disk space / ENOSPC (combine, note as environmental)
3. Startup ANRs / DI initialization (combine background ANRs in Hilt/Dagger)
4. Navigation errors (IllegalArgumentException with nav destinations)
5. Data/parsing errors (JSON, SQLite, date parsing)
6. OOM (OutOfMemoryError)
7. Everything else worth mentioning

### ParseError Analysis

For the top ParseError by event count, determine the likely cause:

- **Plugin type** — The JSON structure mismatch is in a field that a WooCommerce plugin could
  modify (e.g., `method_supports` returning an object instead of array, non-standard field types
  in billing/shipping). Clue: affects few users but generates many events (a few merchants with
  a bad plugin generating repeated errors).
  - If daily affected users < 40: Recommend archiving with a note
  - If daily affected users >= 40: Recommend creating a Linear issue

- **Our API type** — The error is in a core WooCommerce REST API field, or involves a type
  overflow (e.g., int overflow for `items_sold`), or is in FluxC parsing logic.
  - Recommend creating a Linear issue immediately

- **Unknown** — Recommend creating a Linear Triage issue

Before recommending a new Linear issue, check if one already exists by searching Linear for
the Sentry issue ID or error description.

### New Issues This Week

Look through the results for issues first seen recently (check `First seen` dates in the crash
and frequency results — no need for a separate query since Batch 1 already captured these).

Filter out noise (1 user, 1 event ANRs). Flag as potential regressions any new issue with:
- 3+ affected users, OR
- 5+ events, OR
- A pattern matching a known crash family (e.g., a new SelectedSite variant)

## Output Format

```
## Sentry Health Report - WooCommerce Android
**Date:** [today's date]
**Period:** Last 7 days

### Release Health
| Release | Date | New Issues | Notes |
|---------|------|------------|-------|
[Last 3-5 production releases]

**Assessment:** [OK / Warning / Critical] — [brief explanation]

### Crash Families
[Grouped summary — SelectedSite total, ENOSPC total, startup ANR total, then individual notable crashes]

### Top Crashes by User Impact
| # | Issue | Error | Users | Events | Theme | Action |
|---|-------|-------|-------|--------|-------|--------|
[Top 5-8 rows, after grouping families. Include link to Sentry issue.]

### ParseError Alerts
[Count of active ParseErrors, top error details, plugin vs API classification, recommended action]

### New Issues This Week
[Count of notable new issues (after filtering noise), any potential regressions]

### Recommendations
1. [Prioritized actions — most impactful first]
2. [Include Linear issue references where they exist]
3. [Include "create Linear issue" recommendations where needed]
```

### Assessment Labels
- **OK**: No significant new-issue spikes, no new high-impact crashes, known families stable
- **Warning**: New-issue count elevated vs prior release, or new crashes with moderate impact (20-100 users), or known family growing
- **Critical**: Major new-issue spike, or new crash affecting 100+ users, or regression confirmed
