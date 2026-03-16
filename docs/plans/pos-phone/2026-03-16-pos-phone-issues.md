# POS Phone Support - Issues & Fixes

Issues found after on-device testing (Pixel 6 emulator, CPH2671 physical device) and the fixes applied.

## Issue 1: Menu button hidden behind tabs (Critical) - FIXED

**Problem:** The menu button was overlaid at TopStart but `WooPosItemsScreen`'s tabs covered it. Also, no popup menu was rendered on phone.

**Fix:** Rewrote `WooPosPhoneProductsScreen` with a custom compact toolbar that renders [Menu button] [Tabs] [Search icon] in one row. No longer uses `WooPosItemsScreen` — instead composes `WooPosItemsViewModel` state directly with `WooPosProductsScreen`, `WooPosCouponsScreen`, and search screens. Added `PhonePopUpMenu` composable with `WooPosBackgroundOverlay` that observes toolbar ViewModel state.

## Issue 2: Back from totals skips cart (Major) - FIXED

**Problem:** Pressing back on totals screen popped the entire back stack to products, skipping cart.

**Fix:** Track previous `screenPositionState` in `WooPosHomePhoneContent`. When transitioning from `CartWithTotals` to `Cart` (user pressed back), pop only one step (totals -> cart). When transitioning from `FullScreenTotals` to `Cart` (payment completed + new order), pop all the way to products.

## Issue 3: Search bar not always visible (Major) - DEFERRED

**Problem:** Search is a toggle icon, not a persistent search bar below tabs.

**Status:** Deferred to follow-up iteration. The search toggle works for v1. The custom phone toolbar renders the search icon in the same row as menu + tabs, and clicking it opens the full search overlay.

## Issue 4: Cart button doesn't show total price (Minor) - DEFERRED

**Problem:** Cart button shows "Cart (2)" without the total price.

**Status:** Deferred. The cart ViewModel doesn't expose a formatted total string. Would require either parsing formatted price strings or adding a total field to `WooPosCartState`. Item count is sufficient for v1.

## Issue 5: Cart screen has no back arrow (Minor) - ACCEPTED

**Problem:** No visible back arrow on the phone cart screen. The existing cart toolbar shows the back arrow only during checkout status, not in editable state.

**Status:** Accepted for v1. System back gesture/button works. An overlay back button was tried but overlapped with the "Cart" title text. Clean fix would require modifying `WooPosCartViewModel` to detect phone mode, which is a larger change.

## Issue 6: Dimension changes affect tablet layout (Major) - FIXED

**Problem:** Replacing `.width(604.dp)` and `.fillMaxWidth(0.5f)` with `.fillMaxWidth()` globally broke tablet layout.

**Fix:** Applied `widthIn(max = 604.dp).fillMaxWidth()` pattern for buttons and `widthIn(max = 450.dp).fillMaxWidth()` for grids/summaries. This caps width on tablet while being responsive on phone. Fixed in: `WooPosTotalsScreen`, `WooPosTotalsPaymentSuccessScreen`, `WooPosTotalsPaymentFailedScreen`, `WooPosPaymentSuccessScreen`, `WooPosCardPaymentScreen`.

## Issue 7: Totals screen loads forever on first checkout (Critical) - FIXED

**Problem:** On phone, only one screen is composed at a time via NavHost. When checkout is clicked, `CheckoutClicked` event is sent via SharedFlow before `WooPosTotalsViewModel` exists. The event is dropped, and totals screen stays on shimmer indefinitely. Works on second attempt because ViewModel already exists.

**Fix:** Eagerly create all child ViewModels (`WooPosTotalsViewModel`, `WooPosItemsViewModel`, `WooPosHomeFloatingToolbarViewModel`) in `WooPosHomePhoneContent` using `hiltViewModel()`. This ensures they start collecting SharedFlow events immediately, same as tablet where all screens are composed simultaneously.

## Issue 8: Fonts too big for phone (Major) - FIXED

**Problem:** `WooPosTypography` used fixed font sizes (e.g., Heading at 36sp) designed for tablet. On phone, tabs and text were oversized.

