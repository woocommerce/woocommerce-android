# Refund Flow Issues

## Issue 1: WOOMOB-2079 - Issue Refund Dialog Disappears When Orders VM Updates

### Date: 2026-01-28

## Problem Summary
The issue refund dialog disappears when `WooPosOrdersViewModel` updates its state internally during background operations.

## Root Cause
The problem occurs in methods that create **new `WooPosOrdersState.Content` instances** (not using `copy()`). These methods were explicitly setting `dialogState = DialogState.Hidden`, which causes the dialog to disappear.

**Important Note**: Using `data class.copy()` **does preserve** all fields that aren't explicitly overridden. The issue was **not** with `copy()` calls - those were working correctly.

### Affected Methods
The following methods in `WooPosOrdersViewModel.kt` were creating new Content instances with `dialogState` explicitly set to `Hidden`:

1. **`performSearch()` (lines 495, 511, 526)** - Creates new Content states during search operations
2. **`replaceOrders()` (line 655)** - Creates new Content when replacing the entire order list
3. **`appendOrders()` (line 682)** - Creates new Content when appending more orders

Note: `performSearch()` intentionally dismisses the dialog since search is a user-initiated state transition.

## Solution
Fixed the methods that create new `WooPosOrdersState.Content` instances to preserve the existing `dialogState`:

### Changes Made

**File**: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/orders/WooPosOrdersViewModel.kt`

1. **Line 655-665** - `replaceOrders()`:
   ```kotlin
   dialogState = (currentState as? WooPosOrdersState.Content)?.dialogState
       ?: WooPosOrdersState.Content.DialogState.Hidden
   ```
   Preserves dialog state from current state if it's Content, otherwise defaults to Hidden.

2. **Line 682-691** - `appendOrders()`:
   ```kotlin
   dialogState = current.dialogState
   ```
   Preserves dialog state from current state.

3. **Lines 495, 511, 526** - `performSearch()`:
   Explicitly sets `dialogState = WooPosOrdersState.Content.DialogState.Hidden` (intentional - search should dismiss dialog)

## Testing
- ✅ All unit tests pass (WooPosOrdersViewModelTest)
- ✅ Detekt passes with no violations
- ✅ Fix verified with reproduction steps from Linear issue

## Impact
The dialog will now remain visible when:
- Orders are refreshed in the background (`replaceOrders()`)
- More orders are loaded via pagination (`appendOrders()`)

The dialog will still be dismissed when:
- User performs a search (intentional UX - search is a state transition)

## Technical Notes
- `data class.copy()` **automatically preserves** all fields not explicitly passed as parameters
- The bug was specifically in constructor calls: `WooPosOrdersState.Content(...)` not in `copy()` calls
- `performSearch()` intentionally dismisses the dialog because search is a user-initiated action that should reset UI state

---

## Issue 2: WOOMOB-1951 - Refund Step Not Reset When Dialog Reopened

### Date: 2026-01-28

### Problem Summary
When the refund dialog is dismissed and "Issue refund" button is clicked again, the refund flow doesn't reset. Instead of starting from scratch with fresh data, it shows the previous state (e.g., if user was on ReviewRefund step, it shows that instead of SelectItems).

### Root Cause
In `WooPosIssueRefundDialog.kt` line 65, the ViewModel uses a persistent key:
```kotlin
hiltViewModel<WooPosRefundViewModel, WooPosRefundViewModel.Factory>(key = "refund_$orderId")
```

This causes the ViewModel to survive across dialog dismissals. When the dialog is reopened:
1. The same ViewModel instance is reused
2. The `init` block (which calls `loadRefundableItems()`) only runs once during VM creation
3. `handleDialogDismissed()` only resets the step to `SelectItems` but doesn't reload data
4. Result: stale state is displayed

### Solution
The fix requires two changes:
1. Keep the `key` parameter with orderId so different orders get different ViewModels
2. Use `LaunchedEffect(Unit)` to reload data every time the dialog is shown (even for the same order)

This ensures:
- Different orders get separate ViewModel instances (via key)
- Data is always fresh when the dialog opens (via LaunchedEffect)

### Changes Made

**File 1**: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/orders/WooPosRefundViewModel.kt`

**Line 52**: Added `loadingJob` property to track and cancel ongoing loads:
```kotlin
private var loadingJob: Job? = null
```

**Line 54-56**: Added `init` block to load data on ViewModel creation:
```kotlin
init {
    loadRefundableItems()
}
```

**Line 58-60**: Cancel existing job before starting new load:
```kotlin
private fun loadRefundableItems() {
    loadingJob?.cancel()
    loadingJob = viewModelScope.launch {
```

This prevents race conditions when dialog is opened/closed rapidly or `DialogOpened` event is triggered multiple times.

**File 2**: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/orders/WooPosIssueRefundDialog.kt`

**Line 65**: Keep the `key = "refund_$orderId"` parameter

**Line 69-71**: Send `DialogOpened` event when dialog opens:
```kotlin
LaunchedEffect(Unit) {
    viewModel.onUIEvent(WooPosRefundUIEvent.DialogOpened)
}
```

**File 3**: `WooPosRefundUIEvent.kt` - Added `DialogOpened` event to trigger data reload

### How It Works
- `key = "refund_$orderId"`: Each orderId gets its own ViewModel instance that persists in memory
- `LaunchedEffect(Unit)`: Runs every time the composable enters composition (i.e., when dialog opens)
- Result: Fresh data loaded each time dialog opens, whether it's the same or different order

### Impact
- Different orders get separate ViewModel instances (no data mixing between orders)
- Data is reloaded fresh every time the dialog opens
- Refund flow always starts from `SelectItems` step with current order data
- ViewModel instances remain in memory but are refreshed on each dialog open

### Testing
- ✅ All unit tests pass (WooPosRefundViewModelTest) - 24 tests
- ✅ Build successful
- ✅ Detekt passes with no violations
- ✅ Fix verified:
  - Refund flow always starts fresh when dialog opens for the same order
  - Different orders get separate ViewModel instances (no data mixing)
  - Data is reloaded every time dialog is shown
  - Race conditions prevented by job cancellation

### Trade-offs
- ~~Data loads twice on first dialog open (init + DialogOpened event)~~ (UPDATE: init block was removed)
- ~~This is acceptable because:~~
  - ~~Keeps ViewModel testable (tests work without triggering DialogOpened)~~
  - ~~Job cancellation ensures the first load is cancelled before the second starts~~
  - ~~Avoids updating 32 unit tests to manually trigger loading~~

### Update: Init Block Removed (2026-01-29)

User removed the `init` block from the ViewModel. Tests needed to be updated:

**Previous approach (with init block)**:
- Init block called `loadRefundableItems()` on ViewModel creation
- Tests would create ViewModel and data would load automatically
- Coroutines ran immediately with `UnconfinedTestDispatcher`

**New approach (without init block)**:
- No automatic loading on ViewModel creation
- Tests must explicitly call `viewModel.onUIEvent(WooPosRefundUIEvent.DialogOpened)` after creating ViewModel
- With `UnconfinedTestDispatcher`, coroutines run synchronously - **no need for `advanceUntilIdle()`**
- Pattern in tests:
  ```kotlin
  viewModel = createViewModel()
  viewModel.onUIEvent(WooPosRefundUIEvent.DialogOpened)
  // State is now Content/Error/NoRefundableItems (no need to advance)
  ```

**Key insight**: `UnconfinedTestDispatcher` executes coroutines eagerly and immediately. When `viewModelScope.launch` is called, the coroutine runs synchronously and completes before the launch call returns. Therefore, `advanceUntilIdle()` is not needed and was actually causing issues because `runTest`'s scheduler is separate from the rule's test dispatcher.

**Changes to tests**:
- All 32 tests updated to call `DialogOpened` event after ViewModel creation
- Removed all `advanceUntilIdle()` calls (not needed with `UnconfinedTestDispatcher`)
- All tests pass ✅

---

---

## Issue 3: Make POS Refund Payment Method Dynamic

### Date: 2026-01-29

### Problem Summary
The POS refund flow had a hardcoded payment method display (`"TEST: payment card ••••1456"`). It should dynamically load and display the actual payment method used for the order, matching the behavior of the store management refund flow.

### Implementation

Replicated the payment method loading logic from `RefundSummaryViewModel` to `WooPosRefundViewModel`.

#### Changes Made

**File 1**: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/orders/WooPosRefundViewModel.kt`

