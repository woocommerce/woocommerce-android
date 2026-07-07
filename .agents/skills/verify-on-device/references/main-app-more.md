# More Menu Tab

Fragment: `MoreMenuFragment` -- Tap `moreMenu` bottom tab.
Fully Compose-based. Identify items by text labels.

## Menu Items

- "Payments" -> Payments Hub
- "Reviews" -> Reviews List
- "Coupons" -> Coupon List
- "Customers" -> Customer List
- "Blaze" -> Blaze Campaign List (if enabled)
- "Google for WooCommerce" -> Google Ads (if enabled)
- "Subscriptions" -> Plan Upgrades (if eligible)
- "Inbox" -> Inbox messages
- "Settings" -> Settings activity

## Screen Identifiers

**Reviews List** -- Fragment: `ReviewListFragment` (Compose-based via `ReviewListScreen`)

| Key Element | Identifier | Notes |
|-------------|-----------|-------|
| **Primary** | text: "Unread reviews only" | Compose screen, use this text to confirm you're on the Reviews list |

**Payments Hub** -- Fragment: `PaymentsHubFragment`

| Key Element | Identifier | Notes |
|-------------|-----------|-------|
| **Primary** | `paymentsHubRv` | Payments options RecyclerView |
| Loading indicator | `paymentsHubLoading` | LinearProgressIndicator |

**Coupon List** -- Fragment: `CouponListFragment`

| Key Element | Identifier | Notes |
|-------------|-----------|-------|
| **Primary** | `couponsComposeView` | ComposeView hosting coupon list |
| Add coupon FAB | `add_coupon_button` | Floating action button |

**Settings** -- separate Activity (`AppSettingsActivity`). Items by text labels.

**Review Detail** -- Fragment: `ReviewDetailFragment`

Identify by review content, rating stars, and moderation buttons (Approve, Spam, Trash).

## Workflows

### Payments Hub

| Step | Action | Element |
|------|--------|---------|
| 1 | Tap "Menu" tab | id: `moreMenu` |
| 2 | Tap "Payments" | text: "Payments" |
| 3 | Card reader setup | text: card reader options |
| 4 | Tap to Pay | text: "Tap to Pay" |
| 5 | Toggle Cash on Delivery | CoD toggle |

### Reviews

| Step | Action | Element                    |
|------|--------|----------------------------|
| 1 | Tap "Reviews" in menu | text: "Reviews"            |
| 2 | Toggle unread filter | text: "Unread reviews only" |
| 3 | Tap a review | row in the list |
| 4 | Approve review | approve button on detail   |
| 5 | Spam/Trash review | spam or trash button       |
| 6 | Mark all read | toolbar menu option        |

### Coupons

| Step | Action | Element |
|------|--------|---------|
| 1 | Tap "Coupons" in menu | text: "Coupons" |
| 2 | Tap a coupon | coupon row |
| 3 | Create coupon | FAB: `add_coupon_button` |
| 4 | Edit coupon details | coupon detail fields |
| 5 | Save coupon | save button |
| 6 | Delete coupon | delete option |

### Customers

| Step | Action | Element |
|------|--------|---------|
| 1 | Tap "Customers" in menu | text: "Customers" |
| 2 | Tap a customer | customer row |
| 3 | View customer details | customer detail screen |

### Blaze Campaigns

| Step | Action | Element |
|------|--------|---------|
| 1 | Tap "Blaze" in menu | text: "Blaze" |
| 2 | View campaign list | campaign rows |
| 3 | Create campaign | create button |
| 4 | Edit ad creative | ad editing screen |
| 5 | Set targeting | location/language/device/interests |
| 6 | Select payment method | payment methods list |
| 7 | Submit campaign | submit button |

### Google for WooCommerce

| Step | Action | Element |
|------|--------|---------|
| 1 | Tap "Google for WooCommerce" in menu | text: "Google for WooCommerce" |
| 2 | WebView opens | Google Ads dashboard or creation flow |
| 3 | Campaign creation success | success bottom sheet |

### Settings

| Screen | Nav Path |
|--------|----------|
| Main Settings | Menu -> "Settings" |
| Privacy Settings | Settings -> "Privacy settings" |
| Beta Features | Settings -> "Experimental features" |
| Developer Options | Settings -> "Developer options" (debug only) |
| Plugins | Settings -> "Plugins" |
| About | Settings -> "About" |
| Logout | Settings -> "Log out" button |
| Domain Management | Settings -> "Domains" |
| Help & Support | Settings -> "Help & Support" |
| Image Optimization | Settings -> toggle |
