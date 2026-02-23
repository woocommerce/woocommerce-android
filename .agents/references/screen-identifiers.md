# Screen Identifiers Reference

This document maps every major screen in the WooCommerce Android app to the identifiers that AI agents can use with `mobile_list_elements_on_screen` to detect which screen is currently displayed and interact with key elements.

All resource IDs below use the debug package prefix `com.woocommerce.android.dev:id/`. When reading the accessibility tree, match the `identifier` field against these values.

## How to Use This Reference

1. **Identify the current screen:** After navigating, call `mobile_list_elements_on_screen` and look for the **Primary Identifier** listed for each screen.
2. **Find interactive elements:** Use the **Key Elements** to locate buttons, lists, and inputs you need to interact with.
3. **Navigate between screens:** Follow the **Nav Path** to reach any screen from the Dashboard.
4. **Confirm via ADB (fallback):** Run `adb -s <device> shell dumpsys activity top | head -5` to get the current Fragment/Activity class name.

## Global Elements (Always Present)

These elements exist across all screens within `MainActivity`.

| Element | Resource ID | Notes |
|---------|------------|-------|
| Bottom Navigation Bar | `bottom_nav` | Contains tab items; visible on top-level screens, hidden on detail screens |
| Collapsing Toolbar | `collapsing_toolbar` | Shows the screen title; collapses on scroll for some screens |
| Toolbar | `toolbar` | Material toolbar inside the collapsing toolbar or standalone |
| Navigation Host | `nav_host_fragment_main` | Container for all fragments |
| Offline Bar | `offline_bar` | Visible only when device is offline |
| Trial Bar | `trial_bar` | Visible only during free trial period |

## Bottom Navigation Tabs

The bottom bar can show up to 6 tabs depending on store configuration. Not all tabs are visible simultaneously (max 5 shown at once).

| Tab | Resource ID | Label Text | Target Screen |
|-----|------------|------------|---------------|
| My Store | `dashboard` | "My store" | Dashboard |
| Orders | `orders` | "Orders" | Orders List |
| Products | `products` | "Products" | Products List |
| Bookings | `bookings` | "Bookings" | Bookings List (only if Bookings extension active) |
| Point of Sale | `point_of_sale` | "Point of Sale" | POS (only if POS is enabled) |
| Menu | `moreMenu` | "Menu" | More Menu |

**Determining the active tab:** The selected tab has `selected: true` in the accessibility tree.

---

## Top-Level Screens

### Dashboard (My Store)

| | |
|---|---|
| **Fragment** | `DashboardFragment` |
| **Primary Identifier** | `dashboard_container` (ComposeView hosting the dashboard cards) |
| **Nav Path** | Tap `dashboard` bottom tab |

| Key Element | Resource ID / Test Tag | Notes |
|-------------|----------------------|-------|
| Stats container | `my_store_stats_container` | Root LinearLayout |
| Dashboard Compose content | `dashboard_container` | ComposeView with all dashboard cards |
| Stats card | testTag: `dashboard_stats_card` | Compose test tag (DashboardStatsTestTags) |
| Top performers card | testTag: `dashboard_top_performers_card` | Compose test tag (DashboardStatsTestTags) |
| Date range dropdown | testTag: `stats_range_dropdown_button` | Compose test tag (DashboardStatsTestTags) |
| Date range menu | testTag: `stats_range_dropdown_menu` | Compose test tag (DashboardStatsTestTags) |
| JITM message | `jitmFragment` | Just-in-time message banner (may not be visible) |
| Jetpack benefits banner | `jetpack_benefits_banner` | Bottom banner (may not be visible) |

### Orders List

| | |
|---|---|
| **Fragment** | `OrderListFragment` |
| **Primary Identifier** | `order_list_view` (the order list RecyclerView) |
| **Nav Path** | Tap `orders` bottom tab |

| Key Element | Resource ID | Notes |
|-------------|------------|-------|
| List container | `listPaneContainer` | SwipeRefreshLayout wrapping the list |
| Order list view | `order_list_view` | Custom RecyclerView with order items |
| Filters card | `order_filters_card` | Status filter chips |
| Create order FAB | `createOrderButton` | Floating action button, contentDescription: "Create order" |
| Toolbar | `toolbar` | Shows "Orders" title and search icon |
| Two-pane detail | `detailPaneContainer` | Only visible on tablets |

### Products List