1. **Added imports** for payment method handling:
   - `PaymentChargeRepository` - for fetching card details
   - `WCGatewayStore` - for loading payment gateway info
   - `CoroutineDispatchers` - for IO operations
   - `PaymentGateway`, `isCashPayment`, `toAppModel` - domain models and extensions

2. **Added constructor parameters** (lines 48-50):
   - `paymentChargeRepository: PaymentChargeRepository`
   - `gatewayStore: WCGatewayStore`
   - `coroutineDispatchers: CoroutineDispatchers`

3. **Added constant** (lines 65-67):
   - `REFUND_METHOD_MANUAL = "manual"` - fallback for manual refunds

4. **Added payment method loading methods** (lines 114-163):
   - `loadPaymentGateway(order)` - Fetches gateway info from order.paymentMethod
   - `loadPaymentMethod(order)` - Main method that determines the payment method string
   - `enrichRefundMethodWithCardDetails(order, refundMethod)` - Fetches card details if chargeId exists
   - `loadCardDetails(chargeId, refundMethod)` - Formats payment method with card brand and last 4 digits

5. **Updated `loadRefundableItems()`** (line 203):
   - Calls `loadPaymentMethod(order)` after fetching order and refunds
   - Passes payment method to `buildContentState()`

6. **Updated `buildContentState()` signature** (line 224):
   - Added `paymentMethod: String` parameter
   - Changed line 247 from hardcoded string to use the `paymentMethod` parameter

7. **Updated `recalculateRefundState()`** (line 339):
   - Passes `currentState.paymentMethod` when rebuilding state after selection changes

**File 2**: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/orders/WooPosIssueRefundDialog.kt`

- **Line 521**: Changed from hardcoded `"TEST: Via payment card ••••1456"` to dynamic:
  ```kotlin
  stringResource(R.string.woopos_orders_via_payment_method, state.paymentMethod)
  ```

**File 3**: `WooCommerce/src/main/res/values/strings.xml`

- **Line 3822**: Added new string resource:
  ```xml
  <string name="woopos_orders_via_payment_method">Via %1$s</string>
  ```

**File 4**: `WooCommerce/src/test/kotlin/com/woocommerce/android/ui/woopos/orders/WooPosRefundViewModelTest.kt`

1. **Added mock dependencies** (lines 54-56):
   - `paymentChargeRepository: PaymentChargeRepository = mock()`
   - `gatewayStore: WCGatewayStore = mock()`
   - `coroutineDispatchers = coroutineTestRule.testDispatchers`

2. **Added mock setups in `@Before setUp()`** (lines 128-139):
   - Mock resource strings for refund methods
   - Mock gateway store to return null (fallback to manual)
   - Mock payment charge repository to return error (no card details)

3. **Updated `createViewModel()`** (lines 149-151):
   - Added three new constructor parameters to match production code

### Behavior
The payment method now displays dynamically based on:
1. **Payment gateway** from order's payment method (via `WCGatewayStore`)
2. **Card details** from chargeId (via `PaymentChargeRepository`)
   - Format: `"{Gateway Title} ({Brand} **** {last4})"` when card details available
   - Example: `"Credit card refund (Visa **** 4242)"`
3. **Fallback** to gateway title or "Manual refund" when details unavailable

Edge cases handled:
- Manual refunds (no card details)
- Cash payments (use payment method title)
- Missing chargeId (use gateway title)
- Failed card data fetch (fallback to gateway title)

### Testing
- ✅ All 32 unit tests pass
- ✅ Detekt passes with no violations
- ✅ Payment method loading logic matches store management refund flow
- ✅ Graceful fallbacks when gateway/card data unavailable

### Refactoring: Extract to Use Case (2026-01-29)

Extracted payment method loading logic to a reusable use case class following WooPOS patterns.

#### Changes Made

**File 1**: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/orders/WooPosLoadPaymentMethod.kt` (NEW)

Created new use case class following the same pattern as `WooPosGetRefundableItems`, `WooPosCalculateRefundSubtotal`, etc.
- Encapsulates all payment method loading logic
- Dependencies injected via constructor: `PaymentChargeRepository`, `WCGatewayStore`, `SelectedSite`, `ResourceProvider`, `CoroutineDispatchers`
- Main method: `suspend operator fun invoke(order: Order): String`
- Private helper methods: `loadPaymentGateway()`, `enrichRefundMethodWithCardDetails()`, `loadCardDetails()`
- Constant: `REFUND_METHOD_MANUAL = "manual"`

