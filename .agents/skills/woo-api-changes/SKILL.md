---
name: woo-api-changes
description: Check recent WooCommerce Core REST API changes from the #woo-core-rest-api-changes Slack channel and evaluate their impact on the Android app. Searches the codebase to verify whether affected endpoints are used. Use when asked to "check API changes", "woo api changes", "REST API updates", "what changed in woo core", or any question about whether recent WooCommerce backend changes affect the Android app.
user-invocable: true
allowed-tools: mcp__context-a8c__context-a8c-load-provider, mcp__context-a8c__context-a8c-execute-tool
---

# Woo API Changes Review

Review recent WooCommerce Core REST API changes announced in Slack and evaluate their
impact on the WooCommerce Android app.

**Slack Channel:** `#woo-core-rest-api-changes` (ID: `C060C3Q1ETS`)

## Why this matters

WooCommerce Core ships REST API changes that can break or affect the Android app. These
changes are announced in a Slack channel. Reviewing them promptly prevents surprises
when merchants update their WooCommerce version. The Android app consumes these APIs
through FluxC REST clients, so we need to check whether changed endpoints, fields, or
behaviors overlap with what the app uses.

## Steps

### 1. Load providers and fetch messages

Load the Slack and GitHub providers in parallel, then fetch recent messages from the channel.

**Important:** Always compute a Unix timestamp for 2 days ago and pass it as the `oldest`
parameter. This keeps the result set focused and avoids pulling in old, already-reviewed
messages. If the user specifies a different range, adjust accordingly.

```
mcp__context-a8c__context-a8c-load-provider(provider='slack')
mcp__context-a8c__context-a8c-load-provider(provider='github')

# Calculate 2-days-ago timestamp: current_epoch - (2 * 86400)
mcp__context-a8c__context-a8c-execute-tool(
  provider='slack',
  tool='messages',
  params={"channel": "C060C3Q1ETS", "limit": 50, "oldest": "<2-days-ago-unix-timestamp>"}
)
```

### 2. Filter messages to review

Analyze messages where the `:done:` reaction count is **0 or 1**. This captures messages
the current user likely hasn't reviewed yet (0 = no one reviewed, 1 = only one teammate did).
Skip messages with 2+ `:done:` reactions — those have been reviewed by multiple people.

The API returns reactions as `[{"name": "done", "count": N}]`. If a message has no
`reactions` field, treat the count as 0.

Skip thread replies — only process top-level messages (these are the bot announcements).
Thread replies are often team discussion; read them for context if needed during analysis.

### 3. Analyze unreviewed messages

Each message from the bot typically follows this format:
```
#PRNUMBER: PR title
Milestone: X.Y.Z
Changed files:
  path/to/file1.php
  path/to/file2.php
```

For each unreviewed message:

**a) Fetch the WooCommerce Core PR metadata.** Extract the PR number from the message
(e.g., `#63556`) and fetch it from the `woocommerce/woocommerce` GitHub repo:

```
mcp__context-a8c__context-a8c-execute-tool(
  provider='github',
  tool='pull-request',
  params={"owner": "woocommerce", "repo": "woocommerce", "pullNumber": 63556, "method": "get"}
)
```

This gives you the PR title, milestone, and description for context. However, do NOT rely
on the description alone to assess mobile impact — PR descriptions are written for the web
team and often omit mobile-relevant details. The diff (step b) is what matters most.

**b) Fetch and analyze the PR diff.** PR descriptions are often vague or focused on the
web admin context — they may not mention mobile impact at all. The diff is the ground truth.
Fetch it directly:

```
mcp__context-a8c__context-a8c-execute-tool(
  provider='github',
  tool='pull-request',
  params={"owner": "woocommerce", "repo": "woocommerce", "pullNumber": <PR_NUMBER>, "method": "diff"}
)
```

If `method: "diff"` is not available, fall back to fetching the PR files list:
```
mcp__context-a8c__context-a8c-execute-tool(
  provider='github',
  tool='pull-request-files',
  params={"owner": "woocommerce", "repo": "woocommerce", "pullNumber": <PR_NUMBER>}
)
```

**What to look for in the diff:**

1. **Response schema changes** — Look for additions/removals in arrays like
   `$data['field_name']`, `'field' => ...`, or `register_rest_field()` calls. These directly
   affect what the mobile app receives when calling the endpoint.

2. **Endpoint route changes** — Look for `register_rest_route()`, route path strings, or
   HTTP method changes. Removed or renamed routes break the app immediately.

3. **Query/filter logic changes** — Look for modifications to `get_items()`, `prepare_items_query()`,
   or SQL query construction. These change *which* data is returned (pagination, sort order,
   filtering), even if the schema stays the same.

4. **Validation changes** — Look for `validate_callback`, `sanitize_callback`, or parameter
   requirement changes. Stricter validation can cause previously-accepted requests to fail.

5. **Cache/header changes** — Look for `Cache-Control`, `ETag`, `Last-Modified` header additions.
   Caching on endpoints the app polls can cause stale data.

