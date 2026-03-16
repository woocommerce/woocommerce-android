# POS Phone Support - Design Spec

## Goal

Add phone support to the WooCommerce POS (Point of Sale). Currently, POS only works on tablets in landscape mode. This first iteration adds portrait-mode phone support for the home screen flow: product browsing, cart, checkout, and payments (cash and card reader).

## Screenshots Reference

Tablet screenshots for comparison saved in `docs/plans/pos-phone/screenshots/`:
- `tablet-01-home-empty-cart.png` - Products (65%) | Cart (35%) with empty state
- `tablet-02-cart-with-items.png` - Products | Cart with items + "Check out" button
- `tablet-03-checkout-reader-disconnected.png` - Cart (35%) | Totals (65%) with reader not connected
- `tablet-04-checkout-reader-connected.png` - Cart (35%) | Totals (65%) with reader connected, showing totals grid + cash payment button
- `tablet-06-card-payment-success.png` - Full-screen payment success with "New order" and "Email receipt" buttons

## Approach

**Branch at the home screen level.** Detect phone vs tablet and render a different composable tree for phone while reusing all child composables, ViewModels, and state management. Tablet code stays untouched.

Key principle: zero risk to existing tablet code. Phone support is purely additive.

## Phone UX Flow

```
Products ──(tap cart button)──→ Cart ──(tap checkout)──→ Totals/Payment
    ↑                             ↑                          │
    └──────(back)─────────────────┘──────(back)──────────────┘
```

Portrait mode only. No landscape support on phone in this iteration.

## Detailed Design

### 1. Activity & Screen Size Gate

**`WooPosActivity.onCreate`** - detect phone vs tablet and set orientation:

```kotlin
val isPhone = // WindowSizeClass == Compact or screen short side < 674dp
if (isPhone) {
    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
} else {
    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
}
```

**`WooPosIsScreenSizeAllowed`** - currently blocks phones with `MIN_SCREEN_SHORT_SIZE_DP = 674` and `MIN_SCREEN_LONG_SIZE_DP = 800`. A typical phone like Pixel 6 (411x914dp) fails the short size check. Lower to `MIN_SCREEN_SHORT_SIZE_DP = 320` and `MIN_SCREEN_LONG_SIZE_DP = 480`. POS eligibility is already gated by feature flag + country. These values exclude unreasonably small screens while allowing all modern phones.

**`WooPosTabShouldBeVisible`** - no code changes needed in this class. However, lowering `WooPosIsScreenSizeAllowed` thresholds is what makes the POS tab visible on phones. Note that `WooPosIsScreenSizeAllowed` is also used by `ClientSidePosBanner` — verify that banner behavior remains correct on phones.

### 2. Home Screen Branching

In the root screen or navigation host, detect phone vs tablet and branch:

- Phone: render `WooPosHomePhoneScreen`
- Tablet: render existing `WooPosHomeScreen` (no changes, no rename, no extract)

`WooPosHomePhoneScreen` is a new composable that uses **Jetpack Navigation Compose** internally with three routes. Separate ViewModels where needed to keep things small.

```
PHONE_PRODUCTS_ROUTE → WooPosPhoneProductsScreen
PHONE_CART_ROUTE     → WooPosPhoneCartScreen
PHONE_TOTALS_ROUTE   → WooPosPhoneTotalsScreen
```

### 3. Products Screen (Phone)

Main screen the user sees after entering POS.

```
┌──────────────────────────────┐
│ [≡]  Products    Coupons     │  menu button left of tabs
├──────────────────────────────┤
│ [🔍 Search products...     ]│  always visible search bar
├──────────────────────────────┤
│                              │
│  Product list                │  reuse WooPosItemsList
│  (full width, scrollable)    │
│                              │
├──────────────────────────────┤
│  🛒 Cart (2)       $196.66  │  cart button, visible when items > 0
└──────────────────────────────┘
```

**Reused:** Product list composables, coupons tab content, search functionality, menu popup items (Settings, Orders, Bookings, Exit) from floating toolbar.

**New:** Top bar layout with menu button to the left of tabs. Always-visible search bar below tabs (render `WooPosSearchInput` in a permanently `Open` state — the Open/Closed toggle from tablet is not used on phone). Cart button at the bottom showing item count and total.

