# POS Phone - Full UX/UI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Complete the phone POS prototype with polished UX/UI across all screens - home flow, orders, bookings, settings, and shared components - suitable for team presentation and later iOS port.

**Architecture:** Build on the existing `pos-phone` branch. The home screen flow (products/cart/totals) is already working. This plan adds: (1) component-level fixes for phone sizing, (2) UX improvements to the home flow, (3) a reusable master-detail component, (4) phone adaptation for Orders/Bookings/Settings screens.

**Tech Stack:** Kotlin, Jetpack Compose, Hilt, Material 3, existing WooPos design system (`WooPosTypography`, `WooPosSpacing`, `WooPosSizes`)

**Key UX Decisions (agreed during brainstorming):**
- Hamburger menu - no bottom tabs. Maximize screen space for the payment flow
- Full-screen cart navigation - clear linear flow (Products -> Cart -> Checkout), no bottom sheet
- Cart button on products screen should visually match the checkout button (panel background)
- Quantity controls on product tiles when added to cart (phone-only, since cart is not visible)
- Reusable master-detail component for Orders/Bookings/Settings
- Orders/Bookings/Settings need ViewModel refactoring: split into Master VM + List VM + Detail VM

---

## Phase 0: Merge Card Reader Connection UI v2 Branch

This must be done first since the branch changes `WooPosDialogWrapper` (adds `widthFraction` parameter) and other components that later tasks depend on.

### Task 1: Merge woomob-1854-woopos-card-reader-connection-ui-v2 and adapt for phone

**Branch:** `woomob-1854-woopos-card-reader-connection-ui-v2`

This branch replaces the old `WooPosCardReaderActivity` (a separate activity for card reader connection) with a new in-POS connection dialog system. Key changes:

**What the branch adds:**
- `WooPosCardReaderConnectionDialog.kt` (1218 lines) - full connection flow as a Compose dialog inside POS: scanning, reader found, connecting, connected, bluetooth/location disabled, permission requests, update required/optional/complete, error states, onboarding errors
- `WooPosCardReaderConnectionController.kt` (553 lines) - orchestrates the connection flow state machine
- `WooPosCardReaderConnectionControllerFactory.kt` - DI factory
- `WooPosCardReaderConnectionViewModel.kt` - thin ViewModel
- `WooPosCardReaderConnectionState.kt` - 18+ sealed interface states
- `WooPosOnboardingErrorMapper.kt` - maps onboarding errors to UI
- `WooPosLoadingIndicators.kt` - new `WooPosUpdateProgressIndicator` component
- `WooPosIcons.kt` (411 lines) - custom vector icons for reader states
- New `DialogState.CardReaderConnectionDialog` added to `WooPosHomeState`
- `WooPosDialogWrapper` now accepts a `widthFraction` parameter (was hardcoded 0.75f)
- Deletes old `WooPosCardReaderActivity`, replaces with `WooPosCardReaderOnboardingActivity`
- Adds battery warning icon/status to floating toolbar
- Adds `Reconnecting` state to `WooPosCardReaderStatus`

- [ ] **Step 1: Merge the branch into pos-phone**

```bash
git merge origin/woomob-1854-woopos-card-reader-connection-ui-v2
```

Resolve any conflicts (likely in `WooPosHomeState.kt`, `WooPosHomeScreen.kt` where both branches added dialog states).

- [ ] **Step 2: Make connection dialog width adaptive**

The branch uses `widthFraction = 0.55f` for the connection dialog. On tablet (~1000dp) that's ~550dp - good. On phone (360dp) that's ~198dp - too narrow. Update `WooPosCardReaderConnectionDialogContent` to use a wider fraction on phone:

```kotlin
val configuration = LocalConfiguration.current
val shortSide = minOf(configuration.screenWidthDp, configuration.screenHeightDp)
val dialogFraction = if (shortSide < 674) 0.92f else 0.55f

WooPosDialogWrapper(
    ...
    widthFraction = dialogFraction,
    ...
)
```

- [ ] **Step 3: Make connection dialog icon sizes adaptive**

The dialog uses hardcoded sizes:
- `Modifier.size(width = 160.dp, height = 143.dp)` - card reader illustration
- `Modifier.size(120.dp)` - update progress indicators (2 occurrences)
- `Modifier.size(140.dp)` - update completed indicator
- `.height(80.dp)` - loading row in `MultipleReadersFoundContent`

Apply `toAdaptiveComponentSize()` to these values.

- [ ] **Step 4: Wire the connection dialog into WooPosHomePhoneScreen**

The tablet home screen shows the dialog via `Dialogs()` when `dialogState is DialogState.CardReaderConnectionDialog`. The phone home screen has its own `PhoneDialogs()` in `WooPosHomePhoneScreen.kt`. Add the same dialog handling:

```kotlin
if (dialogState is WooPosHomeState.DialogState.CardReaderConnectionDialog) {
    WooPosCardReaderConnectionDialog(
        onDismiss = { onHomeUIEvent(WooPosHomeUIEvent.DismissCardReaderConnectionDialog) },
        onConnectionSuccess = { onHomeUIEvent(WooPosHomeUIEvent.DismissCardReaderConnectionDialog) }
    )
}
```

- [ ] **Step 5: Verify the floating toolbar battery warning on phone**

The branch adds battery warning icons to the floating toolbar. On phone, the floating toolbar is replaced by the hamburger menu popup. Check if battery/connection warnings need to be surfaced in the phone menu or if they can be skipped (since the connection dialog handles everything).

- [ ] **Step 6: Verify on phone and tablet** - trigger card reader connection from the menu. All dialog states should be readable and buttons should be tappable on both form factors.

- [ ] **Step 7: Commit and add to git**

---

## Phase 1: Bug Fixes & Component-Level Improvements

These are foundational fixes that affect multiple screens. Do these first so all subsequent work benefits.