| | |
|---|---|
| **Fragment** | `ProductListFragment` |
| **Primary Identifier** | `productsRecycler` (products RecyclerView) |
| **Nav Path** | Tap `products` bottom tab |

| Key Element | Resource ID | Notes |
|-------------|------------|-------|
| Refresh layout | `productsRefreshLayout` | SwipeRefreshLayout wrapping the content |
| Products list | `productsRecycler` | RecyclerView with product items |
| Add product FAB | `addProductButton` | Floating action button, contentDescription: "Add product" |
| Sort and filter card | `products_sort_filter_card` | Filter/sort controls |
| Search tab view | `productsSearchTabView` | Appears when search is active |
| Blaze banner | `blaze_banner_view` | Promotional banner (may not be visible) |
| Empty view | `empty_view` | Shown when no products match |
| Toolbar | `toolbar` | Shows "Products" title and search icon |

### More Menu

| | |
|---|---|
| **Fragment** | `MoreMenuFragment` |
| **Primary Identifier** | Look for text elements: "Payments", "Customers", "Coupons", "Settings" |
| **Nav Path** | Tap `moreMenu` bottom tab |

This screen is **fully Compose-based** and has no XML resource IDs. Identify it by the presence of menu item labels like "Payments", "Settings", "Coupons", etc. Menu items are displayed as clickable rows.

**Common menu items** (vary by store configuration):
- "Payments" — navigates to Payments Hub
- "Reviews" — navigates to Reviews List
- "Coupons" — navigates to Coupon List
- "Customers" — navigates to Customer List
- "Blaze Campaigns" — navigates to Blaze Campaign List
- "Settings" — opens Settings activity
- "Subscriptions" — navigates to Subscriptions
- "Google for WooCommerce" — navigates to Google Ads

### Reviews List

| | |
|---|---|
| **Fragment** | `ReviewListFragment` |
| **Primary Identifier** | `reviewsList` (reviews RecyclerView) |
| **Nav Path** | Menu tab → tap "Reviews" |

| Key Element | Resource ID | Notes |
|-------------|------------|-------|
| Refresh layout | `notifsRefreshLayout` | SwipeRefreshLayout |
| Reviews container | `notifsContainer` | Root container |
| Reviews list | `reviewsList` | RecyclerView with review items |
| Unread filter switch | `unread_filter_switch` | Toggle to filter unread reviews |
| Empty view | `empty_view` | Shown when no reviews |

### Analytics Hub

| | |
|---|---|
| **Fragment** | `AnalyticsHubFragment` |
| **Primary Identifier** | `analyticsRefreshLayout` or `analyticsDateSelectorCard` |
| **Nav Path** | Dashboard → tap "Analytics" or via More Menu |

| Key Element | Resource ID | Notes |
|-------------|------------|-------|
| Refresh layout | `analyticsRefreshLayout` | SwipeRefreshLayout |
| Analytics root | `analyticsViewRoot` | Root ConstraintLayout |
| Date range selector | `analyticsDateSelectorCard` | Date range picker card |
| Analytics cards | `cards` | RecyclerView with analytics cards |
| Feedback banner | `analyticsHubFeedbackBanner` | Banner (may not be visible) |

---

## Detail Screens

### Order Detail

| | |
|---|---|
| **Fragment** | `OrderDetailFragment` |
| **Primary Identifier** | `orderDetail_container` (LinearLayout containing all order sections) |
| **Nav Path** | Orders tab → tap any order row |

| Key Element | Resource ID | Notes |
|-------------|------------|-------|
| Refresh layout | `orderRefreshLayout` | Root SwipeRefreshLayout |
| Order content | `orderDetail_container` | Main content container |
| Order status | `orderDetail_orderStatus` | Status card at the top |
| Product list | `orderDetail_productList` | Products in the order |
| Payment info | `orderDetail_paymentInfo` | Payment details card |
| Customer info | `orderDetail_customerInfo` | Customer details card |
| Refunds info | `orderDetail_refundsInfo` | Refunds section (visible if refunds exist) |
| Shipping labels | `orderDetail_shippingLabelList` | Shipping labels (may not be visible) |
| Shipment tracking | `orderDetail_shipmentList` | Tracking info (may not be visible) |
| Notes list | `orderDetail_noteList` | Order notes section |
| Trash button | `orderDetail_trash` | Move to trash button at the bottom |
| Custom fields | `customFieldsCard` | Custom fields card |
| Toolbar | `toolbar` | Shows order number (e.g., "#1234") |
| Scroll view | `scrollView` | NestedScrollView containing the detail content |

