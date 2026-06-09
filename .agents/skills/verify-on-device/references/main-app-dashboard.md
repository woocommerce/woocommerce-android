# Dashboard Tab

Fragment: `DashboardFragment` -- Tap `dashboard` bottom tab.

## Screen Identifiers

**Dashboard (My Store)**

| Key Element | Identifier | Notes |
|-------------|-----------|-------|
| **Primary** | `dashboard_container` | ComposeView hosting dynamic dashboard cards |
| Stats card | tag: `dashboard_stats_card` | Revenue/visitors/orders stats |
| Top performers | tag: `dashboard_top_performers_card` | Best-selling products |
| Date range dropdown | tag: `stats_range_dropdown_button` | Today, Week, Month, Year, Custom |
| JITM container | `jitmFragment` | Just-In-Time promotional messages |

**Analytics Hub** -- Fragment: `AnalyticsHubFragment` -- Dashboard -> "View all store analytics"

| Key Element | Identifier | Notes |
|-------------|-----------|-------|
| **Primary** | `analyticsRefreshLayout` | SwipeRefreshLayout |
| Date range selector | `analyticsDateSelectorCard` | Date range picker card |
| Analytics cards | `cards` | RecyclerView with metric cards |

## Dashboard Cards

The dashboard uses a dynamic widget system. Cards can be added, removed, and reordered by the user.

| Card | Description |
|------|-------------|
| Stats | Revenue, orders, visitors with date range |
| Top Performers | Best-selling products |
| Orders | Recent orders summary |
| Reviews | Recent product reviews |
| Product Stock | Low stock alerts |
| Coupons | Active coupons |
| Blaze Campaigns | Active Blaze campaigns |
| Google Ads | Google Ads overview |
| Inbox | Notification messages |
| Store Onboarding | Setup checklist (new stores) |
| Push Notifications | Enable notifications prompt |

## Workflows

### Dashboard Navigation

| Step | Action | Element |
|------|--------|---------|
| 1 | Tap "My Store" tab | id: `dashboard` |
| 2 | Pull to refresh | swipe down on dashboard |
| 3 | Change date range | tag: `stats_range_dropdown_button` |
| 4 | Set custom date range | date picker dialog |
| 5 | View analytics hub | text: "View all store analytics" |

### Analytics Hub

| Step | Action | Element |
|------|--------|---------|
| 1 | Navigate from Dashboard | text: "View all store analytics" |
| 2 | Change date range | `analyticsDateSelectorCard` |
| 3 | Pull to refresh | swipe down |
| 4 | Open settings | settings icon |
| 5 | View full report | report link on card |

### Widget Editor (Customize Dashboard)

| Step | Action | Element |
|------|--------|---------|
| 1 | Tap "Edit layout" on dashboard | edit button |
| 2 | Toggle cards on/off | card toggle switches |
| 3 | Reorder cards | drag handles |
| 4 | Tap "Save" | save button |

### Store Onboarding (New Stores)

| Step | Action | Element |
|------|--------|---------|
| 1 | Tap onboarding task | task item in onboarding card |
| 2 | "About your store" | about store screen |
| 3 | "Name your store" | name dialog |
| 4 | "Launch your store" | launch store screen |
| 5 | Payments setup | payments pre-setup screen |

### Inbox

| Step | Action | Element |
|------|--------|---------|
| 1 | Tap inbox card or navigate from menu | inbox card / menu item |
| 2 | View message | message row |
| 3 | Take action (archive, view, etc.) | action button on message |