**Cart button state access:** The cart button needs live item count and total from `WooPosCartViewModel`. Since the cart ViewModel is scoped at the activity level (via `@ActivityRetainedScoped` event bus), the phone products screen can access cart state by obtaining the cart ViewModel at the `WooPosHomePhoneScreen` NavHost scope and passing cart summary data down to the products screen composable.

**Menu button wiring:** The menu button triggers the same navigation events as the floating toolbar menu items. Reuse `WooPosHomeFloatingToolbarViewModel`'s menu item definitions and navigation event emission. Reader status is not shown on the products screen.

**No floating toolbar on phone.** The menu button replaces it. No reader connection status shown on the products screen - the totals screen handles reader connection.

**Barcode scanning:** `WooPosHomePhoneScreen` applies the `listenForBarcodes` modifier at its root, same as tablet's `WooPosHomeScreen`. Barcode events are forwarded to the ViewModel via `WooPosHomeUIEvent.OnBarcodeEvent`.

### 4. Cart Screen (Phone)

Full-screen cart, navigated to from the cart button.

```
┌──────────────────────────────┐
│ ←  Cart              2 items │  back arrow + title + count
├──────────────────────────────┤
│                              │
│  Cart item 1            🗑   │  reuse existing cart item composables
│  Cart item 2            🗑   │
│                              │
├──────────────────────────────┤
│      [ Check out ]           │  existing checkout button
└──────────────────────────────┘
```

**Reused:** Nearly everything from existing `WooPosCartScreen` - cart item rows, toolbar (back button, title, count, clear all), checkout button. The existing composable already renders a full-height column. On phone just render it at full width instead of 35%.

**New:** Minimal - likely a thin wrapper passing a full-width modifier.

### 5. Totals/Payment Screen (Phone)

Full-screen totals, navigated to after tapping "Check out".

```
┌──────────────────────────────┐
│                              │
│     [Reader status icon]     │  disconnected/connecting/ready
│     Reader not connected     │
│     Connect to reader        │
│                              │
├──────────────────────────────┤
│  Subtotal           $196.66  │
│  Taxes                $0.00  │
│  ─────────────────────────── │  reuse TotalsGrid
│  Total              $196.66  │
│                              │
├──────────────────────────────┤
│      [ Cash payment ]        │  outlined button
└──────────────────────────────┘
```

**Card payment flow:** When reader is connected, the reader status area shows "Tap, insert, or swipe" instructions and payment processing animation. This happens within the totals screen (same as tablet where it happens within the totals panel). No back navigation during payment processing - same as tablet where FullScreenTotals hides the cart.

**Payment success:** Shows full-screen success with "New order" and "Email receipt" buttons (see `tablet-06-card-payment-success.png`). Already full-screen on tablet, works the same on phone.

**Cash payment:** Navigates to `CASH_PAYMENT_ROUTE` - a separate full-screen destination in the root navigation graph. No changes needed, works the same on phone and tablet.

**Reused:** `WooPosTotalsScreen` already renders a full-height column. Render at full width.

**New:** System back for navigation (handled by NavController). No custom back arrow needed.

**Known dimension issues:** Several existing composables have hardcoded dimensions designed for tablet width that will break on phone (~360-412dp):
- `WooPosPaymentSuccessScreen`: buttons hardcoded at `.width(604.dp)` — will overflow phone screen. Change to `fillMaxWidth()` with horizontal padding on phone.
- `WooPosPaymentFailedScreen`: same `.width(604.dp)` issue on both action buttons.
- `TotalsGrid`: uses `.fillMaxWidth(0.5f)` which gives ~180dp on phone — too narrow for label + value pairs. Use full width on phone.
- `TotalsLoading` shimmer: `.width(332.dp)` may clip on smaller phones. Use `fillMaxWidth()` with padding.
- `ReaderDisconnected` "Connect to a reader" button: `.fillMaxWidth(0.5f)` gives ~180dp on phone. Use wider fraction or full width.
- Reader status area (256dp Lottie animation) takes most visible height on phone. Users may need to scroll to see totals and payment button. Consider reducing animation size on phone or making the column scrollable (it already uses `verticalScroll`).

### 6. Navigation & State Mapping