### Product Detail

| | |
|---|---|
| **Fragment** | `ProductDetailFragment` |
| **Primary Identifier** | `productDetail_root` (CoordinatorLayout) |
| **Nav Path** | Products tab → tap any product row |

| Key Element | Resource ID | Notes |
|-------------|------------|-------|
| Root container | `productDetail_root` | CoordinatorLayout |
| Toolbar | `productDetailToolbar` | Shows product name |
| Collapsing toolbar | `collapsing_toolbar` | Collapses product image on scroll |
| Image gallery | `imageGallery` | Product image carousel |
| Add image container | `addImageContainer` | Shown when no images |
| Product cards | `cardsRecyclerView` | RecyclerView with product detail cards |
| Add more button | `productDetail_addMoreButton` | "Add more details" button at the bottom |
| Scroll view | `scrollView` | NestedScrollView with product detail content |
| Error container | `productErrorStateContainer` | Shown on error |

### Review Detail

| | |
|---|---|
| **Fragment** | `ReviewDetailFragment` |
| **Primary Identifier** | Look for `fragment_review_detail` layout elements |
| **Nav Path** | Reviews → tap any review row |

---

## Settings Screens

Settings is a separate Activity (`AppSettingsActivity`) with its own navigation graph (`nav_graph_settings`).

### Main Settings

| | |
|---|---|
| **Fragment** | `MainSettingsFragment` |
| **Primary Identifier** | Look for text "Settings" in toolbar and option labels like "Store", "Beta features" |
| **Nav Path** | Menu tab → tap "Settings" |

The Settings screen opens a new Activity. Use `adb shell dumpsys activity top` to confirm you're in `AppSettingsActivity`. Settings items are identified by their text labels.

### Settings Sub-Screens

| Screen | Fragment | Nav Path from Settings |
|--------|----------|----------------------|
| Privacy Settings | `PrivacySettingsFragment` | Settings → "Privacy settings" |
| Beta Features | `BetaFeaturesFragment` | Settings → "Beta features" |
| About | `UnifiedAboutScreenActivity` | Settings → "About" |
| Licenses | `LicensesFragment` | Settings → "Licenses" |
| Developer Options | `DeveloperOptionsFragment` | Settings → "Developer options" (debug builds only) |
| Account Settings | `AccountSettingsFragment` | Settings → Account section |
| Notification Settings | `NotificationSettingsFragment` | Settings → "Notifications" |
| Plugins | `PluginsFragment` | Settings → "Plugins" |

---

## Payments & Commerce Screens

### Payments Hub

| | |
|---|---|
| **Fragment** | `PaymentsHubFragment` |
| **Primary Identifier** | `paymentsHubRv` (payments options RecyclerView) |
| **Nav Path** | Menu tab → tap "Payments" |

| Key Element | Resource ID | Notes |
|-------------|------------|-------|
| Toolbar | `toolbar` | Shows "Payments" title |
| Payments list | `paymentsHubRv` | RecyclerView with payment options |
| Loading indicator | `paymentsHubLoading` | LinearProgressIndicator |
| Onboarding error | `paymentsHubOnboardingFailedTv` | Error text at bottom |

### Coupon List

| | |
|---|---|
| **Fragment** | `CouponListFragment` |
| **Primary Identifier** | `couponsComposeView` (Compose content) |
| **Nav Path** | Menu tab → tap "Coupons" |

| Key Element | Resource ID | Notes |
|-------------|------------|-------|
| Compose content | `couponsComposeView` | ComposeView hosting the coupon list |
| Add coupon FAB | `add_coupon_button` | Floating action button |

---

## Common Dialogs and Overlays

These may appear at any time and should be dismissed before proceeding.

| Dialog | How to Detect | How to Dismiss |
|--------|---------------|----------------|
| Privacy Banner | Text "Privacy Settings" or "Save" button on a bottom sheet | Tap "Save" — this dialog is NOT cancellable via BACK |
| What's New | Element with id containing `closeFeatureAnnouncementButton` or text "Close" | Tap the close button |
| App Rating | AlertDialog text containing "rate" or "enjoy" | Tap "No Thanks" or "Remind Me Later" |
| Android Permission | Elements from `com.android.permissioncontroller` | Tap "Allow" or "Don't allow" |
| Snackbar | Element with id `snackbar_text` near bottom of screen | Wait 3-4 seconds for auto-dismiss |
| Store Name Dialog | Text "Name your store" (id: `nameYourStoreDialogFragment`) | Tap "Save" or dismiss |
| Create Test Order | Text related to test order creation | Tap "Dismiss" or "Create" |