### Task 2: Fix variations back navigation on phone (bug)

**Files:**
- Modify: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/home/phone/WooPosPhoneProductsScreen.kt:303-306`

The `onBackClicked` lambda in `PhoneItemsContent` for the `Variations` case is an empty lambda `{}`. System back does nothing when viewing variations on phone.

- [ ] **Step 1: Fix the empty lambda**

In `PhoneItemsContent`, the `Variations` branch at line 303 passes `onBackClicked = {}`. This needs to delegate to the items ViewModel, same as the toolbar back button does. However, `PhoneItemsContent` doesn't have access to the ViewModel's `onUIEvent`.

The fix: add an `onBackFromVariations` parameter to `PhoneItemsContent` and wire it through.

```kotlin
// In PhoneItemsContent signature, add:
onBackFromVariations: () -> Unit,

// In the Variations branch:
is PhoneScreenState.Variations -> WooPosVariationsScreen(
    modifier = Modifier.padding(horizontal = WooPosSpacing.Medium.value),
    variableProductData = screenState.variableProductData,
    onBackClicked = onBackFromVariations,
)
```

At the call site in `WooPosPhoneProductsScreen`, pass:
```kotlin
PhoneItemsContent(
    ...
    onBackFromVariations = {
        itemsViewModel.onUIEvent(WooPosItemsUIEvent.BackFromVariationsClicked)
    },
)
```

- [ ] **Step 2: Verify on device** - open a variable product, see variations, press back. Should return to product list.

- [ ] **Step 3: Commit and add to git**

---

### Task 3: Make WooPosDialogWrapper default width adaptive for phone

**Files:**
- Modify: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/common/composeui/component/WooPosDialogWrapper.kt`

**Prerequisite:** Task 1 (merge) must be done first. After the merge, `WooPosDialogWrapper` accepts a `widthFraction` parameter with default `0.75f`. Change the default to be adaptive:

- [ ] **Step 1: Make the default width fraction adaptive**

```kotlin
@Composable
fun WooPosDialogWrapper(
    ...
    widthFraction: Float = defaultDialogWidthFraction(),
    ...
)

@Composable
private fun defaultDialogWidthFraction(): Float {
    val configuration = LocalConfiguration.current
    val shortSide = minOf(configuration.screenWidthDp, configuration.screenHeightDp)
    return if (shortSide < 674) 0.92f else 0.75f
}
```

This makes all dialogs (scanning setup, exit confirmation) phone-adaptive by default. The connection dialog still overrides to `0.55f` on tablet (handled in Task 1 Step 2).

- [ ] **Step 2: Verify on device** - open any dialog (e.g. barcode scanner setup from Settings). Should be wider on phone.

- [ ] **Step 3: Commit and add to git**

---

### Task 4: Make WooPosErrorAndEmptyStateButtonModifier adaptive

**Files:**
- Modify: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/common/composeui/component/WooPosErrorAndEmptyStateButtonModifier.kt`

Currently `fillMaxWidth(0.5f).height(80.dp)`. On phone, 50% width = ~180dp (too narrow) and 80dp height is too tall.

- [ ] **Step 1: Convert to a composable function**

The current `val` modifier can't read composable state. Convert to a `@Composable` function:

```kotlin
@Composable
fun wooPosErrorAndEmptyStateButtonModifier(): Modifier {
    val configuration = LocalConfiguration.current
    val shortSide = minOf(configuration.screenWidthDp, configuration.screenHeightDp)
    val isPhone = shortSide < 674
    return if (isPhone) {
        Modifier
            .fillMaxWidth()
            .height(80.dp.toAdaptiveComponentSize())
    } else {
        Modifier
            .fillMaxWidth(0.5f)
            .height(80.dp)
    }
}

// Keep the old val for backward compatibility during migration:
val WooPosErrorAndEmptyStateButtonModifier = Modifier
    .fillMaxWidth(0.5f)
    .height(80.dp)
```

- [ ] **Step 2: Update all usages** to call `wooPosErrorAndEmptyStateButtonModifier()`. Search for `WooPosErrorAndEmptyStateButtonModifier` in `WooPosErrorScreen.kt`, `WooPosEmptyScreen.kt`, and any other files. Remove the old `val` in the same commit since all usages are internal.

- [ ] **Step 3: Verify on device** - trigger an error state (e.g. disconnect network). Button should fill width on phone.

- [ ] **Step 4: Commit and add to git**

---

### Task 5: Fix variations error hardcoded 640dp width

**Files:**
- Modify: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/home/items/variations/WooPosVariationsScreen.kt`

The error state uses `Modifier.width(640.dp)` which overflows on phone.

- [ ] **Step 1: Replace with constrained width**

```kotlin
// Change:
Modifier.width(640.dp)
// To:
Modifier.widthIn(max = 640.dp).fillMaxWidth()
```

- [ ] **Step 2: Commit and add to git**

---

### Task 6: Make Lottie animations and illustrations adaptive

**Files:**
- Modify: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/cardpayment/WooPosCardPaymentScreen.kt`
- Modify: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/home/totals/WooPosTotalsScreen.kt`
- Modify: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/home/totals/payment/inprogress/WooPosTotalsPaymentInProgressScreen.kt`

Lottie animations use hardcoded `256.dp` and loading spinners `160.dp`. On a 360dp phone, 256dp takes 71% of width.

- [ ] **Step 1: Apply adaptive sizing to all Lottie animation sizes**

Replace `Modifier.size(256.dp)` with `Modifier.size(256.dp.toAdaptiveComponentSize())`.
Replace `Modifier.size(160.dp)` with `Modifier.size(160.dp.toAdaptiveComponentSize())`.

Apply this in all payment screen files where these sizes appear.

- [ ] **Step 2: Verify on device** - go through card payment flow. Animations should be proportionally smaller on phone.

- [ ] **Step 3: Commit and add to git**

---

### Task 7: Fix payment success screen ConstraintLayout width

**Files:**
- Modify: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/home/totals/payment/success/WooPosTotalsPaymentSuccessScreen.kt`
- Modify: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/paymentsuccess/WooPosPaymentSuccessScreen.kt`

Both success screens use `ConstraintLayout` without `fillMaxWidth()`, so buttons inside don't size properly.

- [ ] **Step 1: Add fillMaxWidth to ConstraintLayout**

In both files, add `.fillMaxWidth()` to the ConstraintLayout modifier.

- [ ] **Step 2: Verify on device** - complete a payment. Success buttons should fill width properly on phone.

- [ ] **Step 3: Commit and add to git**

---

### Task 8: Add max width to phone popup menu

**Files:**
- Modify: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/home/phone/WooPosPhoneProductsScreen.kt:359-361`