**File 2**: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/orders/WooPosRefundViewModel.kt`

**Removed:**
- Imports: `PaymentChargeRepository`, `WCGatewayStore`, `CoroutineDispatchers`, `PaymentGateway`, `isCashPayment`, `toAppModel`, `withContext`
- Constructor parameters: `paymentChargeRepository`, `gatewayStore`, `coroutineDispatchers` (3 params)
- Companion object with `REFUND_METHOD_MANUAL` constant
- Methods: `loadPaymentGateway()`, `loadPaymentMethod()`, `enrichRefundMethodWithCardDetails()`, `loadCardDetails()` (~50 lines)

**Added:**
- Constructor parameter: `loadPaymentMethod: WooPosLoadPaymentMethod` (1 param)
- Simplified call: `val paymentMethod = loadPaymentMethod(order)` (line 203)

**File 3**: `WooCommerce/src/test/kotlin/com/woocommerce/android/ui/woopos/orders/WooPosRefundViewModelTest.kt`

**Removed:**
- Mock dependencies: `paymentChargeRepository`, `gatewayStore`, `coroutineDispatchers` (3 mocks)
- Mock setups for resource strings and repositories (7 lines)
- Constructor parameters in `createViewModel()` (3 params)

**Added:**
- Mock dependency: `loadPaymentMethod: WooPosLoadPaymentMethod = mock()` (1 mock)
- Simple mock setup: `whenever(loadPaymentMethod.invoke(any())).thenReturn("Manual refund")` (1 line)
- Constructor parameter: `loadPaymentMethod` (1 param)

### Benefits
- **Separation of concerns**: Payment method logic isolated in dedicated use case
- **Reusability**: Can be used by other features needing payment method display
- **Testability**: Easier to test both the use case and ViewModel independently
- **Maintainability**: ViewModel reduced from ~350 lines to ~300 lines
- **Consistency**: Follows existing WooPOS patterns (`WooPosGetRefundableItems`, etc.)

### Testing (After Refactoring)
- ✅ All 32 unit tests pass
- ✅ Detekt passes with no violations
- ✅ No behavior changes - pure refactoring

---

## PR Review: File Naming Consistency (2026-02-02)

### Date: 2026-02-02

### Problem Summary
PR reviewer (@kidinov) noted: "overall naming sometimes is `Order` and sometimes `orders`"

Specifically pointed out:
- File named `WooPosOrdersDetails` (plural) represents a single order detail view
- Should be renamed to `WooPosOrderDetails` for consistency with `WooPosOrderDetailsMapper`

Also questioned whether `WooPosOrderItemMapper` and `WooPosOrderStatusMapper` could move to `details/` package if primarily used for order details display.

### Analysis

**Initial assessment** was incorrect. Upon reconsideration:
- Both mappers are fundamentally about **displaying order information** (which is the domain of the details view)
- `WooPosOrderItemMapper` maps order data for display in the list (still order display logic)
- `WooPosOrderStatusMapper` provides status display formatting (order presentation concern)
- Both mappers are utilities for rendering order information, not core order list functionality

**Revised conclusion**: Both mappers should move to the `details` package because:
- They handle order presentation/display concerns
- The orders list just consumes these utilities
- Better semantic grouping: details package contains all order display logic

**File naming issue**:
- File: `WooPosOrdersDetails.kt` (plural)
- Composable function: `WooPosOrderDetails` (singular)
- Inconsistency: File name doesn't match function name

### Solution

1. Renamed file to match the singular naming:
   - `WooPosOrdersDetails.kt` → `WooPosOrderDetails.kt`

2. Moved mappers to `details` package:
   - `WooPosOrderItemMapper` → `orders.details.WooPosOrderItemMapper`
   - `WooPosOrderStatusMapper` → `orders.details.WooPosOrderStatusMapper`

### Changes Made

**Step 1**: Used `git mv` to rename the details file:
```bash
git mv WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/orders/details/WooPosOrdersDetails.kt \
       WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/orders/details/WooPosOrderDetails.kt
```

**No code changes needed for file rename**:
- Composable function already named `WooPosOrderDetails` (singular) ✅
- Package declaration unchanged: `package com.woocommerce.android.ui.woopos.orders.details` ✅
- All imports reference the function name, not the file name ✅
- No test files exist for this UI-only composable ✅

**Step 2**: Moved mapper files using `git mv`:
```bash
git mv WooCommerce/src/.../orders/WooPosOrderItemMapper.kt \
       WooCommerce/src/.../orders/details/WooPosOrderItemMapper.kt
git mv WooCommerce/src/.../orders/WooPosOrderStatusMapper.kt \
       WooCommerce/src/.../orders/details/WooPosOrderStatusMapper.kt
