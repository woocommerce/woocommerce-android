# Main App Navigation Reference

Navigation map for the WooCommerce store management app (everything outside POS).
Uses Fragment-based navigation with XML nav graphs inside `MainActivity`.

## Feature Tree

```
MainActivity
|
+-- Dashboard (My Store) tab
|   +-- Stats Card (revenue, visitors, orders)
|   +-- Top Performers Card
|   +-- Date Range Selector
|   +-- -> Analytics Hub
|   |   +-- Analytics Settings
|   +-- -> Store Onboarding
|   +-- -> Inbox
|   +-- -> Blaze Campaigns
|
+-- Orders tab
|   +-- Order List (filterable by status)
|   |   +-- Order Filters
|   |   +-- Search Orders
|   |   +-- Create Order (FAB)
|   |   +-- Barcode Scanning
|   +-- -> Order Detail
|       +-- Order Status Change
|       +-- Add Order Note
|       +-- Order Fulfillment
|       +-- -> Issue Refund
|       |   +-- Refund Detail
|       +-- -> Shipping Labels (Print, Create, Refund)
|       +-- -> Edit Order
|       +-- -> Customer/Address Editing
|       +-- -> Receipt Preview
|       +-- -> Card Payment Flow
|
+-- Products tab
|   +-- Product List (filterable, sortable)
|   |   +-- Product Filters
|   |   +-- Search Products
|   |   +-- Add Product (FAB)
|   |   +-- Scan to Update Inventory
|   +-- -> Product Detail
|       +-- Product Images (Gallery)
|       +-- Product Pricing
|       +-- Product Inventory
|       +-- -> Variation List -> Variation Detail
|       +-- -> Product Attributes
|       +-- -> Product Categories
|       +-- -> Product Reviews
|       +-- -> Product Downloads
|       +-- -> Product Subscriptions
|       +-- -> Product Add-ons
|       +-- -> Blaze Campaign Creation
|
+-- Bookings tab (if extension active)
|   +-- Booking List (filterable)
|   +-- -> Booking Detail
|
+-- Point of Sale tab (if enabled)
|   +-- -> WooPosActivity (see pos-navigation.md)
|
+-- Menu (More) tab
    +-- Payments -> Payments Hub
    |   +-- Card Reader Setup
    |   +-- Tap to Pay
    |   +-- Scan to Pay
    +-- Reviews -> Review List -> Review Detail
    +-- Coupons -> Coupon List -> Coupon Detail
    |   +-- Create/Edit Coupon
    +-- Customers -> Customer List -> Customer Detail
    +-- Subscriptions
    +-- Blaze -> Campaign List -> Campaign Detail
    +-- Google for WooCommerce
    +-- Settings (opens AppSettingsActivity)
        +-- Privacy Settings
        +-- Account Settings
        +-- Notification Settings
        +-- Experimental Features (Beta)
        +-- Developer Options (debug only)
        +-- Plugins
        +-- Theme Picker
        +-- About
```

## Global Elements

These elements exist across all screens within `MainActivity`.

| Element | Resource ID | Notes |
|---------|------------|-------|
| Bottom Navigation Bar | `bottom_nav` | Visible on top-level screens, hidden on detail screens |
| Toolbar | `toolbar` | Material toolbar, shows screen title |
| Navigation Host | `nav_host_fragment_main` | Container for all fragments |
| Offline Bar | `offline_bar` | Visible only when device is offline |

## Bottom Navigation Tabs

| Tab | Resource ID | Label | Target Screen |
|-----|------------|-------|---------------|
| My Store | `dashboard` | "My store" | Dashboard |
| Orders | `orders` | "Orders" | Orders List |
| Products | `products` | "Products" | Products List |
| Bookings | `bookings` | "Bookings" | Bookings List (only if extension active) |
| Point of Sale | `point_of_sale` | "Point of Sale" | POS (only if enabled) |
| Menu | `moreMenu` | "Menu" | More Menu |

To navigate between tabs, find the target tab by its `identifier`, compute center coordinates, and tap. The active tab has `selected: true` in the accessibility tree.

## Screen Identifiers

Use these to confirm which screen is displayed after navigation. Call `mobile_list_elements_on_screen` and look for the **Primary Identifier**.

### Top-Level Screens

**Dashboard (My Store)** -- Fragment: `DashboardFragment` -- Tap `dashboard` bottom tab

| Key Element | Identifier | Notes |
|-------------|-----------|-------|
| **Primary** | `dashboard_container` | ComposeView hosting dashboard cards |
| Stats card | testTag: `dashboard_stats_card` | Revenue/visitors stats |
| Top performers | testTag: `dashboard_top_performers_card` | Top-performing products |
| Date range dropdown | testTag: `stats_range_dropdown_button` | Date range selector |

**Orders List** -- Fragment: `OrderListFragment` -- Tap `orders` bottom tab

| Key Element | Identifier | Notes |
|-------------|-----------|-------|
| **Primary** | `order_list_view` | Order list RecyclerView |
| Filters card | `order_filters_card` | Status filter chips |
| Create order FAB | `createOrderButton` | contentDescription: "Create order" |

**Products List** -- Fragment: `ProductListFragment` -- Tap `products` bottom tab

| Key Element | Identifier | Notes |
|-------------|-----------|-------|
| **Primary** | `productsRecycler` | Products RecyclerView |
| Add product FAB | `addProductButton` | contentDescription: "Add product" |
| Sort/filter card | `products_sort_filter_card` | Filter/sort controls |
| Empty view | `empty_view` | Shown when no products match |

**More Menu** -- Fragment: `MoreMenuFragment` -- Tap `moreMenu` bottom tab