**Fix:** Made `WooPosTypography` adaptive, following the same pattern as `WooPosSpacing`. Added `toAdaptiveTextStyle()` that scales font sizes based on screen size:
- < 880dp longest side: 0.65x
- 880-1200dp: 0.85x (Pixel 6 falls here)
- >= 1200dp: 1.0x (tablet, unchanged)

Added 14sp minimum floor to prevent small text (BodySmall, Caption) from becoming unreadable.

## Issue 9: Buttons too big for phone (Minor) - FIXED

**Problem:** `WooPosButton` and `WooPosOutlinedButton` used hardcoded 80dp height.

**Fix:** Added `Dp.toAdaptiveComponentSize()` to the design system (gentler scaling: 0.85x for 880-1200dp, 0.7x for <880dp). Button heights now use `80.dp.toAdaptiveComponentSize()` (~68dp on Pixel 6). Also removed hardcoded `.height(80.dp)` from the Connect Reader button in totals screen.

## Issue 10: Product/cart cards too big for phone (Minor) - FIXED

**Problem:** Cart item images (96dp) and product list items (112dp) had fixed sizes.

**Fix:** Applied `toAdaptiveComponentSize()` to image sizes in `WooPosCartScreen` (96dp -> ~82dp on phone) and `WooPosItemsList` (112dp -> ~95dp on phone). Tablet sizes unchanged at 1.0x.

## Issue 11: Search button needs right margin (Minor) - FIXED

**Problem:** Search icon and open search input field extended to the right edge without margin.

**Fix:** Added `Spacer(WooPosSpacing.Medium)` after the search icon in the toolbar row, and `padding(end = WooPosSpacing.Medium)` on the open search input.

## Summary

| # | Issue | Severity | Status |
|---|-------|----------|--------|
| 1 | Menu button hidden + no popup | Critical | Fixed |
| 7 | Totals loading forever on first checkout | Critical | Fixed |
| 2 | Back from totals skips cart | Major | Fixed |
| 6 | Dimension changes break tablet | Major | Fixed |
| 8 | Fonts too big for phone | Major | Fixed |
| 9 | Buttons too big for phone | Minor | Fixed |
| 10 | Product/cart cards too big | Minor | Fixed |
| 11 | Search button right margin | Minor | Fixed |
| 3 | Search bar not always visible | Major | Deferred |
| 4 | Cart button no total price | Minor | Deferred |
| 5 | Cart screen no back arrow | Minor | Accepted |

## Files Changed (Final)

**New (4):**
- `WooPosHomePhoneScreen` — phone NavHost with products/cart/totals routes, ViewModel-driven navigation, eager ViewModel creation
- `WooPosPhoneProductsScreen` — custom compact toolbar (menu + tabs + search), popup menu, floating cart button, content crossfade
- `WooPosPhoneCartScreen` — thin wrapper around `WooPosCartScreen`
- `WooPosPhoneTotalsScreen` — thin wrapper around `WooPosTotalsScreen`

**Modified - Phone support (6):**
- `WooPosActivity` — portrait orientation for phones
- `WooPosCardReaderActivity` — portrait orientation for phones
- `WooPosCouponCreationActivity` — portrait orientation for phones
- `WooPosIsScreenSizeAllowed` — lower thresholds (320dp/480dp)
- `WooPosHomeNavigation` — branch phone vs tablet at home screen
- `WooPosContextExt` — `isWooPosPhoneLayout()` helper

**Modified - Adaptive design system (4):**
- `WooPosTypography` — adaptive font sizes with `toAdaptiveTextStyle()`, 14sp floor
- `WooPosSizes` — new `toAdaptiveComponentSize()` function
- `WooPosButtons` — adaptive button heights
- `WooPosItemsList` — adaptive product card sizes

**Modified - Responsive dimensions (6):**
- `WooPosCartScreen` — adaptive cart item image sizes
- `WooPosTotalsScreen` — `widthIn(max)` for grid/button, adaptive sizes
- `WooPosTotalsPaymentSuccessScreen` — `widthIn(max = 604.dp)` for buttons
- `WooPosTotalsPaymentFailedScreen` — `widthIn(max = 604.dp)` for buttons
- `WooPosPaymentSuccessScreen` — `widthIn(max = 604.dp)` for buttons
- `WooPosCardPaymentScreen` — `widthIn(max = 604.dp)` for buttons, adaptive summary