The `PhonePopUpMenu` uses `width(IntrinsicSize.Max)` with no upper bound. Long localized strings could overflow.

- [ ] **Step 1: Add widthIn constraint**

```kotlin
WooPosCard(
    modifier = modifier
        .widthIn(max = 280.dp)
        .width(IntrinsicSize.Max),
    ...
)
```

- [ ] **Step 2: Commit and add to git**

---

## Phase 2: Home Flow UX Improvements

Polish the core Products -> Cart -> Checkout flow.

### Task 9: Cart button with bottom panel background and nav bar padding

**Files:**
- Modify: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/home/phone/WooPosPhoneProductsScreen.kt:160-174`

The cart button currently floats over the product list with no background and no `navigationBarsPadding()`. It should have a surface panel underneath (matching the checkout button's visual treatment on the cart screen) and respect the gesture navigation bar.

- [ ] **Step 1: Add a surface panel behind the cart button with nav bar padding**

Replace the current `AnimatedVisibility` + `PhoneCartButton` block with a version that has a background surface and navigation bar padding:

```kotlin
AnimatedVisibility(
    visible = cartItemCount > 0,
    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
    modifier = Modifier.align(Alignment.BottomCenter)
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceBright,
        modifier = Modifier.fillMaxWidth()
    ) {
        PhoneCartButton(
            itemCount = cartItemCount,
            onClick = onCartClicked,
            modifier = Modifier
                .fillMaxWidth()
                .padding(WooPosSpacing.Medium.value)
                .navigationBarsPadding()
        )
    }
}
```

Add imports: `import androidx.compose.material3.Surface`, `import androidx.compose.foundation.layout.navigationBarsPadding`

The `surfaceBright` color matches what the cart screen uses for its background (see `WooPosCartScreen` line 113).

- [ ] **Step 2: Verify on device** - cart button should sit on a white/light panel strip at the bottom, above the gesture navigation bar. Visually compare with the checkout button on the cart screen - they should look the same.

- [ ] **Step 3: Commit and add to git**

---

### Task 10: Add back button to cart screen on phone

**Files:**
- Modify: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/home/cart/WooPosCartViewModel.kt:549-555`
- Modify: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/home/cart/WooPosCartScreen.kt:94-101`
- Modify: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/home/phone/WooPosPhoneCartScreen.kt`

Currently the cart toolbar shows the back arrow only during `CHECKOUT` status (line 560). On phone, the back arrow should also be visible during `EDITABLE` status since the cart is a separate screen.

- [ ] **Step 1: Add phone-aware back button logic**

Option A (simpler, recommended): Pass a phone back click handler to `WooPosCartScreen` and show it when provided.

Add `onPhoneBackClick` parameter to `WooPosCartScreen`:

```kotlin
// WooPosCartScreen.kt - public composable
@Composable
fun WooPosCartScreen(
    modifier: Modifier = Modifier,
    onPhoneBackClick: (() -> Unit)? = null,
) {
    val viewModel: WooPosCartViewModel = hiltViewModel()
    viewModel.state.observeAsState().value?.let {
        WooPosCartScreen(modifier, it, viewModel::onUIEvent, onPhoneBackClick)
    }
}
```

In the private `WooPosCartScreen`, pass `onPhoneBackClick` to `CartToolbar`. When `onPhoneBackClick` is not null, force `backIconVisible = true` regardless of cart status, and use `onPhoneBackClick` for the back action in EDITABLE state (the existing `onBackClicked` from `WooPosCartUIEvent.BackToProductsClicked` still handles CHECKOUT back).

```kotlin
// In CartToolbar, show back button when:
// - toolbar.backIconVisible (existing: checkout status on tablet)
// - OR onPhoneBackClick is not null (phone: always show)
val showBackButton = toolbar.backIconVisible || onPhoneBackClick != null
```

Wire the back action:
```kotlin
// If in editable state and phone, use onPhoneBackClick
// If in checkout state, use existing onBackClicked
val backAction = if (!toolbar.backIconVisible && onPhoneBackClick != null) {
    onPhoneBackClick
} else {
    onBackClicked
}
```

- [ ] **Step 2: Update WooPosPhoneCartScreen to pass onPhoneBackClick**

```kotlin
@Composable
fun WooPosPhoneCartScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WooPosCartScreen(
        modifier = modifier.fillMaxSize(),
        onPhoneBackClick = onBackClick,
    )
}
```

- [ ] **Step 3: Update WooPosHomePhoneScreen** to pass the back handler.

Note: `WooPosHomePhoneScreen` already has a `BackHandler { navController.popBackStack() }` for the cart route that handles system back. The `onBackClick` is for the visible toolbar arrow only - not a duplicate:

```kotlin
composable(PHONE_CART_ROUTE) {
    CompositionLocalProvider(
        LocalViewModelStoreOwner provides parentViewModelStoreOwner
    ) {
        BackHandler {
            navController.popBackStack()
        }
        WooPosPhoneCartScreen(
            onBackClick = { navController.popBackStack() }
        )
    }
}
```

