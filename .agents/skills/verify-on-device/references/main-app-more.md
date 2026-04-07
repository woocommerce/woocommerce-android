# More Menu Tab

Fragment: `MoreMenuFragment` -- Tap `moreMenu` bottom tab.
Fully Compose-based. Identify items by text labels.
Logcat events are prefixed with `woocommerceandroid_`.

## Menu Items

- "Payments" -> Payments Hub
- "Reviews" -> Reviews List
- "Coupons" -> Coupon List
- "Customers" -> Customer List
- "Blaze" -> Blaze Campaign List (if enabled)
- "Google for WooCommerce" -> Google Ads (if enabled)
- "Subscriptions" -> Plan Upgrades (if eligible)
- "Bookings" -> Booking List (if extension active, larger screens)
- "Inbox" -> Inbox messages
- "Settings" -> Settings activity

## Screen Identifiers

**Reviews List** -- Fragment: `ReviewListFragment`

| Key Element | Identifier | Notes |
|-------------|-----------|-------|
| **Primary** | `reviewsList` | Reviews RecyclerView |
| Unread filter | `unread_filter_switch` | Toggle to filter unread |

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

| Step | Action | Element | Logcat Event |
|------|--------|---------|-------------|
| 1 | Tap "Menu" tab | id: `moreMenu` | |
| 2 | Tap "Payments" | text: "Payments" | |
| 3 | Card reader setup | text: card reader options | `payments_hub_order_card_reader_tapped` |
| 4 | Tap to Pay | text: "Tap to Pay" | `payments_hub_tap_to_pay_tapped` |
| 5 | Toggle Cash on Delivery | CoD toggle | `payments_hub_cash_on_delivery_toggled` |

### Reviews

| Step | Action | Element | Logcat Event |
|------|--------|---------|-------------|
| 1 | Tap "Reviews" in menu | text: "Reviews" | `reviews_loaded` |
| 2 | Toggle unread filter | `unread_filter_switch` | |
| 3 | Tap a review | review row in `reviewsList` | `review_open` |
| 4 | Approve review | approve button on detail | `review_action_success` |
| 5 | Spam/Trash review | spam or trash button | `review_action_success` |
| 6 | Mark all read | toolbar menu option | `reviews_mark_all_read_success` |

### Coupons

| Step | Action | Element | Logcat Event |
|------|--------|---------|-------------|
| 1 | Tap "Coupons" in menu | text: "Coupons" | `coupons_loaded` |
| 2 | Tap a coupon | coupon row | `coupon_details` |
| 3 | Create coupon | FAB: `add_coupon_button` | `coupon_creation_initiated` |
| 4 | Edit coupon details | coupon detail fields | |
| 5 | Save coupon | save button | `coupon_creation_success` / `coupon_update_success` |
| 6 | Delete coupon | delete option | `coupon_delete_success` |

### Customers

| Step | Action | Element | Logcat Event |
|------|--------|---------|-------------|
| 1 | Tap "Customers" in menu | text: "Customers" | |
| 2 | Tap a customer | customer row | |
| 3 | View customer details | customer detail screen | |

### Blaze Campaigns

| Step | Action | Element | Logcat Event |
|------|--------|---------|-------------|
| 1 | Tap "Blaze" in menu | text: "Blaze" | `blaze_entry_point_tapped` |
| 2 | View campaign list | campaign rows | `blaze_flow_started` |
| 3 | Create campaign | create button | `blaze_creation_form_displayed` |
| 4 | Edit ad creative | ad editing screen | `blaze_creation_edit_ad_tapped` |
| 5 | Set targeting | location/language/device/interests | |
| 6 | Select payment method | payment methods list | |
| 7 | Submit campaign | submit button | `blaze_campaign_creation_success` |

### Google for WooCommerce

| Step | Action | Element | Logcat Event |
|------|--------|---------|-------------|
| 1 | Tap "Google for WooCommerce" in menu | text: "Google for WooCommerce" | `googleads_entry_point_tapped` |
| 2 | WebView opens | Google Ads dashboard or creation flow | `googleads_flow_started` |
| 3 | Campaign creation success | success bottom sheet | `googleads_campaign_creation_success` |

### Bookings (if extension active)

| Step | Action | Element | Logcat Event |
|------|--------|---------|-------------|
| 1 | Tap "Bookings" in menu or bottom tab | text: "Bookings" | `booking_list_view` |
| 2 | Filter by date/status | filter controls | `booking_list_apply_filters` |
| 3 | Tap a booking | booking row | `booking_list_booking_tap` |
| 4 | View booking details | booking detail screen | |
| 5 | Cancel booking | cancel button | `booking_detail_cancel_booking` |
| 6 | Add note | add note button | `booking_detail_add_note_tap` |
| 7 | View linked order | order link | `booking_detail_view_linked_order_tap` |

### Settings

| Screen | Nav Path | Logcat Event |
|--------|----------|-------------|
| Main Settings | Menu -> "Settings" | `main_menu_settings_tapped` |
| Privacy Settings | Settings -> "Privacy settings" | `settings_privacy_settings_button_tapped` |
| Beta Features | Settings -> "Experimental features" | `settings_beta_features_button_tapped` |
| Developer Options | Settings -> "Developer options" (debug only) | |
| Plugins | Settings -> "Plugins" | |
| About | Settings -> "About" | `settings_about_button_tapped` |
| Logout | Settings -> "Log out" button | `settings_logout_button_tapped` |
| Domain Management | Settings -> "Domains" | `settings_domains_tapped` |
| Help & Support | Settings -> "Help & Support" | |
| Image Optimization | Settings -> toggle | `settings_image_optimization_toggled` |
