# POS Navigation Reference

Navigation map for the WooCommerce Point of Sale (POS) feature.
POS runs in a **separate Activity** (`WooPosActivity`), uses **pure Compose navigation** (no Fragments), and is **landscape-only** with a split-pane layout.

## Quick Launch

Skip all main app navigation -- launch POS directly:

```bash
adb -s <device_id> shell am start -n com.woocommerce.android.dev/com.woocommerce.android.ui.woopos.root.WooPosActivity
```

**Prerequisites:** App must be logged in. If POS shows an eligibility screen, the store may not have POS enabled.

## Feature Tree

```
WooPosActivity (landscape)
|
+-- Splash Screen (loading/eligibility check)
|   +-- -> Eligibility Screen (if store not eligible)
|
+-- Home Screen (split-pane layout)
    |
    +-- LEFT PANE: Cart
    |   +-- Cart Items (quantity, remove)
    |   +-- Cart Summary
    |
    +-- RIGHT PANE: Product/Coupon Browser
    |   +-- Products Tab
    |   |   +-- Product Grid (with images, prices)
    |   |   +-- Search Products
    |   |   +-- -> Product Variations Modal
    |   |   +-- Barcode Scanning
    |   +-- Coupons Tab
    |       +-- Coupon List
    |       +-- Search Coupons
    |       +-- -> Create Coupon (external activity)
    |
    +-- CHECKOUT (right pane expands)
    |   +-- Totals (subtotal, tax, discounts)
    |   +-- -> Card Payment Screen
    |   |   +-- -> Payment Success Screen
    |   |       +-- -> Email Receipt Screen
    |   +-- -> Cash Payment Screen
    |       +-- -> Payment Success Screen
    |           +-- -> Email Receipt Screen
    |
    +-- FLOATING TOOLBAR (bottom-left)
        +-- -> Orders
        |   +-- -> Order Detail
        |       +-- -> Issue Refund
        |       |   +-- Refund Reason Screen
        |       +-- -> Email Receipt
        +-- -> Bookings
        |   +-- Date Selector
        |   +-- Booking List
        |   +-- -> Booking Note
        |   +-- -> Booking Payment (Card/Cash)
        |       +-- -> Payment Success
        +-- -> Settings
        |   +-- Hardware
        |   |   +-- Card Reader Setup
        |   |   +-- Barcode Scanner Setup
        |   +-- Local Catalog
        |   +-- Store (Receipt preferences)
        |   +-- Help
        +-- -> Exit POS (returns to MainActivity)
```

## Screen Layout

POS uses a **horizontal split-pane** layout in landscape orientation:

```
+-------------------------+-----------------------------------------+
|                         |  [Products]  [Coupons]        [Search]  |
|                         |                                         |
|       CART              |       PRODUCT / COUPON GRID             |
|    (left ~35%)          |          (right ~65%)                   |
|                         |                                         |
|   - Item 1    $10.00   |   +------+  +------+  +------+         |
|   - Item 2    $15.00   |   |Prod 1|  |Prod 2|  |Prod 3|         |
|                         |   +------+  +------+  +------+         |
|                         |                                         |
|   Subtotal    $25.00   |   +------+  +------+  +------+         |
|   Tax          $2.50   |   |Prod 4|  |Prod 5|  |Prod 6|         |
|                         |   +------+  +------+  +------+         |
|                         |                                         |
+-------------------------+-----------------------------------------+
| [Menu] [Connection]                          [Checkout $27.50]    |
+-------------------------------------------------------------------+
```

During checkout, the right pane expands to show payment options.

## POS Screen Identifiers

POS screens are 100% Compose. Elements are identified via `Modifier.testTag()` (exposed as resource IDs because `testTagsAsResourceId` is enabled) or by display text/contentDescription.

### Test Tags (defined in `WooPosTestTags`)

| Test Tag | Screen/Element |
|----------|---------------|
| `woo_pos_product_item` | Product card in grid |
| `woo_pos_checkout_button` | Checkout / Charge button |
| `woo_pos_cash_payment_button` | Cash payment option |
| `woo_pos_complete_payment_button` | Mark payment as complete (cash) |
| `woo_pos_new_order_button` | New order (after payment success) |
| `woo_pos_success_checkmark_icon` | Payment success screen checkmark |
| `woo_pos_cart_items_count` | Cart item count badge |

### Screen Detection