The existing `WooPosHomeViewModel` state machine drives both tablet and phone:

| Tablet state | Tablet behavior | Phone behavior |
|---|---|---|
| `ScreenPositionState.Cart` | Horizontal scroll to 0 (products + cart visible) | Show products screen |
| `Checkout.CartWithTotals` | Scroll to end (cart + totals visible) | Navigate to totals screen |
| `Checkout.FullScreenTotals` | Totals expand full width | Stay on totals screen |
| `BackFromCheckoutToCartClicked` | Scroll back | Navigate back to cart |
| `OrderSuccessfullyPaid` | Reset to cart state | Pop back to products screen |

**Back navigation on phone:** The ViewModel is the single source of truth for screen position. Phone routes use `BackHandler` on each route to call back into `WooPosHomeViewModel.onUIEvent(SystemBackClicked)`. The ViewModel state change is then observed by `WooPosHomePhoneScreen`, which drives `NavController.navigate(...)`. NavController does NOT autonomously handle back — each route intercepts back presses and routes them through the ViewModel, same as the tablet's `BackHandler` in `WooPosHomeScreen`. This keeps ViewModel state in sync with the displayed screen and ensures child ViewModels receive the correct events (e.g., `BackFromCheckoutToCartClicked` resets `WooPosTotalsViewModel`).

**Empty cart after product removal:** When all products are removed from the cart while on the totals screen, `WooPosTotalsViewModel` sends `BackFromCheckoutToCartClicked`, which navigates phone back to cart. If the cart is empty, the cart screen should handle this gracefully — either auto-navigate back to products or show an empty state with a "Browse products" action.

### SharedFlow Event Delivery

The parent-child communication uses `MutableSharedFlow()` with `replay=0` and `extraBufferCapacity=0`. On tablet, all three child screens are composed simultaneously and always collecting. On phone, only one screen is composed at a time via NavHost.

This means events could be dropped if a subscriber is not yet collecting. Mitigation: verify that all child ViewModels (`WooPosCartViewModel`, `WooPosTotalsViewModel`, `WooPosItemsViewModel`) start collecting events in their `init` blocks (via `viewModelScope.launch`), NOT in composable `LaunchedEffect`. Since ViewModels are scoped to the activity (via `@ActivityRetainedScoped` event bus) and survive NavHost route changes, their collectors stay active regardless of which screen is composed. If any ViewModel starts collection in a composable, it must be moved to `init`.

## Files Changed

**Modified:**

| File | Change |
|------|--------|
| `WooPosActivity` | Set portrait orientation for phones |
| `WooPosIsScreenSizeAllowed` | Allow phone screen sizes (`MIN_SCREEN_SHORT_SIZE_DP = 320`, `MIN_SCREEN_LONG_SIZE_DP = 480`) |
| `WooPosRootScreen` / `WooPosRootHost` | Branch to phone home screen |
| `WooPosCardReaderActivity` | Set portrait orientation on phones (currently hardcodes landscape) |
| `WooPosCouponCreationActivity` | Set portrait orientation on phones (currently hardcodes landscape) |

**New:**

| File | Purpose |
|------|---------|
| `WooPosHomePhoneScreen` | Phone home with NavHost (products → cart → totals) |
| `WooPosPhoneProductsScreen` | Products with menu button, tabs, always-visible search, cart button |
| `WooPosPhoneCartScreen` | Thin wrapper around existing cart composables at full width |
| `WooPosPhoneTotalsScreen` | Thin wrapper around existing totals composables at full width |

**Untouched:**

- `WooPosHomeScreen` (tablet layout)
- `WooPosHomeViewModel` and all child ViewModels
- All child composables (product list, cart items, totals grid, menu popup items)
- Parent-child communication events
- All navigation routes for cash/card payment, settings, orders, bookings
- Analytics, card reader integration, data layer

**Note on Settings/Orders/Bookings screens:** These screens use two-pane landscape layouts (`Row { Column(.weight(0.3f)) ... Column(.weight(0.7f)) }`). On phone portrait, the panes will be narrow but functional. Phone-optimized layouts for these screens are out of scope for this iteration — they are secondary screens accessed from the menu, not the core POS flow.

## Risk Assessment