```

**Step 3**: Updated package declarations in moved files:

**WooPosOrderItemMapper.kt**:
- Changed package: `com.woocommerce.android.ui.woopos.orders` → `com.woocommerce.android.ui.woopos.orders.details`
- Added import: `com.woocommerce.android.ui.woopos.orders.WooPosOrdersState` (needed after package change)

**WooPosOrderStatusMapper.kt**:
- Changed package: `com.woocommerce.android.ui.woopos.orders` → `com.woocommerce.android.ui.woopos.orders.details`
- Added imports:
  - `com.woocommerce.android.ui.woopos.orders.OrderStatusColorKey`
  - `com.woocommerce.android.ui.woopos.orders.PosOrderStatus`

**Step 4**: Updated imports in files that reference the mappers:

**WooPosOrdersViewModel.kt** (line 16):
- Added: `import com.woocommerce.android.ui.woopos.orders.details.WooPosOrderItemMapper`

**WooPosOrderDetailsMapper.kt** (line 7):
- Removed: `import com.woocommerce.android.ui.woopos.orders.WooPosOrderStatusMapper`
- Now uses same-package reference (both in `details` package)

**WooPosOrdersViewModelTest.kt** (lines 16-17):
- Added: `import com.woocommerce.android.ui.woopos.orders.details.WooPosOrderItemMapper`
- Added: `import com.woocommerce.android.ui.woopos.orders.details.WooPosOrderStatusMapper`

### Testing
- ✅ All POS unit tests pass (WooPosOrders*, WooPosRefund*)
- ✅ Detekt passes with no violations
- ✅ Build successful
- ✅ No behavior changes - pure refactoring

### Benefits
- **Consistency**: File name now matches composable function name
- **Clarity**: Singular naming makes it clear this is for a single order detail view
- **Alignment**: Matches naming pattern of `WooPosOrderDetailsMapper` (singular)
- **Better organization**: Mappers now grouped with other display/presentation logic in `details` package
- **Semantic clarity**: `details` package contains all order display concerns, `orders` package is for list functionality

---

## Merge with trunk (2026-02-02)

### Date: 2026-02-02

### Changes Merged from trunk
Merged latest trunk changes into pos-refunds-package-reorg branch (29 commits ahead after merge).

**Key changes from trunk**:
- Added `WooPosGetPaymentMethod` class in `orders` package (replaces the `WooPosLoadPaymentMethod` we had in branch)
- Payment method loading logic now in trunk with slightly different error handling (throws error instead of returning Result)
- New FTS search database schema (WooPosSearchableFtsEntity)
- Push notification handling improvements
- Beta features UI updates

### Merge Resolution

**Issue**: Missing import for `WooPosGetPaymentMethod` in refund files after auto-merge

**Fixed files**:
1. **WooPosRefundViewModel.kt** (line 10):
   - Added: `import com.woocommerce.android.ui.woopos.orders.WooPosGetPaymentMethod`
   - Constructor parameter already correct from merge (line 41): `private val getPaymentMethod: WooPosGetPaymentMethod`

2. **WooPosRefundViewModelTest.kt** (line 9):
   - Added: `import com.woocommerce.android.ui.woopos.orders.WooPosGetPaymentMethod`
   - Mock declaration already correct from merge (line 55): `private val loadPaymentMethod: WooPosGetPaymentMethod = mock()`

**Note**: trunk's `WooPosGetPaymentMethod` is similar to our `WooPosLoadPaymentMethod` but:
- Located in `orders` package (not `orders.details`)
- Throws error instead of returning `Result<PaymentGateway>`
- Simpler API: `suspend operator fun invoke(order: Order): String`

### Testing
- ✅ All POS unit tests pass (WooPosOrders*, WooPosRefund*, WooPosGetPaymentMethod*)
- ✅ Detekt passes with no violations
- ✅ Build successful
- ✅ No behavior regressions

---

## Package Reorganization (2026-01-30)

### Date: 2026-01-30

### Problem Summary
The `com.woocommerce.android.ui.woopos.orders` package contained too many files (27 Kotlin files) with mixed responsibilities. Order list logic, order details logic, and refund logic were all in the same package, making it difficult to navigate and understand the codebase.

### Solution
Reorganized the package into a more granular structure:

1. **`orders` package** (kept) - Order list and general order functionality
   - `WooPosOrdersScreen.kt` - Main orders list screen
   - `WooPosOrdersViewModel.kt` - ViewModel for orders list
   - `WooPosOrdersState.kt` - State for orders list
   - `WooPosOrdersUIEvent.kt` - UI events for orders list
   - `WooPosOrdersDataSource.kt` - Data source for orders
   - `WooPosOrdersInMemoryCache.kt` - In-memory cache
   - `WooPosOrdersNavigation.kt` - Navigation for orders
   - `WooPosOrdersAnalyticsTracker.kt` - Analytics tracking
   - `WooPosOrdersLoadingScreen.kt` - Loading screen
   - `WooPosOrdersStatusBadge.kt` - Status badge component
   - `WooPosOrderItemMapper.kt` - Item mapper
   - `WooPosOrderStatusMapper.kt` - Status mapper
   - `WooPosOrderActionsProvider.kt` - Actions provider

2. **`orders.details` package** (new) - Order detail functionality
   - `WooPosOrdersDetails.kt` - Order details screen
   - `WooPosOrderDetailsMapper.kt` - Details mapper

3. **`orders.details.refund` package** (new) - Refund-specific logic
   - `WooPosIssueRefundDialog.kt` - Main refund dialog UI
   - `WooPosRefundViewModel.kt` - Refund view model
   - `WooPosRefundState.kt` - Refund state
   - `WooPosRefundUIEvent.kt` - Refund UI events
   - `WooPosRefundReasonScreen.kt` - Refund reason screen
   - `WooPosRefundReasonNavigation.kt` - Refund reason navigation
   - `WooPosRefundInfoBuilder.kt` - Refund info builder
   - `WooPosRefundableItem.kt` - Refundable item model
   - `WooPosGetRefundableItems.kt` - Get refundable items use case
   - `WooPosGroupRefundItems.kt` - Group refund items use case
   - `WooPosCalculateRefundSubtotal.kt` - Calculate refund subtotal
   - `WooPosCalculateRefundTax.kt` - Calculate refund tax

### Implementation

**Step 1**: Created new package directories
- `orders/details/` - For order detail screens and mappers
- `orders/details/refund/` - For refund-specific logic

**Step 2**: Moved files using `git mv`
- Moved 2 files to `orders.details`
- Moved 12 files to `orders.details.refund` (including WooPosIssueRefundDialog.kt)
- Moved corresponding test files to match new structure

**Step 3**: Updated package declarations
- Changed package declarations in all moved files from:
  - `package com.woocommerce.android.ui.woopos.orders`
- To:
  - `package com.woocommerce.android.ui.woopos.orders.details`
  - `package com.woocommerce.android.ui.woopos.orders.details.refund`

**Step 4**: Updated imports across the codebase
- Updated imports in production code:
  - `WooPosOrdersScreen.kt` - Added imports for `WooPosIssueRefundDialog` and `WooPosOrderDetails`
  - `WooPosOrdersViewModel.kt` - Added imports for `WooPosOrderDetailsMapper` and `WooPosRefundInfoBuilder`
  - `WooPosOrderActionsProvider.kt` - Added import for `WooPosGetRefundableItems`
  - `WooPosOrderDetailsMapper.kt` - Added imports for classes from orders package
  - `WooPosRefundInfoBuilder.kt` - Added imports for `RefundsFetchResult` and `WooPosOrdersState`
  - `WooPosOrdersDetails.kt` - Added imports for various orders package classes
  - `WooPosIssueRefundDialog.kt` - Added imports for refund package classes
  - `WooPosRefundViewModel.kt` - Added import for `WooPosOrdersDataSource`
  - `WooPosRefundReasonNavigation.kt` - Added import for `ORDERS_ROUTE`
  - `WooPosOrdersNavigation.kt` - Added import for `REFUND_REASON_RESULT_KEY`
  - `WooPosMainFlowGraph.kt` - Updated import for `refundReasonScreen`
  - `WooPosNavigationEventHandler.kt` - Updated import for `navigateToRefundReason`

- Updated imports in test code:
  - `WooPosOrdersViewModelTest.kt` - Added imports for classes from details and refund packages
  - `WooPosRefundViewModelTest.kt` - Added import for `WooPosOrdersDataSource`

### Benefits
- **Better organization**: Related functionality is now grouped together
- **Easier navigation**: Developers can quickly find refund-specific or detail-specific code
- **Clearer responsibilities**: Package structure reflects the feature hierarchy
- **Improved maintainability**: Changes to refund logic don't require navigating through order list files
- **Scalability**: Easier to add more sub-packages as features grow

### Testing
- ✅ All unit tests pass (WooPosRefundViewModelTest - 32 tests)
- ✅ All order-related tests pass
- ✅ Detekt passes with no violations
- ✅ Build successful
- ✅ No behavior changes - pure refactoring

---

### PR Review Fix: Preserve Gateway Title When Disabled (2026-01-30)

#### Problem
Copilot review comment identified that `loadPaymentGateway()` in `WooPosGetPaymentMethod.kt` was discarding the gateway title when the gateway was disabled. The method was replacing disabled gateways with `PaymentGateway(methodTitle = REFUND_METHOD_MANUAL)`, which has a blank `title` field. This caused the disabled gateway test to fail because the code at line 28 checks `paymentGateway.title.isNotBlank()`, which would always be false for the fallback instance.

Expected behavior from test: `"Manual refund (Credit Card)"`
Actual behavior: `"Manual refund"`

#### Solution
Simplified `loadPaymentGateway()` to return the gateway as-is (whether enabled or disabled) or the fallback if not found. Removed the check for `isEnabled` - the caller already handles disabled gateways and gateways without refund support.

**Changed** (lines 41-44):
```kotlin
private suspend fun loadPaymentGateway(order: Order): PaymentGateway = withContext(coroutineDispatchers.io) {
    gatewayStore.getGateway(selectedSite.get(), order.paymentMethod)?.toAppModel()
        ?: PaymentGateway(methodTitle = REFUND_METHOD_MANUAL)
}
```

**Previous code** (lines 41-48):
```kotlin
private suspend fun loadPaymentGateway(order: Order): PaymentGateway = withContext(coroutineDispatchers.io) {
    val paymentGateway = gatewayStore.getGateway(selectedSite.get(), order.paymentMethod)?.toAppModel()
    return@withContext if (paymentGateway != null && paymentGateway.isEnabled) {
        paymentGateway
    } else {
        PaymentGateway(methodTitle = REFUND_METHOD_MANUAL)
    }
}
```

#### Impact
- Disabled gateways now correctly display as `"Manual refund (Credit Card)"` instead of just `"Manual refund"`
- The `invoke()` method at line 27 already has the logic to handle disabled gateways and format the manual refund string
- Simplified code - removed unnecessary enabled check that was duplicating logic

#### Testing
- ✅ All unit tests pass (WooPosLoadPaymentMethodTest) - especially the disabled gateway test
- ✅ Detekt passes with no violations
- ✅ Addresses PR review comment from Copilot

---

## Merge with trunk (2026-01-29)

### Changes in trunk
The trunk branch included significant refactoring for item selection feature:
- Added `ItemSelectionToggled` and `SelectAllToggled` events to `WooPosRefundUIEvent.kt`
- Refactored `loadRefundableItems()` to use helper methods:
  - `fetchSiteSettings()` - Fetches and caches site settings
  - `fetchTaxRoundAtSubtotal()` - Fetches and caches tax rounding setting
  - `fetchOrderAndRefunds()` - Fetches order and refunds
- Added caching: `cachedNumberOfDecimalPoints`, `cachedTaxRoundAtSubtotal`
- Added item selection handling:
  - `handleItemSelection()` - Toggle individual item selection
  - `handleSelectAllToggled()` - Toggle all items selection
  - `recalculateRefundState()` - Recalculate totals when selection changes
- Updated `buildContentState()` to support `selectedItemIds` parameter
- Updated `WooPosRefundState.Content` to include selection-related fields

### Merge conflicts resolved
1. **WooPosRefundUIEvent.kt**: Combined both sets of events (DialogOpened + item selection events)
2. **WooPosRefundViewModel.kt**: Integrated our job cancellation logic with trunk's refactored structure:
   - Kept `loadingJob` property
   - Kept `init` block
   - Preserved trunk's helper methods and caching logic
   - Kept `handleDialogDismissed()` business logic (sets to Loading + cancels job)

### Post-merge fix
Updated tests to match business logic in `handleDialogDismissed()`:
- Business logic: Sets state to `Loading` and cancels loading job when dialog dismissed
- Updated 3 tests to expect `Loading` state instead of `Content` with `SelectItems` step:
  - `given content state at ReviewRefund step, when DialogDismissed event, then state resets to Loading`
  - `given content state at SelectItems step, when DialogDismissed event, then state resets to Loading`
  - `given content state at ConfirmRefund step, when DialogDismissed event, then state resets to Loading`
- This ensures fresh data loads when dialog reopens (via `DialogOpened` event)

### Testing
- ✅ All 32 unit tests pass (added 8 new tests for item selection)
- ✅ Detekt passes
- ✅ Merge conflicts resolved successfully

---

## Issue 4: Implement Refund Success Screen (2026-01-29)

### Date: 2026-01-29

### Problem Summary
The refund success screen was showing a simple text-based success message. The Figma design specified a polished success screen with:
- Green checkmark icon in circular background
- "Refund complete" heading
- Dynamic refund message showing amount and payment method
- "Done" button (primary action)
- "Email receipt" button (secondary action)

### Implementation

Redesigned the refund success screen to match Figma specifications.

#### Changes Made

**File 1**: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/orders/WooPosRefundState.kt`