- [ ] **Step 4: Verify on device** - cart screen should show back arrow in top-left. Tapping it goes back to products. During checkout status, back arrow still works (goes back to products via existing logic).

- [ ] **Step 5: Commit and add to git**

---

### Task 11: Quantity controls on product tiles (phone-only)

**Files:**
- Modify: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/home/items/products/WooPosProductsScreen.kt`
- Modify: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/home/phone/WooPosPhoneProductsScreen.kt`
- Possibly modify: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/home/items/WooPosItemsList.kt`

On phone, since the cart is not visible alongside products, tapping a product should show a quantity indicator on that product tile with minus/plus controls.

**Design:**
When a product is in the cart, overlay a small row on the product card: `[ - ]  2  [ + ]`
- Minus removes one from cart
- Plus adds one more
- The control appears on the right side of the product list item, replacing the price area or overlaying it
- When quantity goes to 0, the control disappears

**Implementation approach:**

The phone products screen needs to know which products are in the cart and their quantities. The `WooPosCartViewModel` already tracks cart items. The phone products screen already has access to it (via `cartItemCount` passed from `WooPosHomePhoneScreen`).

- [ ] **Step 1: Create a cart-aware product list item wrapper**

Create a map of `productId -> quantity` from the cart state and pass it to the products screen. In `WooPosHomePhoneScreen`, extract the cart items map:

```kotlin
val cartItemsMap: Map<Long, Int> = cartState.value?.body?.let { body ->
    when (body) {
        is WooPosCartState.Body.WithItems -> {
            body.itemsInCart
                .filterIsInstance<WooPosCartItemViewState.Product>()
                .groupBy { it.id }  // Note: the field is `id`, not `productId`
                .mapValues { (_, items) -> items.size }
        }
        else -> emptyMap()
    }
} ?: emptyMap()
```

Pass this to `WooPosPhoneProductsScreen` as a new parameter `cartProductQuantities: Map<Long, Int>`.

- [ ] **Step 2: Create a PhoneQuantityControl composable**

```kotlin
@Composable
private fun PhoneQuantityControl(
    quantity: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier,
)
```

This is a compact horizontal row: minus icon button, quantity text, plus icon button. Use `WooPosButtonSmall` style or custom small circular buttons.

- [ ] **Step 3: Integrate with product list items**

This is the trickiest part. The product list uses `WooPosItemsList` which renders items via `WooPosProductsScreen`. The quantity control needs to overlay or replace part of each product card when that product is in the cart.

Approach: pass the `cartProductQuantities` map and cart event handlers through to the product list rendering. On phone, each product item checks if `cartProductQuantities[productId] > 0` and shows the quantity control.

**Important note on events:** Decrementing uses `WooPosCartUIEvent.ItemRemovedFromCart(item)`. However, incrementing (adding one more of the same product) does NOT have a corresponding cart event - adding products flows through `WooPosItemsUIEvent` / the items ViewModel, not the cart ViewModel. The implementer needs to either: (a) re-trigger the same "product tapped" event through the items ViewModel to add another, or (b) add a new `ItemIncrementedInCart` event to `WooPosCartUIEvent`. Option (a) is simpler and reuses existing logic.

- [ ] **Step 4: Verify on device** - add a product. The product tile should show quantity controls. Tap plus to add more. Tap minus to reduce. When quantity hits 0, controls disappear.

- [ ] **Step 5: Commit and add to git**

**Note:** This task is the most complex in the plan. The exact implementation will depend on how the product list item composable is structured and how to thread the cart state through without breaking the tablet path. The implementer should read `WooPosItemsList.kt` and `WooPosProductsScreen.kt` carefully before starting. On tablet, this control should NOT appear (the cart pane is always visible).

---

## Phase 3: Reusable Master-Detail Component

Build a reusable adaptive layout component that shows two panes side-by-side on tablet and navigates between them on phone.

### Task 12: Create WooPosListDetailLayout component

**Files:**
- Create: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/common/composeui/component/WooPosListDetailLayout.kt`

This is the core reusable component used by Orders, Bookings, and Settings.

- [ ] **Step 1: Design the API**

```kotlin
@Composable
fun WooPosListDetailLayout(
    isDetailVisible: Boolean,
    onBackFromDetail: () -> Unit,
    listPane: @Composable () -> Unit,
    detailPane: @Composable () -> Unit,
    emptyDetailPane: @Composable () -> Unit = {},
    modifier: Modifier = Modifier,
    listWeight: Float = 0.3f,
    detailWeight: Float = 0.7f,
)
```

Parameters:
- `isDetailVisible` - whether a detail item is selected (drives phone navigation)
- `onBackFromDetail` - called when user navigates back from detail on phone
- `listPane` / `detailPane` - slot content
- `emptyDetailPane` - shown on tablet when nothing is selected
- `listWeight` / `detailWeight` - split ratio for tablet (default 30/70 matching current)

- [ ] **Step 2: Implement tablet path**

Use `LocalConfiguration.current` to detect phone vs tablet (consistent with existing adaptive helpers like `toAdaptiveComponentSize()`):
```kotlin
val configuration = LocalConfiguration.current
val isPhone = minOf(configuration.screenWidthDp, configuration.screenHeightDp) < 674
```

When `isPhone` is false (tablet):

```kotlin
Row(modifier = modifier.fillMaxSize()) {
    Box(modifier = Modifier.weight(listWeight)) {
        listPane()
    }
    Box(modifier = Modifier.weight(detailWeight)) {
        if (isDetailVisible) {
            detailPane()
        } else {
            emptyDetailPane()
        }
    }
}
```

This matches the current Orders/Bookings/Settings layout exactly.

- [ ] **Step 3: Implement phone path**

When `isPhone` is true:

```kotlin
// Use AnimatedContent with horizontal slide transition
BackHandler(enabled = isDetailVisible) {
    onBackFromDetail()
}

AnimatedContent(
    targetState = isDetailVisible,
    transitionSpec = {
        if (targetState) {
            // Navigating forward to detail
            (slideInHorizontally(initialOffsetX = { it }) + fadeIn()) togetherWith
                (slideOutHorizontally(targetOffsetX = { -it }) + fadeOut())
        } else {
            // Navigating back to list
            (slideInHorizontally(initialOffsetX = { -it }) + fadeIn()) togetherWith
                (slideOutHorizontally(targetOffsetX = { it }) + fadeOut())
        }
    },
    label = "list_detail_transition"
) { showDetail ->
    if (showDetail) {
        detailPane()
    } else {
        listPane()
    }
}
```

This uses the same horizontal slide animation as the existing POS navigation transitions in `WooPosSettingsDetailPaneScreen.kt:72-79`.

- [ ] **Step 4: Write basic test** - verify that on phone config the component shows list when `isDetailVisible = false` and detail when `true`.

- [ ] **Step 5: Commit and add to git**

---

## Phase 4: Adapt Orders, Bookings, Settings Screens for Phone

### Task 13: Adapt Orders screen for phone

**Files:**
- Modify: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/orders/WooPosOrdersViewModel.kt`
- Modify: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/orders/WooPosOrdersState.kt`
- Modify: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/orders/WooPosOrdersScreen.kt`

Currently `WooPosOrdersViewModel` manages both the list and detail state. The screen uses `isSingleOrderMode` to decide layout, but this is route-based not screen-size-based.

**Goal:** The ViewModel doesn't strictly need to be split into 3 VMs for v1 - the key change is making the screen composable use `WooPosListDetailLayout`. The ViewModel already exposes `state` with both list and detail data. The screen just needs to split its rendering into list slot and detail slot.

- [ ] **Step 1: Extract list pane content into a separate composable**

Extract the left-side order list (`OrdersListPane`) from `OrdersListWithDetails` into its own composable function that can be passed as a slot.

- [ ] **Step 2: Extract detail pane content into a separate composable**

Extract the right-side order details (`OrderDetailsPane`) into its own composable function.

- [ ] **Step 3: Replace the hardcoded Row layout**

Replace the current:
```kotlin
Row(modifier = Modifier.fillMaxSize()) {
    // list at weight(0.3f)
    // detail at weight(0.7f)
}
```

With:
```kotlin
// Note: the actual field is `selectedDetails` inside `WooPosOrdersState.Content`, not `selectedOrder`
WooPosListDetailLayout(
    isDetailVisible = (state as? WooPosOrdersState.Content)?.selectedDetails != null,
    onBackFromDetail = { viewModel.onBackFromDetail() },
    listPane = { OrdersListPane(...) },
    detailPane = { OrderDetailsPane(...) },
    emptyDetailPane = { OrdersEmptyDetailPlaceholder() },
)
```

- [ ] **Step 4: Handle `isSingleOrderMode`** - when navigating directly to a specific order (via `home/orders/{orderId}`), skip the list and show detail full-screen. This already works with `isSingleOrderMode` flag - keep that path as-is.

- [ ] **Step 5: Add `onBackFromDetail` to the ViewModel** - clear the selected order when back is pressed on phone. On tablet this is a no-op since both panes are visible.

- [ ] **Step 6: Verify on tablet** - orders screen should look exactly the same as before (30/70 split).

- [ ] **Step 7: Verify on phone** - orders screen should show list full-screen. Tapping an order slides to detail. Back slides to list.

- [ ] **Step 8: Run existing orders tests** to make sure nothing is broken:
```bash
./gradlew :WooCommerce:testWasabiDebugUnitTest --tests "*.WooPosOrdersViewModelTest"
```

- [ ] **Step 9: Commit and add to git**

---

### Task 14: Adapt Bookings with WooPosListDetailLayout

**Files:**
- Modify: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/bookings/WooPosBookingsScreen.kt`
- Modify: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/bookings/WooPosBookingsViewModel.kt`

Same pattern as Orders - extract list pane and detail pane, replace `Row(weight)` with `WooPosListDetailLayout`.

- [ ] **Step 1: Extract list pane composable** from `WooPosBookingsListPane`.

- [ ] **Step 2: Extract detail pane composable** from the detail `Box(Modifier.weight(0.7f))`.

- [ ] **Step 3: Replace Row layout with WooPosListDetailLayout**

```kotlin
// Note: the actual field is `selectedDetails` inside `WooPosBookingsState.Content`, not `selectedBooking`
WooPosListDetailLayout(
    isDetailVisible = (state as? WooPosBookingsState.Content)?.selectedDetails != null,
    onBackFromDetail = { viewModel.onBackFromDetail() },
    listPane = { BookingsListPane(...) },
    detailPane = { BookingsDetailPane(...) },
    emptyDetailPane = { BookingsEmptyDetailPlaceholder() },
)
```

**Note:** `WooPosBookingsLoadingScreen.kt` also has a hardcoded 30/70 `weight` split at lines 51/74. It should also be adapted or wrapped with `WooPosListDetailLayout` (on phone, show a simple centered loading spinner instead of the two-pane shimmer).

- [ ] **Step 4: Add `onBackFromDetail` to ViewModel** - clear selected booking.

- [ ] **Step 5: Verify on both tablet and phone.**

- [ ] **Step 6: Run existing bookings tests:**
```bash
./gradlew :WooCommerce:testWasabiDebugUnitTest --tests "*.WooPosBookingsViewModelTest"
```

- [ ] **Step 7: Commit and add to git**

---

### Task 15: Adapt Settings with WooPosListDetailLayout

**Files:**
- Modify: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/settings/WooPosSettingsScreen.kt`
- Modify: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/settings/WooPosSettingsViewModel.kt`

Settings is slightly different - the "list" is a categories list, and the "detail" pane uses `AnimatedContent` internally for nested navigation (e.g. Hardware -> Card Readers). The detail pane's internal navigation is already ViewModel-state-driven via `WooPosSettingsDetailDestination` and doesn't need changes.

- [ ] **Step 1: Replace Row layout with WooPosListDetailLayout**

Settings needs an `isDetailPaneOpen` state: on tablet it's always `true` (a category is always selected and both panes visible). On phone it starts `false` (showing categories list) and becomes `true` when a category is tapped, `false` when back is pressed.

```kotlin
WooPosListDetailLayout(
    isDetailVisible = state.isDetailPaneOpen,
    onBackFromDetail = { viewModel.onBackToCategories() },
    listPane = {
        WooPosSettingsCategoriesPaneScreen(
            selectedCategory = state.selectedCategory,
            onCategorySelected = { viewModel.onCategorySelected(it) },
        )
    },
    detailPane = {
        WooPosSettingsDetailPaneScreen(
            state = state,
            onNavigate = { viewModel.onNavigate(it) },
            onBack = { viewModel.onBack() },
            ...
        )
    },
)
```

- [ ] **Step 2: Add isDetailPaneOpen to Settings state and ViewModel**

```kotlin
// In WooPosSettingsState:
data class WooPosSettingsState(
    val selectedCategory: WooPosSettingsCategory = WooPosSettingsCategory.STORE,
    val currentDestination: WooPosSettingsDetailDestination = selectedCategory.rootDestination,
    val dialogState: WooPosSettingsDialogState = WooPosSettingsDialogState.Hidden,
    val isDetailPaneOpen: Boolean = false,
)
```

In the ViewModel:
- `onCategorySelected(category)` -> set `isDetailPaneOpen = true`
- `onBackToCategories()` -> set `isDetailPaneOpen = false`
- On init for tablet: set `isDetailPaneOpen = true` (category is pre-selected)

- [ ] **Step 3: Verify on tablet** - settings should look exactly the same as before (30/70 split, always showing both panes).

- [ ] **Step 4: Verify on phone** - settings opens to categories list. Tap a category -> slides to detail. Back slides to categories. Inside Hardware, tapping Card Readers slides within the detail pane (existing `AnimatedContent` handles this). Back from Card Readers goes to Hardware Overview (existing logic). Back from Hardware Overview goes to categories list.

- [ ] **Step 5: Run existing settings tests:**
```bash
./gradlew :WooCommerce:testWasabiDebugUnitTest --tests "*.WooPosSettingsCategoriesViewModelTest"
./gradlew :WooCommerce:testWasabiDebugUnitTest --tests "*.WooPosSettingsViewModelTest"
```

- [ ] **Step 6: Commit and add to git**

---

## Phase 5: Tap to Pay Support

### Task 16: Add Tap to Pay (TTP) support to POS on phone

**Files:**
- Modify: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/home/totals/WooPosCardReaderPaymentControllerFactory.kt:80`
- Modify: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/home/totals/WooPosTotalsViewModel.kt`
- Modify: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/home/totals/WooPosTotalsScreen.kt`
- Possibly modify: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/cardpayment/WooPosCardPaymentViewModel.kt`
- Possibly modify: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/eligibility/WooPosEligibilityViewModel.kt`