**Key file paths to watch for:**
  - `plugins/woocommerce/src/StoreApi/` — Store API (**not used by Android app** — POS uses `wc/pos/v1` instead)
  - `plugins/woocommerce/src/Internal/Orders/` — Orders REST API
  - `plugins/woocommerce/src/REST/` or `includes/rest-api/` — WC REST API v3 (primary API the app uses)
  - `plugins/woocommerce/src/Admin/API/` — Admin API endpoints
  - `plugins/woocommerce/src/Internal/Admin/Settings/` — Admin settings pages (mobile apps
    open some of these in webviews — e.g., payment onboarding flows)

**c) Search the Android codebase for usage.** Look in:
- `libs/fluxc/` — REST clients, network DTOs, stores (this is where API calls live)
- `WooCommerce/src/main/kotlin/.../model/` — domain models that map to API responses
- Search for the endpoint path, field names, or controller name

**Webview dependency check:** The Android app opens several WC admin pages in webviews
(not just REST API calls). When the diff shows changes to WC admin tasks, onboarding flows,
or admin page routing (look for `task` slugs, page paths, redirect logic, or removed/renamed
admin features), also grep the Android codebase for webview URLs:

```
# Search for wc-admin page references opened in webviews
grep -r "wc-admin" WooCommerce/src/main/kotlin/ --include="*.kt"
grep -r "wp-admin" WooCommerce/src/main/kotlin/ --include="*.kt"
```

Known webview entry points in the Android app:
- `GetPaidViewModel.kt` — Opens `/wp-admin/admin.php?page=wc-admin&task={taskId}` for
  payment onboarding. If a WC admin task is removed or renamed server-side, this breaks.
- `CardReaderOnboardingErrorCtaClickHandler.kt` — Opens `/admin.php?page=wc-admin&path=/payments/connect`
- `AppUrls.kt` — Opens Google Listings dashboard and other admin pages

If the diff removes, renames, or changes routing for any admin task or page that the app
opens in a webview, classify the impact as **High** — the app cannot detect the removal
and users will see a broken page or infinite spinner.

**d) Classify the impact:**
- **None** — The change is in an API the app doesn't use (e.g., Store API blocks, admin-only endpoints)
- **Low** — The change is in an API the app uses but is additive (new optional field, new endpoint). No breakage expected.
- **Medium** — The change modifies behavior or fields the app reads. The app will still work but may show stale/wrong data or miss new functionality.
- **High** — The change removes or renames fields/endpoints the app depends on. Could cause crashes or broken features.

**e) Assign a confidence level** to your impact assessment. This helps the reviewer
know how much to trust the analysis vs. doing their own investigation:
- **High confidence** — You found (or confirmed the absence of) direct usage in the codebase.
  The PR description is clear about what changed. You're sure about the assessment.
- **Medium confidence** — The PR touches an area the app uses, but the exact impact is
  ambiguous (e.g., behavioral change that might or might not affect the app's usage pattern).
  Or the codebase search was inconclusive (e.g., the endpoint is used but through a generic
  path that's hard to trace).
- **Low confidence** — The PR description is vague, the changed files don't clearly indicate
  the API surface affected, or the codebase search couldn't determine usage. The reviewer
  should investigate manually.

**f) If there's a thread on the Slack message**, read it — teammates may have already analyzed the impact.

### 4. Present the summary

## Output Format

```
## Woo API Changes Review
**Date:** [today's date]
**Period:** Last 2 days
**Channel:** #woo-core-rest-api-changes

### Unreviewed Changes: [count]

#### [PR #NUMBER]: [Title]
- **Milestone:** [version]
- **Changed files:** [list]
- **PR:** [link to WooCommerce GitHub PR]
- **What changed:** [brief description of the API change, informed by PR description]
- **Android impact:** [None / Low / Medium / High]
- **Confidence:** [High / Medium / Low]
- **Details:** [Why this does or doesn't affect the app. If it does, which FluxC client/store/model is affected.]
- **Action needed:** [None / Monitor / Update model / Fix required]
- **:done:?** [Yes — safe to mark reviewed / No — needs follow-up first]

[Repeat for each unreviewed message]

### Skipped: [count] messages with 2+ :done: reactions

### Summary
- Total messages checked: N
- Reviewed (0-1 :done:): N
- Skipped (2+ :done:): N
- Impact breakdown: X None, Y Low, Z Medium, W High
- Ready for :done:: [list messages that are safe to mark as reviewed]
- Action items: [list any Medium/High items that need follow-up before marking :done:]
```

### Impact classification guide

When searching the codebase, these are the most common patterns to look for:

- **Order endpoints** (`/wc/v3/orders`) → `OrderRestClient`, `OrderStore`, `OrderMapper`
- **Product endpoints** (`/wc/v3/products`) → `ProductRestClient`, `WCProductStore`
- **Payment gateways** (`/wc/v3/payment_gateways`) → `WCGatewayStore`
- **Shipping** (`/wc/v3/shipping`) → `ShippingLabelRestClient`
- **Customers** (`/wc/v3/customers`) → `CustomerRestClient`
- **Reports/stats** (`/wc-analytics/`) → `WCStatsStore`, stats-related clients
- **Settings** (`/wc/v3/settings`) → `WooCommerceStore.fetchSiteSettings`
- **POS API** (`/wc/pos/v1/`) → `WooPosProductRestClient`, POS-related code in `ui/woopos/`
- **Store API** (`/wc/store/v1/`) → **Not used by the Android app.** Changes here have no impact.