Added `paymentMethod: String` field to `RefundSuccess` data class (line 59):
- Allows displaying payment method in success message
- Format: "You refunded $21.60 via payment card ••••1456"

**File 2**: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/orders/WooPosRefundViewModel.kt`

Updated `processRefund()` method (lines 326-331):
- Pass `paymentMethod` from `contentState` when creating `RefundSuccess` state
- Ensures payment method is available in success screen

**File 3**: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/orders/WooPosIssueRefundDialog.kt`

**Replaced `RefundSuccessContent` composable** (lines 251-329):
- Added `onNavigationEvent` parameter for email receipt navigation
- Centered layout using `Box` with `Column`
- Added `RefundSuccessCheckmark()` composable:
  - 166dp circular background with green success color
  - 72dp white checkmark icon
  - Shadow elevation for depth
- "Refund complete" heading (bold, large typography)
- Dynamic success message using new string resource
- Full-width "Done" button (80dp height, primary style)
- Full-width "Email receipt" button (80dp height, outlined style)
- Proper spacing between elements (XXXLarge, Medium, Huge)

**Updated imports**:
- Added: `background`, `CircleShape`, `height`, `shadow`
- Added: `WooPosElevation`, `WooPosIcons`

**Updated calling code** (lines 113-117):
- Pass `onNavigationEvent` to `RefundSuccessContent`
- Email receipt button navigates via `WooPosNavigationEvent.OpenEmailReceipt(orderId)`

**File 4**: `WooCommerce/src/main/res/values/strings.xml`

Added new string resources (lines 3832-3833):
- `woopos_refund_complete`: "Refund complete"
- `woopos_refund_success_message`: "You refunded %1$s via %2$s." (takes amount and payment method)

**File 5**: `WooCommerce/src/test/kotlin/com/woocommerce/android/ui/woopos/orders/WooPosRefundViewModelTest.kt`

Updated test assertions (line 752):
- Added assertion: `assertThat(successState.paymentMethod).isEqualTo("Manual refund")`
- Verifies payment method is correctly passed to success state

### Design Patterns Used
- Reused checkmark icon pattern from `WooPosTotalsPaymentSuccessScreen` (green circle + white check)
- Used `WooPosTheme.colors.success` for circle background
- Used `WooPosTheme.colors.onSuccess` for checkmark color
- Used `WooPosButton` and `WooPosOutlinedButton` components
- Consistent spacing and sizing with existing payment success screen
- Navigation via `WooPosNavigationEvent.OpenEmailReceipt(orderId)`

### Impact
- Professional, polished refund success experience
- Clear visual feedback with green checkmark
- Actionable buttons for next steps (Done, Email receipt)
- Dynamic message shows refunded amount and payment method
- Matches Figma design specifications (without close button per user request)

### Testing
- ✅ All 32 unit tests pass
- ✅ Detekt passes with no violations
- ✅ UI components render correctly with proper spacing
- ✅ Email receipt navigation wired up correctly

### Code Refactoring: Shared Success Checkmark Component (2026-01-29)

Extracted the success checkmark animation to a reusable component to eliminate code duplication between the refund success screen and payment success screen.

#### Changes Made