Currently POS hardcodes `cardReaderType = CardReaderType.EXTERNAL` in `WooPosCardReaderPaymentControllerFactory.kt:80`. This means POS only supports external Bluetooth card readers. On phone, Tap to Pay (built-in NFC reader) should be the primary/only card payment method.

The TTP infrastructure already exists in the cardreader library:
- `ReaderType.BuildInReader.TapToPayDevice` - the reader type
- `CardReaderTypesToDiscover.SpecificReaders.BuiltInReaders` - discovery filter
- `CardReaderType.BUILT_IN` enum value in the onboarding flow
- `TTPPaymentProgressDelegate` - already used by POS totals ViewModel
- `BuiltInReaderFailedPayment` states already handled (thrown as `IllegalArgumentException` currently)

**What needs to change:**

- [ ] **Step 1: Research the existing TTP flow in the main app**

The main WooCommerce app already supports TTP outside of POS (in the payments flow). Study how `CardReaderPaymentController` handles `CardReaderType.BUILT_IN` vs `EXTERNAL`:
- How reader discovery differs (Bluetooth scan vs local device check)
- How connection differs (pair vs just activate)
- How the payment UI differs (shows "Hold card near device" instead of "Insert/swipe card")
- What onboarding/eligibility checks are needed (device NFC capability, country support)

Check `CardReaderPaymentController`, `ConnectionManager`, and `DiscoverReadersAction` for the existing BUILT_IN paths.

- [ ] **Step 2: Check device + site TTP eligibility**

Before offering TTP, verify:
- Device supports NFC / TTP (Android API check)
- The store's country supports TTP (check `CardReaderConfigForUSA`, `CardReaderConfigForGB`, `CardReaderConfigForCanada` for built-in reader support)
- Stripe Terminal TTP requirements are met

This may need a new eligibility check or extending the existing `WooPosEligibilityViewModel`.

- [ ] **Step 3: On phone, use BUILT_IN reader type instead of EXTERNAL**

Change `WooPosCardReaderPaymentControllerFactory.create()` to accept the reader type as a parameter instead of hardcoding `CardReaderType.EXTERNAL`:

```kotlin
fun create(
    orderId: Long,
    paymentType: PaymentType,
    isTTPPaymentInProgress: KMutableProperty0<Boolean>,
    cardReaderType: CardReaderType = CardReaderType.EXTERNAL, // default keeps tablet behavior
    allowCancelledStatus: Boolean = false,
): CardReaderPaymentController = CardReaderPaymentController(
    ...
    cardReaderType = cardReaderType,
    ...
)
```