**Low-Medium.** Five existing files are modified, and changes are small (orientation checks, screen size thresholds, one branch point). All phone code is new/additive. Tablet flow is untouched and can be verified with the saved screenshots as reference.

**Key risks to watch:**
- Back navigation sync between NavController and ViewModel state (see Section 6)
- SharedFlow event delivery timing when phone screens are composed one at a time (see SharedFlow section)
- Hardcoded dimension values in totals/payment composables that overflow phone width (see Section 5)

**Foldables:** Foldable devices may ignore `requestedOrientation`. Mid-session fold/unfold causes Activity recreation. Foldable support is not in scope for v1 — these devices will use whichever layout matches their current screen size at Activity creation time.

## On-Device Smoke Test (mcp-mobile)

After all implementation is done, run this smoke test on a phone emulator using mcp-mobile tools. The goal is to verify that the full cash and card payment flows work end-to-end on a phone.

### Setup

1. Build and install the debug APK on a phone emulator (e.g. Pixel 6, API 34)
2. Launch the app via deeplink to auto-login:
   ```
   https://woocommerce.com/mobile/auto-login?site=https%3A%2F%2Fpressable-kidinov.mystagingwebsite.com%2F&user=andrey.kidinov%40automattic.com&password=m*Nwg2igdCddQD@NGj@c
   ```
3. Navigate to the POS tab in the bottom navigation bar

### Test 1: Products Screen Layout

1. Take a screenshot of the products screen
2. Verify:
   - Menu button (hamburger) is visible at the top left
   - "Products" and "Coupons" tabs are visible
   - Search bar is always visible below tabs
   - Product list is scrollable and fills the screen width
   - No floating toolbar is shown

### Test 2: Cart Flow - Add Items

1. Tap on 2-3 different products to add them to the cart
2. Verify the cart button appears at the bottom with the correct item count and total
3. Take a screenshot showing the cart button
4. Tap the cart button to navigate to the cart screen

### Test 3: Cart Screen

1. Take a screenshot of the cart screen
2. Verify:
   - Back arrow is in the top left
   - Title says "Cart" with item count
   - Cart items are listed with delete buttons
   - "Check out" button is at the bottom
3. Tap back to return to products, then re-open cart to confirm navigation works both ways

### Test 4: Cash Payment Flow

1. From the cart screen, tap "Check out"
2. Take a screenshot of the totals screen - verify subtotal, taxes, total are shown
3. Tap "Cash payment" button
4. On the cash payment screen, confirm the amount and complete the cash payment
5. Verify the payment success screen appears with "New order" and "Email receipt" buttons
6. Take a screenshot of the success screen
7. Tap "New order" - verify it returns to the products screen with an empty cart

### Test 5: Second Cash Payment (Full Round Trip)

1. Add 1-2 products to the cart again
2. Tap cart button → tap "Check out" → tap "Cash payment" → complete payment
3. Verify success screen appears again
4. Tap "New order" to return to products
5. This confirms the full cycle works repeatedly without getting stuck

### Test 6: Menu Button

1. From the products screen, tap the menu button (hamburger icon)
2. Verify the menu popup shows: Settings, Orders, Bookings, Exit POS
3. Take a screenshot of the menu
4. Dismiss the menu without selecting anything

### Test 7: Back Navigation Edge Cases

1. Add items to cart, tap cart button, tap "Check out" to reach totals screen
2. Press system back — verify return to cart screen with correct state (items still there, checkout button visible)
3. Press system back again — verify return to products screen with cart button still showing correct count
4. Tap cart button → "Check out" again — verify totals screen loads correctly (no stale state)

### Test 8: Tablet Regression

1. Re-run Tests 1-6 on a tablet emulator (e.g. Pixel Tablet, API 34) to verify no regressions
2. Verify side-by-side layout is unchanged: Products (65%) | Cart (35%)
3. Verify floating toolbar is present at bottom-left with reader status
4. Complete one cash payment end-to-end on tablet

### Pass Criteria

All 8 tests pass. Every screen transition is smooth, no crashes, and the app returns to a clean state after each payment. Specific checks: correct title/button visible on each screen, cart item count carries over between screens, back stack is clean after a full round-trip (pressing back from products after completing a payment should show exit confirmation, not a blank screen).