Fully Compose-based with no XML resource IDs. Identify by text labels: "Payments", "Settings", "Coupons", etc. Common menu items:
- "Payments" -> Payments Hub
- "Reviews" -> Reviews List
- "Coupons" -> Coupon List
- "Customers" -> Customer List
- "Blaze" -> Blaze Campaign List
- "Settings" -> Settings activity
- "Subscriptions" -> Subscriptions
- "Google for WooCommerce" -> Google Ads

**Reviews List** -- Fragment: `ReviewListFragment` -- Menu tab -> "Reviews"

| Key Element | Identifier | Notes |
|-------------|-----------|-------|
| **Primary** | `reviewsList` | Reviews RecyclerView |
| Unread filter | `unread_filter_switch` | Toggle to filter unread |

**Analytics Hub** -- Fragment: `AnalyticsHubFragment` -- Dashboard -> "View all store analytics"

| Key Element | Identifier | Notes |
|-------------|-----------|-------|
| **Primary** | `analyticsRefreshLayout` | SwipeRefreshLayout |
| Date range selector | `analyticsDateSelectorCard` | Date range picker card |
| Analytics cards | `cards` | RecyclerView with metric cards |

### Detail Screens

**Order Detail** -- Fragment: `OrderDetailFragment` -- Orders tab -> tap any order row

| Key Element | Identifier | Notes |
|-------------|-----------|-------|
| **Primary** | `orderDetail_container` | Main content container |
| Order status | `orderDetail_orderStatus` | Status card at top |
| Product list | `orderDetail_productList` | Products in the order |
| Payment info | `orderDetail_paymentInfo` | Payment details card |
| Customer info | `orderDetail_customerInfo` | Customer details card |
| Refunds info | `orderDetail_refundsInfo` | Visible if refunds exist |
| Notes list | `orderDetail_noteList` | Order notes section |
| Trash button | `orderDetail_trash` | Move to trash |

**Product Detail** -- Fragment: `ProductDetailFragment` -- Products tab -> tap any product row

| Key Element | Identifier | Notes |
|-------------|-----------|-------|
| **Primary** | `productDetail_root` | Root CoordinatorLayout |
| Toolbar | `productDetailToolbar` | Shows product name |
| Image gallery | `imageGallery` | Product image carousel |
| Product cards | `cardsRecyclerView` | Product detail cards |

**Review Detail** -- Fragment: `ReviewDetailFragment` -- Reviews -> tap any review row

Identify by `fragment_review_detail` layout elements.

### Settings Screens

Settings is a separate Activity (`AppSettingsActivity`). Use `adb shell dumpsys activity top` to confirm. Items are identified by text labels.

| Screen | Fragment | Nav Path |
|--------|----------|----------|
| Main Settings | `MainSettingsFragment` | Menu -> "Settings" |
| Privacy Settings | `PrivacySettingsFragment` | Settings -> "Privacy settings" |
| Experimental features | `BetaFeaturesFragment` | Settings -> "Experimental features" |
| Developer Options | `DeveloperOptionsFragment` | Settings -> "Developer options" (debug only) |
| Manage Notifications | OS Notification Settings | Settings -> Manage Notifications |
| Account Settings | `AccountSettingsFragment` | Settings -> Account section |
| About | `UnifiedAboutScreenActivity` | Settings -> "About" |
| Plugins | `PluginsFragment` | Settings -> "Plugins" |

### Payments & Commerce

**Payments Hub** -- Fragment: `PaymentsHubFragment` -- Menu tab -> "Payments"

| Key Element | Identifier | Notes |
|-------------|-----------|-------|
| **Primary** | `paymentsHubRv` | Payments options RecyclerView |
| Loading indicator | `paymentsHubLoading` | LinearProgressIndicator |

**Coupon List** -- Fragment: `CouponListFragment` -- Menu tab -> "Coupons"

| Key Element | Identifier | Notes |
|-------------|-----------|-------|
| **Primary** | `couponsComposeView` | ComposeView hosting coupon list |
| Add coupon FAB | `add_coupon_button` | Floating action button |

## Compose Test Tags

Stable test tags applied via `Modifier.testTag()`. They appear in the accessibility tree as resource IDs.

**Dashboard** (defined in `DashboardStatsTestTags`):
`dashboard_stats_card`, `dashboard_top_performers_card`, `stats_range_dropdown_button`, `stats_range_dropdown_menu`

## Navigation Flows

Step-by-step paths for reaching common screens from the Dashboard.

```
Orders List:       Tap bottom nav "orders" tab
Order Detail:      Orders List -> tap any order row -> wait for orderDetail_container
Order Creation:    Orders List -> tap createOrderButton (FAB)
Order Filters:     Orders List -> tap filter chip in order_filters_card

Products List:     Tap bottom nav "products" tab
Product Detail:    Products List -> tap any product row -> wait for productDetail_root
Product Creation:  Products List -> tap addProductButton (FAB) -> select product type
Product Search:    Products List -> tap search icon in toolbar -> type in search field

Settings:          Menu tab -> tap "Settings" (opens AppSettingsActivity)
Privacy Settings:  Settings -> tap "Privacy settings"
Experimental:      Settings -> tap "Experimental features"
Developer Options: Settings -> tap "Developer options" (debug builds only)

Payments Hub:      Menu tab -> tap "Payments"
Coupon List:       Menu tab -> tap "Coupons"
Reviews List:      Menu tab -> tap "Reviews"
Analytics Hub:     Dashboard -> tap "View all store analytics"
Customer List:     Menu tab -> tap "Customers"
Blaze:             Menu tab -> tap "Blaze"
```