| Screen | How to Detect |
|--------|---------------|
| Home (product browsing) | `woo_pos_product_item` elements present |
| Cart with items | `woo_pos_checkout_button` visible |
| Checkout/Totals | Text containing "Subtotal" or "Total" with payment buttons |
| Card Payment | Text "Tap, insert, or swipe" or card reader status |
| Cash Payment | `woo_pos_complete_payment_button` visible |
| Payment Success | `woo_pos_success_checkmark_icon` visible |
| Orders List | Text "Orders" as screen title, order rows with # numbers |
| Order Detail | Order number header with "Issue refund" button |
| Settings | Text "Settings" with category list (Hardware, Store, etc.) |
| Bookings | Text "Bookings" with date selector |
| Eligibility | Text about POS requirements or plan upgrade |

## Key Workflows

### Checkout Flow (Products -> Payment)

1. **Add products to cart** -- find product elements (`woo_pos_product_item`), tap to add
2. **Verify cart updated** -- check `woo_pos_cart_items_count` or cart item list
3. **Tap Checkout** -- find and tap `woo_pos_checkout_button`
4. **Choose payment method:**
   - Card: tap card payment button -> follow card reader prompts
   - Cash: tap `woo_pos_cash_payment_button` -> tap `woo_pos_complete_payment_button`
5. **Payment success** -- verify `woo_pos_success_checkmark_icon` appears
6. **New order or email receipt** -- tap `woo_pos_new_order_button` or email receipt option

### Orders & Refund Flow

1. **Open menu** -- find and tap the menu/toolbar button (bottom-left area)
2. **Tap "Orders"** -- find text "Orders" in menu
3. **Wait for orders list** -- look for order rows with `#` prefixed numbers
4. **Select an order** -- tap an order row
5. **View order detail** -- verify order information displayed
6. **Issue refund** -- find and tap "Issue refund" button
7. **Select items to refund** -- items are pre-selected, tap "Continue"
8. **Review refund** -- verify amounts, tap "Continue"
9. **Confirm refund** -- tap confirmation button in dialog
10. **Refund success** -- verify success message, tap "Close"

### Bookings Flow

1. **Open menu** -> tap "Bookings"
2. **Browse bookings** -- use date selector to filter
3. **Select a booking** -- tap a booking row
4. **View booking details** -- check notes, status
5. **Accept payment** -- initiates card/cash payment flow with `source=BOOKINGS`

### Settings

1. **Open menu** -> tap "Settings"
2. **Settings uses master-detail pane** -- categories on left, detail on right
3. **Categories:** Hardware, Local Catalog, Store, Help
4. **Hardware** -> Card Reader Setup, Barcode Scanner Setup

### Exit POS

1. **Open menu** -> tap "Exit POS"
2. **Confirm exit dialog** -- tap confirmation button
3. **Returns to `MainActivity`** (main app dashboard)

## Tracking Events (for Logcat Verification)

Use these with the logcat verification technique from the base skill.

| Action | Logcat Event |
|--------|-------------|
| POS loaded | `pos_loaded` |
| Product added to cart | `pos_item_added_to_cart` |
| Checkout tapped | `pos_checkout_tapped` |
| Cash payment tapped | `pos_checkout_cash_payment_tapped` |
| Cash payment completed | `pos_cash_collect_payment_success` |
| New order tapped | `pos_create_new_order_tapped` |
| Settings opened | `pos_settings_opened` |
| Orders menu tapped | `pos_orders_menu_item_tapped` |
| Orders list fetched | `pos_orders_list_fetched` |

**Example -- verify checkout completed:**

```bash
# After tapping checkout button via mobile-mcp
adb -s <device_id> shell "timeout 5 logcat | grep -m1 'pos_checkout_tapped'"
```

**Example -- event-driven workflow (clear, act, verify):**

```bash
# 1. Clear logcat
adb -s <device_id> logcat -c
# 2. Perform action via mobile-mcp (tap product, tap checkout, etc.)
# 3. Wait for confirmation event
adb -s <device_id> shell "timeout 5 logcat | grep -m1 'pos_item_added_to_cart'"
```

## Navigation Routes (Internal)

These are the Compose Navigation routes defined in `WooPosMainFlowGraph.kt`:

| Route | Screen |
|-------|--------|
| `splash` | Splash/loading screen |
| `home` | Main POS home (split-pane) |
| `home/settings` | Settings |
| `home/orders` | Orders list |
| `home/orders/{orderId}` | Order detail |
| `orders/refund_reason/{orderId}/{initialReason}` | Refund reason |
| `home/bookings` | Bookings list |
| `bookings/note/{bookingId}` | Booking note |
| `home/card_payment/{orderId}` | Card payment |
| `home/cash_payment/{orderId}` | Cash payment |
| `home/email_receipt/{orderId}` | Email receipt |
| `home/payment_success/{orderId}/{source}` | Payment success |
| `eligibility/{reason}` | Eligibility gate |
