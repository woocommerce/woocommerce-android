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
+-- Home Screen (split-pane layout: Products | Cart | Totals)
    |
    +-- LEFT PANE: Product/Coupon Browser (~65%)
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
    +-- RIGHT PANE: Cart (~35%)
    |   +-- Cart Items (remove)
    |   +-- Checkout button
    |
    +-- CHECKOUT (scrolls right to show Totals pane full-screen)
    |   +-- Totals (subtotal, tax, discounts)
    |   +-- -> Card Payment Screen
    |   |   +-- -> Payment Success Screen
    |   |       +-- -> Email Receipt Screen
    |   +-- -> Cash Payment Screen
    |       +-- -> Payment Success Screen
    |           +-- -> Email Receipt Screen
    |
    +-- FLOATING TOOLBAR (bottom-left)
        +-- -> Bookings (CIAB sites only)
        |   +-- Date Selector
        |   +-- Booking List
        |   +-- -> Booking Note
        |   +-- -> Booking Payment (Card/Cash)
        |       +-- -> Payment Success
        +-- -> Orders
        |   +-- -> Order Detail
        |       +-- -> Issue Refund
        |       |   +-- Refund Reason Screen
        |       +-- -> Email Receipt
        +-- -> Settings (master-detail pane)
        |   +-- Store (Receipt preferences)
        |   +-- Hardware
        |   |   +-- Card Reader Setup
        |   |   +-- Barcode Scanner Setup
        |   +-- Local Catalog
        |   +-- Help
        +-- -> Exit POS (confirmation dialog -> returns to MainActivity)
```

## POS Screen Identifiers

POS screens are 100% Compose. Elements are identified via `Modifier.testTag()` or by display text/contentDescription.

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

Each step shows: action and element to find.

### Checkout Flow

| Step | Action | Element |
|------|--------|---------|
| 1 | Tap a product to add to cart | tag: `woo_pos_product_item` |
| 2 | Verify cart updated | tag: `woo_pos_cart_items_count` |
| 3 | Tap Checkout | tag: `woo_pos_checkout_button` |
| 4a | **Card:** tap card payment button | text: card payment option |
| 4b | **Cash:** tap cash, then complete | tags: `woo_pos_cash_payment_button` -> `woo_pos_complete_payment_button` |
| 5 | Verify payment success | tag: `woo_pos_success_checkmark_icon` |
| 6 | Tap New Order or Email Receipt | tag: `woo_pos_new_order_button` |

### Orders & Refund Flow

| Step | Action | Element |
|------|--------|---------|
| 1 | Open toolbar menu | bottom-left menu button |
| 2 | Tap "Orders" | text: "Orders" |
| 3 | Wait for orders list | order rows with `#` numbers |
| 4 | Select an order | tap order row |
| 5 | Tap "Issue refund" | text: "Issue refund" |
| 6 | Select items to refund | items pre-selected, tap "Continue" |
| 7 | Review and confirm refund | verify amounts, tap "Continue" |
| 8 | Confirm in dialog | tap confirmation button |

### Bookings Flow (CIAB sites only)

| Step | Action | Element |
|------|--------|---------|
| 1 | Open toolbar menu, tap "Bookings" | text: "Bookings" |
| 2 | Browse bookings | date selector to filter |
| 3 | Select a booking | tap booking row |
| 4 | View details / add note | booking detail pane |
| 5 | Accept payment | card/cash flow with `source=BOOKINGS` |

### Other Events

| Action | Element |
|--------|---------|
| POS loaded | splash -> home |
| Open Settings | toolbar menu -> "Settings" |
| Close Settings | back / close button |
| Exit POS | toolbar menu -> "Exit POS" -> confirm |