**File 1 (NEW)**: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/common/composeui/component/WooPosSuccessCheckmark.kt`

Created shared component with:
- `WooPosSuccessCheckmark` composable with animation support
- `WooPosSuccessCheckmarkAnimationStage` enum (INITIAL, BUTTONS, CIRCLE, ICON, FINISHED)
- Animated circle size (0dp → 166dp)
- Animated icon size (0dp → 72dp)
- Green circle background with shadow
- White checkmark icon

**File 2**: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/orders/WooPosIssueRefundDialog.kt`

- Removed local `RefundSuccessCheckmark` composable and `RefundSuccessAnimationStage` enum
- Added imports for shared `WooPosSuccessCheckmark` and `WooPosSuccessCheckmarkAnimationStage`
- Updated all references to use shared types
- Removed unused imports (CircleShape, shadow, WooPosElevation, WooPosIcons)

**File 3**: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/home/totals/payment/success/WooPosTotalsPaymentSuccessScreen.kt`

- Removed local `CheckMarkIcon` composable and `AnimationStage` enum
- Added imports for shared `WooPosSuccessCheckmark` and `WooPosSuccessCheckmarkAnimationStage`
- Updated all references to use shared types
- Removed unused imports (size, CircleShape, Icon, shadow, WooPosElevation, WooPosIcons)
- Added testTag modifier to shared checkmark component

#### Benefits
- **Code reuse**: Single source of truth for success checkmark animation
- **Consistency**: Both screens use identical animation timing and appearance
- **Maintainability**: Updates to checkmark apply to both screens automatically
- **Reduced duplication**: Eliminated ~50 lines of duplicate code

#### Testing
- ✅ All 32 unit tests pass
- ✅ Detekt passes with no violations
- ✅ Both success screens use the same component
- ✅ Animation behavior preserved

### Animation Management Refactoring (2026-01-29)

Further improved the `WooPosSuccessCheckmark` component to be fully self-contained by moving all animation management logic into the component itself.

#### Problem
The animation was "launch and forget" - once started, it runs to completion with no external control needed. However, each screen using the checkmark had to manage the animation state with:
- Instrumented test detection
- Animation state flow management
- LaunchedEffect to start animations
- Suspend function for animation sequencing
- ~40 lines of boilerplate code per screen

#### Solution
Moved all animation management into `WooPosSuccessCheckmark` component:

**File 1**: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/common/composeui/component/WooPosSuccessCheckmark.kt`

**Added animation management**:
- Instrumented test detection (skips animation in tests)
- Animation state management with `MutableStateFlow`
- `LaunchedEffect(Unit)` to auto-start animation
- `startSuccessCheckmarkAnimations()` suspend function
- State persistence with `rememberSaveable`
- Optional callback: `onAnimationStageChanged: (WooPosSuccessCheckmarkAnimationStage) -> Unit = {}`

**Updated signature** (lines 28-32):
```kotlin
@Composable
fun WooPosSuccessCheckmark(
    contentDescription: String,  // No longer requires animationStage parameter
    modifier: Modifier = Modifier,
    onAnimationStageChanged: (WooPosSuccessCheckmarkAnimationStage) -> Unit = {}
)
```

The callback allows parent screens to react to animation stage changes (e.g., for adjusting spacing).

**File 2**: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/orders/WooPosIssueRefundDialog.kt`

**Removed** (~40 lines):
- Wrapper `RefundSuccessContent` function with animation state management
- `isInstrumentedTest` detection
- `savedAnimationStage` and `animationStateFlow` state
- `LaunchedEffect` block
- `startRefundSuccessAnimations()` suspend function
- Imports: `rememberSaveable`, `collectAsState`, `delay`, `MutableStateFlow`, `flow.update`

**Simplified** to (lines 267-273):
```kotlin
@Composable
private fun RefundSuccessContent(...) {
    val animationStage = remember { mutableStateOf(WooPosSuccessCheckmarkAnimationStage.INITIAL) }
    // ... UI code
    WooPosSuccessCheckmark(
        contentDescription = stringResource(R.string.woopos_refund_complete),
        onAnimationStageChanged = { stage -> animationStage.value = stage },
        // ...
    )
}
```

**File 3**: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/home/totals/payment/success/WooPosTotalsPaymentSuccessScreen.kt`

**Removed** (~40 lines):
- Wrapper `WooPosPaymentSuccessScreen` function with animation state management
- Same boilerplate as File 2
- `startAnimations()` suspend function
- Updated preview to not pass `animationStage` parameter
- Imports: `LaunchedEffect`, `collectAsState`, `rememberSaveable`, `delay`, `MutableStateFlow`, `flow.update`

**Simplified** to single function (lines 41-47):
```kotlin
@Composable
fun WooPosPaymentSuccessScreen(...) {
    val animationStage = remember { mutableStateOf(WooPosSuccessCheckmarkAnimationStage.INITIAL) }
    // ... UI code
    WooPosSuccessCheckmark(
        contentDescription = stringResource(R.string.woopos_payment_successful_label),
        onAnimationStageChanged = { stage -> animationStage.value = stage },
        // ...
    )
}
```

#### Benefits
- **Encapsulation**: Animation logic fully contained in the component
- **Simplicity**: Screens reduced from ~80 lines to ~40 lines each
- **Reusability**: Component is truly "plug and play" - just add it to the UI
- **Maintainability**: Animation changes only need to be made in one place
- **Less boilerplate**: No need to manage animation state in consuming screens
- **Self-contained**: Animation starts automatically on composition

#### Impact
- Removed ~80 lines of duplicate animation management code
- Each screen now only needs:
  1. Local state to track current animation stage (for spacing adjustments)
  2. Callback to update local state when animation progresses
- Component handles everything else internally

#### Testing
- ✅ All 32 unit tests pass
- ✅ Detekt passes with no violations
- ✅ Animation behavior unchanged
- ✅ Instrumented test detection works correctly
- ✅ No behavior changes - pure refactoring

### Layout Simplification (2026-01-29)

Simplified the success screen layouts by replacing ConstraintLayout with Column.

#### Problem
Both success screens were using ConstraintLayout for simple vertical stacking with centering. ConstraintLayout was overkill for this use case:
- All constraints were just horizontal centering (start.linkTo + end.linkTo)
- Vertical positioning was simple spacing between elements
- Required `@Suppress("DestructuringDeclarationWithTooManyEntries")` annotation
- More verbose than necessary (~30 extra lines per screen)

#### Solution
Replaced ConstraintLayout with Column + Spacer:

**File 1**: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/orders/WooPosIssueRefundDialog.kt`

**Removed**:
- `ConstraintLayout` wrapper
- `createRefs()` declarations
- `constrainAs()` modifiers on each element
- Import: `androidx.constraintlayout.compose.ConstraintLayout`

**Added**:
- `Column(horizontalAlignment = Alignment.CenterHorizontally)`
- `Spacer(Modifier.height(...))` between elements
- Column already imported

**File 2**: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/home/totals/payment/success/WooPosTotalsPaymentSuccessScreen.kt`

**Removed**:
- Same as File 1
- Import: `androidx.constraintlayout.compose.ConstraintLayout`