On phone, pass `CardReaderType.BUILT_IN` when TTP is supported.

- [ ] **Step 4: Handle TTP-specific payment states in POS**

Currently `WooPosTotalsViewModel` throws `IllegalArgumentException` for `BuiltInReaderFailedPayment` states (line 406). These need proper handling:
- `BuiltInReaderPaymentCapturing` - show processing animation
- `BuiltInReaderFailedPayment.Cancelable` / `.NonCancelable` - show error with retry
- `BuiltInReaderPaymentSuccessful` - show success screen

- [ ] **Step 5: Update the totals screen UI for TTP**

On phone with TTP:
- The "Connect to reader" / "Reader disconnected" area should either not appear (TTP doesn't need pre-connection) or show "Ready for Tap to Pay"
- The payment instruction should say "Hold card near device" instead of "Tap, insert, or swipe"
- Consider: Povilas's iOS approach shows explicit payment method buttons: "Tap to Pay" (primary), "Card Reader", "Cash". This lets the merchant choose. On Android, if TTP is the only option on phone, just pre-connect and auto-use it

- [ ] **Step 6: Pre-connect TTP reader on checkout**

Similar to Povilas's iOS approach: when the user taps "Check out", pre-connect the TTP reader in the background so it's ready when the order is synced. This avoids a "connecting..." delay at payment time.

- [ ] **Step 7: Verify on a physical device** - TTP requires a real device with NFC, cannot be tested on emulator. If no physical device available, verify the code paths compile and the EXTERNAL fallback still works on emulator.

- [ ] **Step 8: Commit and add to git**

**Note:** This is the most research-heavy task in the plan. The implementation depends heavily on how the existing `CardReaderPaymentController` handles BUILT_IN reader type. The implementer should spend time reading the existing TTP code paths in the main app payments flow before writing POS-specific code. The key reference is Povilas's iOS PR (#16830) which implemented TTP for iOS POS.

---

## Phase 6: Polish & Testing

### Task 17: Widen eligibility screen content on phone

**Files:**
- Modify: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/eligibility/WooPosEligibilityScreen.kt`

Currently uses `fillMaxWidth(0.6f)` for content and `fillMaxWidth(0.5f)` for buttons. On phone these are too narrow.

- [ ] **Step 1: Make fractions adaptive**

```kotlin
val configuration = LocalConfiguration.current
val shortSide = minOf(configuration.screenWidthDp, configuration.screenHeightDp)
val isPhone = shortSide < 674
val contentWidthFraction = if (isPhone) 0.9f else 0.6f
val buttonWidthFraction = if (isPhone) 0.85f else 0.5f
```

Replace `CONTENT_WIDTH_FRACTION` and `BUTTON_WIDTH_FRACTION` usages with these adaptive values.

- [ ] **Step 2: Verify on device** - trigger eligibility screen (e.g. with wrong currency). Content and buttons should use more width on phone.

- [ ] **Step 3: Commit and add to git**

---

### Task 18: Add phone light mode preview

**Files:**
- Modify: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/common/composeui/WooPosComposePreviewAnnotation.kt`

Both phone preview configurations (Phone Vertical and Phone Horizontal) use `UI_MODE_NIGHT_YES`. Add light mode phone previews for completeness.

- [ ] **Step 1: Add light mode phone preview**

Add a new preview variant:
```kotlin
@Preview(
    name = "Phone Vertical Light",
    device = "spec:width=411dp,height=891dp,dpi=420",
    uiMode = Configuration.UI_MODE_TYPE_NORMAL
)
```

- [ ] **Step 2: Commit and add to git**

---

### Task 19: On-device testing with mobile MCP subagents

After each phase (or after significant tasks), dispatch a subagent with mobile MCP tools to build, install, and visually verify on an Android phone emulator. **Keep running test subagents until all findings are fixed.** Each test cycle:

1. Subagent builds and installs the app on a phone emulator
2. Subagent navigates through the affected screens using mobile MCP tools (tap, swipe, screenshot)
3. Subagent takes screenshots and reports any visual/functional issues
4. Fix reported issues in the main session
5. Re-run the test subagent to verify fixes
6. Repeat until no issues remain

**Test scenarios per phase:**

- [ ] **Phase 1 verification (after Tasks 1-7):**
  - Open POS on phone emulator
  - Open a variable product, view variations, press system back - should return to products (Task 1 fix)
  - Trigger an error state - verify buttons fill width (Tasks 3, 4)
  - Open any dialog (e.g. exit POS confirmation) - verify it uses wide fraction (Task 2)
  - Take screenshots of all above states

- [ ] **Phase 2 verification (after Tasks 8-10):**
  - Add products to cart, verify cart button has panel background and sits above nav bar (Task 8)
  - Verify quantity controls appear on product tiles when added to cart (Task 10)
  - Navigate to cart screen, verify back arrow is visible (Task 9)
  - Tap back arrow, verify return to products
  - Take screenshots comparing cart button (products) vs checkout button (cart) - should match visually

- [ ] **Phase 3-6 verification (after Tasks 11-14):**
  - Open Orders from hamburger menu - should show full-screen list on phone
  - Tap an order - should slide to detail
  - Press back - should slide back to list
  - Repeat for Bookings
  - Open Settings - should show categories list
  - Tap Hardware - should slide to Hardware detail
  - Tap Card Readers - should slide within detail pane
  - Navigate back through all levels
  - Take screenshots of each state

- [ ] **Full regression on tablet emulator:**
  - Run through the same flows on a tablet emulator
  - Verify nothing changed - two-pane layouts still work
  - Take screenshots for comparison

- [ ] **Final full flow on phone:**
  - Complete end-to-end: browse products -> add to cart -> cart -> checkout -> cash payment -> success -> new order
  - Open each menu item (Orders, Settings, etc.)
  - Take screenshots of the complete flow for the team presentation

---

## iOS Port Notes

When applying this UX/UI to iOS, the key decisions to carry over:

1. **Hamburger menu, not bottom tabs** - keep primary screen space for the payment flow
2. **Full-screen cart navigation** - linear flow Products -> Cart -> Checkout
3. **Cart button with panel background** - matches checkout button visual treatment
4. **Quantity controls on product tiles** - phone-only, compensates for hidden cart
5. **Back button on cart screen** - always visible on phone
6. **Reusable list-detail component** - equivalent of `WooPosListDetailLayout`. iOS should build a similar SwiftUI component that switches between `NavigationSplitView` behavior (iPad) and `NavigationStack` push/pop (iPhone)
7. **Adaptive dialog sizing** - wider on phone (92% vs 75%)
8. **Adaptive button/error state sizing** - fill width on phone
9. **Settings nested navigation** - categories list -> detail, with detail pane handling its own internal nesting (same as Android)

iOS-specific considerations:
- Povilas's PR (#16830) already has `POSLayoutScale` (.phone/.tablet) - similar to Android's `isWooPosPhoneLayout()` (verify against current iOS codebase, may have changed)
- iOS uses `POSMenuPresenter` for extracted menu logic - similar to Android's hamburger menu approach (verify)
- iOS should NOT use the bottom sheet cart approach from the earlier prototype - switch to full-screen navigation to match Android
- Povilas's TTP implementation (`startPaymentWithMethod`, pre-connect TTP reader, explicit payment method buttons) is the reference for Android TTP work and should align across platforms

---

## Phase 8: Post-Testing UX Polish (added after on-device testing)

These tasks were identified and implemented during iterative on-device testing.

### Completed fixes

- **Bookings in menu** - Restored bookings menu item (lost in merge), gated by CIAB check
- **Font/component scaling** - Bumped from 0.85x to 0.9x for 880-1200dp screens (was too small for POS)
- **Cart toolbar spacing** - Fixed ConstraintLayout anchoring to match WooPosToolbar spacing
- **Cart slide-up transition** - Cart screen slides up from bottom, totals slides horizontally
- **Settings detail back arrow** - Added back-to-categories arrow on phone at category root
- **Menu popup position** - Moved below toolbar (was overlapping)
- **Cart button exit animation** - Fast fadeOut(100ms) to avoid overlap with cart screen sliding in
- **Orders/Bookings auto-selection on phone** - Added `userHasSelectedItem` state so phone starts on list, not detail
- **Card payment crash** - Fixed SavedStateHandle parameter order in ViewModel constructor
- **Cart title vertical alignment** - Removed offset on back button icon
- **Payment success margins** - Added horizontal padding to text
- **Payment success button heights** - Applied toAdaptiveComponentSize()
- **Dialog close icon** - Applied toAdaptiveComponentSize() to 40dp close icon
- **Quantity counter layout** - Moved `- N +` controls to the price row so title gets full width
- **Coupon trash icon** - Shows trash icon on coupons that are in the cart

### Completed in Phase 8 (additional)

- **Quantity controls -> count badge** - Replaced stepper with simple 28dp purple circle badge at bottom-right of product image (Uber pattern). Much cleaner.
- **Coupon badge with trash icon** - Same badge pattern, trash icon instead of number. Second tap removes coupon.
- **Persistent bottom button** - Single button outside NavHost, morphs label by route: "Cart - $37.49" / "Check out" / "Cash payment" (outlined). Eliminates dual-button overlap.
- **Subtotal on cart button** - Computed client-side from raw BigDecimal prices tracked per item in ViewModel.
- **Stepper pill iterations** - Tried purple pill, then dark semi-transparent pill, then settled on simple badge.
- **Orders/Bookings detail titles** - Show "Order #526" / "Booking #498" instead of generic "Orders" / "Bookings".
- **Orders/Bookings detail back arrows** - Added phone-specific toolbars to detail panes.
- **Orders/Bookings parent toolbar hidden on phone detail** - Prevents double toolbar.
- **BackHandler guard with enabled flag** - List pane BackHandler only active when detail is not selected (prevents exit on back from detail).
- **Card payment screen adaptive buttons** - Applied `toAdaptiveComponentSize()` to all 80dp button heights.
- **Card payment text margins** - Added horizontal padding and center alignment.
- **Bookings attendance layout** - Changed from Row to Column so buttons don't wrap text.
- **Bookings detail crash fix** - Replaced `!!` with safe `?.let` for selectedDetails during back animation.
- **Settings transition fix** - `hasInitialized` flag prevents initial detail->list animation on phone.
- **16dp fixed horizontal margins** - Applied to Orders/Bookings/Settings list and detail content.

### Remaining polish items

- Corner radius could be less rounded on phone (currently using tablet values)
- Card payment screen buttons could be pushed further to the bottom (currently use SpaceBetween)
- Bookings "Collect payment" navigates to card payment screen which needs the same adaptive treatment
- Tablet regression testing needed (verify all changes don't break tablet layout)
- Full end-to-end payment flow testing on phone with simulated reader

---

## Summary

| Phase | Tasks | Description |
|-------|-------|-------------|
| 0 | Task 1 | Merge card reader connection UI v2 branch + phone adaptation |
| 1 | Tasks 2-8 | Bug fixes and component-level improvements |
| 2 | Tasks 9-11 | Home flow UX: cart button panel, back button, quantity controls |
| 3 | Task 12 | Reusable WooPosListDetailLayout component |
| 4 | Tasks 13-15 | Adapt Orders, Bookings, Settings for phone |
| 5 | Task 16 | Tap to Pay (TTP) support for phone POS |
| 6 | Tasks 17-18 | Eligibility screen polish + preview annotations |
| Testing | Task 19 | On-device testing with mobile MCP subagents after each phase, iterate until all findings fixed |