---

## Compose Test Tags

These are stable test tags applied via `Modifier.testTag()` in Compose code. They appear in the accessibility tree only if `testTagsAsResourceId` is enabled.

### Dashboard Test Tags (DashboardStatsTestTags)

| Tag | Element |
|-----|---------|
| `dashboard_stats_card` | Revenue/visitors stats card |
| `dashboard_top_performers_card` | Top-performing products card |
| `stats_range_dropdown_button` | Date range selector button |
| `stats_range_dropdown_menu` | Date range dropdown menu |

### POS Test Tags (WooPosTestTags)

| Tag | Element |
|-----|---------|
| `woo_pos_product_item` | Product item in POS product list |
| `woo_pos_checkout_button` | Checkout button |
| `woo_pos_cash_payment_button` | Cash payment option |
| `woo_pos_complete_payment_button` | Complete payment button |
| `woo_pos_new_order_button` | Start new order button |
| `woo_pos_success_checkmark_icon` | Payment success checkmark |
| `woo_pos_cart_items_count` | Cart items counter |

---

## Navigation Flows

Step-by-step instructions for reaching common screens from the Dashboard.

### Orders

```
Orders List:       Tap bottom nav "orders" tab
Order Detail:      Orders List → tap any order row → wait for orderDetail_container
Order Creation:    Orders List → tap createOrderButton (FAB)
Order Refund:      Order Detail → scroll to orderDetail_refundsInfo → tap "Issue Refund"
Order Notes:       Order Detail → scroll to orderDetail_noteList → tap "Add a note"
Order Filters:     Orders List → tap filter chip in order_filters_card
```

### Products

```
Products List:     Tap bottom nav "products" tab
Product Detail:    Products List → tap any product row → wait for productDetail_root
Product Creation:  Products List → tap addProductButton (FAB) → select product type
Product Search:    Products List → tap search icon in toolbar → type in search field
Product Filters:   Products List → tap filter/sort in products_sort_filter_card
```

### Settings

```
Settings:          Menu tab → tap "Settings" (opens AppSettingsActivity)
Privacy Settings:  Settings → tap "Privacy settings"
Beta Features:     Settings → tap "Beta features"
Developer Options: Settings → tap "Developer options" (debug builds only)
Notifications:     Settings → tap "Notifications"
Account Settings:  Settings → tap account section
About:             Settings → tap "About"
```

### Payments

```
Payments Hub:      Menu tab → tap "Payments"
Collect Payment:   Payments Hub → tap "Collect payment"
Tap to Pay:        Payments Hub → tap "Tap to Pay on Android"
```

### Coupons

```
Coupon List:       Menu tab → tap "Coupons"
Coupon Detail:     Coupon List → tap any coupon row
Create Coupon:     Coupon List → tap add_coupon_button (FAB)
```

### Reviews

```
Reviews List:      Menu tab → tap "Reviews"
Review Detail:     Reviews List → tap any review row
```

### Analytics

```
Analytics Hub:     Dashboard → tap "See analytics" or Menu tab → "Analytics"
Date Range:        Analytics Hub → tap analyticsDateSelectorCard
Analytics Settings: Analytics Hub → tap settings icon
```

### Blaze Campaigns

```
Campaign List:     Menu tab → tap "Blaze Campaigns"
Campaign Detail:   Campaign List → tap any campaign row
Create Campaign:   Campaign List → tap create button
```

### Customers

```
Customer List:     Menu tab → tap "Customers"
Customer Detail:   Customer List → tap any customer row
```

---

## Diagnostic ADB Commands

When `mobile_list_elements_on_screen` is not sufficient:

| Command | Purpose |
|---------|---------|
| `adb -s <device> shell dumpsys activity top \| head -20` | Identify current foreground Fragment/Activity |
| `adb -s <device> shell dumpsys window \| grep mCurrentFocus` | Get the current window/dialog in focus |
| `adb -s <device> logcat -d *:E \| tail -30` | Check recent error logs |
| `adb -s <device> shell am force-stop com.woocommerce.android.dev` | Force kill the app |
| `adb -s <device> shell pm clear com.woocommerce.android.dev` | Clear app data (requires re-login) |