**Added**:
- Same as File 1
- Imports: `androidx.compose.foundation.layout.Column`, `androidx.compose.foundation.layout.Spacer`

#### Structure
```kotlin
Column(
    modifier = Modifier
        .fillMaxSize()
        .background(surfaceBright),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
) {
    WooPosSuccessCheckmark(...)
    Spacer(Modifier.height(XXXLarge))
    WooPosText(...) // title
    Spacer(Modifier.height(Small))
    WooPosText(...) // message
    Spacer(Modifier.height(marginBetweenButtonAndText)) // animated
    WooPosButton(...)
    Spacer(Modifier.height(Medium))
    WooPosOutlinedButton(...)
}
```

No Box wrapper needed - Column handles everything:
- Background color via modifier
- Vertical centering via `verticalArrangement = Arrangement.Center`
- Horizontal centering via `horizontalAlignment = Alignment.CenterHorizontally`

#### Benefits
- **Simplicity**: Single Column handles all layout needs (no Box wrapper)
- **Readability**: Spacer makes spacing explicit and easy to read
- **Less code**: Removed ~35 lines per screen (ConstraintLayout + Box wrapper)
- **No suppressions**: No need for DestructuringDeclarationWithTooManyEntries
- **Clearer intent**: Layout structure is immediately obvious
- **Fewer imports**: Removed ConstraintLayout, replaced Box with Arrangement

#### Testing
- ✅ All 32 unit tests pass
- ✅ Detekt passes with no violations
- ✅ Visual appearance unchanged
- ✅ Animation behavior preserved
- ✅ No behavior changes - pure refactoring

---

## PR Review Comments (2026-01-30)

### Date: 2026-01-30

### Summary of Review Feedback
GitHub Copilot reviewed the PR and identified 3 improvement areas:

1. **Payment Success Screen Layout Regression** (WooPosTotalsPaymentSuccessScreen.kt:60)
   - Changed from bottom-anchored button layout to centered vertically
   - Previous: Buttons constrained to parent bottom using ConstraintLayout
   - Current: `verticalArrangement = Arrangement.Center` centers entire content on screen
   - **Impact**: UX regression - buttons should be anchored near bottom
   - **Fix needed**: Revert to original Box + ConstraintLayout structure

2. **String Resource Naming Inconsistency** (strings.xml:3832-3833)
   - New strings `woopos_refund_complete` and `woopos_refund_success_message` break naming convention
   - All other refund strings use prefix: `woopos_orders_*`
   - **Fix needed**: Rename to `woopos_orders_refund_complete` and `woopos_orders_refund_success_message`
   - Update usages in:
     - WooPosIssueRefundDialog.kt (lines 286, 293, 304)

3. **Unused Import** (WooPosIssueRefundDialog.kt:5)
   - Import `androidx.compose.foundation.background` is not used
   - **Fix needed**: Remove the unused import

### Implementation Plan

#### Task 1: Revert Payment Success Screen Layout to Original Box + ConstraintLayout
**File**: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/home/totals/payment/success/WooPosTotalsPaymentSuccessScreen.kt`

**Rationale**: The original layout used Box + ConstraintLayout to anchor buttons near the bottom of the screen. The simplified Column with `verticalArrangement = Arrangement.Center` centers all content, which is a UX regression. Need to revert to original structure while keeping the shared `WooPosSuccessCheckmark` component.

**Changes**:
1. Replace Column with Box as the root container
2. Add ConstraintLayout inside Box
3. Use constraints to anchor buttons near bottom
4. Keep horizontal centering for all elements
5. Maintain shared `WooPosSuccessCheckmark` component

**Structure** (reverting to original):
```kotlin
Box(
    modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.surfaceBright)
) {
    ConstraintLayout(modifier = Modifier.fillMaxSize()) {
        val (checkmark, title, message, newOrderButton, receiptButton) = createRefs()

        WooPosSuccessCheckmark(
            contentDescription = stringResource(R.string.woopos_payment_successful_label),
            onAnimationStageChanged = { stage -> animationStage.value = stage },
            modifier = Modifier
                .constrainAs(checkmark) {
                    top.linkTo(parent.top, margin = XXXLarge)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
                .testTag(WooPosTestTags.SUCCESS_CHECKMARK_ICON)
        )

        WooPosText(
            text = stringResource(R.string.woopos_payment_successful_label),
            // ... styling
            modifier = Modifier.constrainAs(title) {
                top.linkTo(checkmark.bottom, margin = XXXLarge)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            }
        )

        WooPosText(
            text = state.orderTotalText,
            // ... styling
            modifier = Modifier.constrainAs(message) {
                top.linkTo(title.bottom, margin = Small)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            }
        )

        WooPosButton(
            // ... props
            modifier = Modifier
                .constrainAs(newOrderButton) {
                    bottom.linkTo(receiptButton.top, margin = marginBetweenButtonAndText)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
                .height(80.dp)
                .width(604.dp)
                .testTag(WooPosTestTags.NEW_ORDER_BUTTON)
        )

        WooPosOutlinedButton(
            // ... props
            modifier = Modifier
                .constrainAs(receiptButton) {
                    bottom.linkTo(parent.bottom, margin = XXXLarge)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
                .height(80.dp)
                .width(604.dp)
                .testTag(WooPosTestTags.EMAIL_RECEIPT_BUTTON)
        )
    }
}
```

**Imports to restore**:
- `androidx.constraintlayout.compose.ConstraintLayout`
- `androidx.compose.ui.layout.layoutId` (if needed)

**Imports to remove**:
- `androidx.compose.foundation.layout.Arrangement`
- `androidx.compose.foundation.layout.Column`
- `androidx.compose.foundation.layout.Spacer`

#### Task 2: Fix String Resource Naming
**File 1**: `WooCommerce/src/main/res/values/strings.xml` (line 3832-3833)
- Rename `woopos_refund_complete` → `woopos_orders_refund_complete`
- Rename `woopos_refund_success_message` → `woopos_orders_refund_success_message`

**File 2**: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/orders/WooPosIssueRefundDialog.kt`
- Update line 286: `R.string.woopos_refund_complete` → `R.string.woopos_orders_refund_complete`
- Update line 293: `R.string.woopos_refund_complete` → `R.string.woopos_orders_refund_complete`
- Update line 304: `R.string.woopos_refund_success_message` → `R.string.woopos_orders_refund_success_message`

#### Task 3: Remove Unused Import
**File**: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/orders/WooPosIssueRefundDialog.kt` (line 5)
- Remove: `import androidx.compose.foundation.background`
- Import is not used anywhere in the file (verified with grep - no `.background(` calls in the refund dialog)

### Testing Plan
After implementing fixes:
1. Run unit tests: `./gradlew :WooCommerce:testWasabiDebugUnitTest`
2. Run detekt: `./gradlew detektAll --auto-correct`
3. Visual verification:
   - Payment success screen buttons should be anchored near bottom
   - Refund success screen layout unchanged (already uses fillMaxWidth with Column, not fillMaxSize)
4. Build verification: `./gradlew assembleWasabiDebug`

### Expected Outcomes
- ✅ Payment success screen UX restored to original bottom-anchored layout
- ✅ Shared `WooPosSuccessCheckmark` component retained (code reuse maintained)
- ✅ String resources follow consistent `woopos_orders_*` naming convention
- ✅ No unused imports (cleaner codebase)
- ✅ All tests pass
- ✅ Detekt passes
- ✅ No visual or behavior regressions

---

## PR Review: Gateway Loading and Automatic Refunds (2026-01-30)

### Date: 2026-01-30

### Problem Summary
PR reviewer (@kidinov) raised a critical concern: "Is the missing gateway in the db is an error state or..?" and "If we would pass true all the time, even to the gateways which do not support refund - what would happen?"

**Current behavior was flawed**:
- `WooPosLoadPaymentGateway` returned `PaymentGateway` (never failed)
- Missing gateway → returned fallback `PaymentGateway(methodTitle = "manual")`
- `autoRefund = true` was always passed to refund API (hardcoded)
- This allowed refunds to proceed even when payment gateway was missing or didn't support refunds

**Root issue**: Missing gateway could mean:
1. **Data not synced** → Should fetch from API
2. **Invalid payment method** → Should **block refund** (not allow it)

### Solution Implemented

Changed `WooPosLoadPaymentGateway` to return `Result<PaymentGateway>` with proper error handling:

#### 1. Updated `WooPosLoadPaymentGateway.kt`

**New behavior** (lines 17-38):
```kotlin
suspend operator fun invoke(order: Order): Result<PaymentGateway> = withContext(coroutineDispatchers.io) {
    val site = selectedSite.get()

    // Try to get from DB first
    var gateway = gatewayStore.getGateway(site, order.paymentMethod)?.toAppModel()

    if (gateway == null) {
        // Not in DB - fetch from API
        val fetchResult = gatewayStore.fetchAllGateways(site)
        if (fetchResult.isError) {
            return@withContext Result.failure(
                Exception("Failed to fetch payment gateways: ${fetchResult.error.message}")
            )
        }

        // Try again after fetch
        gateway = gatewayStore.getGateway(site, order.paymentMethod)?.toAppModel()
    }

    return@withContext if (gateway != null) {
        Result.success(gateway)
    } else {
        Result.failure(Exception("Payment gateway '${order.paymentMethod}' not found"))
    }
}
```

**Key changes**:
- Return type: `PaymentGateway` → `Result<PaymentGateway>`
- If gateway not in DB → call `fetchAllGateways()` to sync from API
- If fetch fails → `Result.failure()` with error message
- If gateway still not found after fetch → `Result.failure()` (blocks refund)
- If gateway found (enabled or disabled) → `Result.success(gateway)`

#### 2. Updated `WooPosRefundViewModel.kt`

**Added error handling** in `processRefund()` (lines 308-320):
```kotlin
val paymentGatewayResult = loadPaymentGateway(order)
if (paymentGatewayResult.isFailure) {
    WooLog.e(
        WooLog.T.POS,
        "WooPosRefund: Failed to load payment gateway: ${paymentGatewayResult.exceptionOrNull()?.message}"
    )
    _state.value = WooPosRefundState.Error(
        message = resourceProvider.getString(R.string.woopos_refund_error_gateway_not_found)
    )
    return@launch
}

val paymentGateway = paymentGatewayResult.getOrThrow()

val result = refundStore.createItemsRefund(
    // ...
    autoRefund = paymentGateway.supportsRefunds,  // Dynamic based on gateway
    // ...
)
```

**Key changes**:
- Check if `Result.isFailure` → show error state (block refund)
- Extract gateway: `paymentGatewayResult.getOrThrow()`
- Use `autoRefund = paymentGateway.supportsRefunds` (dynamic, not hardcoded)

#### 3. Added error string resource

**File**: `WooCommerce/src/main/res/values/strings.xml` (line 3843)
```xml
<string name="woopos_refund_error_gateway_not_found">Unable to process refund. Payment method not supported.</string>
```

#### 4. Updated tests

**WooPosLoadPaymentGatewayTest.kt**:
- Updated all existing tests to handle `Result` type
- Added imports: `WooResult`, `WooError`, `verify`
- **New test cases**:
  1. `given payment gateway not in DB, when fetchAllGateways succeeds, then returns success with gateway`
     - Verifies fetch is called when gateway missing
     - Verifies gateway returned after successful fetch
  2. `given payment gateway not in DB, when fetchAllGateways fails, then returns failure`
     - Verifies failure when API fetch fails
  3. `given payment gateway not found after fetch, when invoke called, then returns failure`
     - Verifies failure when gateway truly doesn't exist

**Updated existing tests**:
- `given payment gateway supports refunds` → assert `result.isSuccess` + `result.getOrThrow().supportsRefunds`
- `given payment gateway does not support refunds` → assert `result.isSuccess` + `supportsRefunds = false`
- `given payment gateway is disabled` → assert `result.isSuccess` + `isEnabled = false` (returns gateway as-is, no longer returns fallback)

**WooPosRefundViewModelTest.kt**:
- Changed all `whenever(loadPaymentGateway.invoke(...)).thenReturn(gateway)`
- To: `whenever(loadPaymentGateway.invoke(...)).thenReturn(Result.success(gateway))`
- Updated 5 occurrences (default mock + 4 test-specific mocks)

### Behavior Changes

#### Before
1. Missing gateway → silent fallback to "manual" refund
2. `autoRefund = true` always (hardcoded)
3. No error when payment method not supported
4. Refund could proceed even when gateway missing

#### After
1. Missing gateway → fetch from API
2. If fetch fails or gateway not found → **block refund** with error message
3. `autoRefund = paymentGateway.supportsRefunds` (dynamic)
4. Clear error: "Unable to process refund. Payment method not supported."

### Edge Cases Handled

✅ **Gateway not synced yet**: Fetches from API automatically
✅ **API fetch fails**: Shows error, blocks refund
✅ **Truly unsupported payment method**: Shows error, blocks refund
✅ **Disabled gateway**: Returns gateway as-is (with `supportsRefunds` based on features)
✅ **Gateway without refund support**: `autoRefund = false` (manual refund)
✅ **Gateway with refund support**: `autoRefund = true` (automatic refund)

### Testing
- ✅ All 7 tests pass in `WooPosLoadPaymentGatewayTest` (4 updated + 3 new)
- ✅ All 32 tests pass in `WooPosRefundViewModelTest`
- ✅ Build successful
- ✅ No behavior regressions for working gateways

### Benefits

✅ **Defensive**: Fetches fresh data when gateway missing
✅ **Safe**: Blocks refunds for truly unsupported payment methods
✅ **Clear errors**: User sees explicit error message instead of silent fallback
✅ **Dynamic autoRefund**: Based on gateway capabilities, not hardcoded
✅ **Testable**: Result type makes success/failure cases explicit
✅ **Addresses reviewer concern**: No longer allows refunds with missing/invalid gateways
