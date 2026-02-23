# Wire FTS Search to Product Search UI — Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Connect FTS search results (including variations) to the POS product search UI, gated behind the `POS_PRODUCTS_FTS` feature flag.

**Architecture:** Convert `WooPosProductType` from enum to sealed class so `Variation` carries `parentProductName` with compile-time safety. Pass `parentProductName` from the FTS entity through the search result model, product model, and into the UI. Update the product card to show parent name as title and variation attributes as subtitle. Gate all user-facing changes behind the existing feature flag.

**Tech Stack:** Kotlin, Jetpack Compose (Material 3), Room, Hilt

---

### Task 1: Convert `WooPosProductType` from enum to sealed class

**Files:**
- Modify: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/common/data/models/WooPosProductModel.kt` (lines 71-82)

**Step 1: Convert the enum to a sealed class**

Replace the `WooPosProductType` enum inside `WooPosProductModel` with:

```kotlin
sealed class WooPosProductType(val value: String) : Parcelable {
    @Parcelize data object Simple : WooPosProductType("simple")
    @Parcelize data object Variable : WooPosProductType("variable")
    @Parcelize data object Grouped : WooPosProductType("grouped")
    @Parcelize data object External : WooPosProductType("external")
    @Parcelize data class Variation(val parentProductName: String) : WooPosProductType("variation")
    @Parcelize data object Subscription : WooPosProductType("subscription")
    @Parcelize data object VariableSubscription : WooPosProductType("variable-subscription")
    @Parcelize data object Custom : WooPosProductType("custom")
    @Parcelize data object Bundle : WooPosProductType("bundle")
    @Parcelize data object Composite : WooPosProductType("composite")
}
```

Note: `Variation` is now a `data class` with required `parentProductName`. All others are `data object`.

**Step 2: Fix all compilation errors from the enum-to-sealed-class change**

The compiler will flag every `WooPosProductType.SIMPLE`, `WooPosProductType.VARIABLE`, etc. Replace them:

| Old (enum)                          | New (sealed class)                     |
|-------------------------------------|----------------------------------------|
| `WooPosProductType.SIMPLE`          | `WooPosProductType.Simple`             |
| `WooPosProductType.VARIABLE`        | `WooPosProductType.Variable`           |
| `WooPosProductType.GROUPED`         | `WooPosProductType.Grouped`            |
| `WooPosProductType.EXTERNAL`        | `WooPosProductType.External`           |
| `WooPosProductType.VARIATION`       | `WooPosProductType.Variation(parentProductName)` |
| `WooPosProductType.SUBSCRIPTION`    | `WooPosProductType.Subscription`       |
| `WooPosProductType.VARIABLE_SUBSCRIPTION` | `WooPosProductType.VariableSubscription` |
| `WooPosProductType.CUSTOM`          | `WooPosProductType.Custom`             |
| `WooPosProductType.BUNDLE`          | `WooPosProductType.Bundle`             |
| `WooPosProductType.COMPOSITE`       | `WooPosProductType.Composite`          |

Files that need updating (production):

1. **`WooPosProductModelMapper.kt`** — `mapProductType()` and `fromVariationEntity()`:
   - `mapProductType()` return values: use new sealed class names
   - **Important:** `mapProductType("variation")` can no longer exist standalone — it needs a `parentProductName`. Since `mapProductType` doesn't have this context, remove the `"variation"` case from `mapProductType()` entirely (it was only used for remote API mapping where we don't have variation search results). The `fromVariationEntity()` method constructs `Variation` type directly.
   - In `fromVariationEntity()`: change `type = WooPosProductModel.WooPosProductType.VARIATION` to `type = WooPosProductModel.WooPosProductType.Variation(parentProductName = "")` as a temporary default. This will be updated properly in Task 3.

2. **`WooPosSearchByIdentifierResultConverter.kt`** (line 19):
   - Change `product.type == WooPosProductModel.WooPosProductType.VARIATION` to `product.type is WooPosProductModel.WooPosProductType.Variation`

3. **`WooPosProductsViewModel.kt`** (lines 398-400):
   - Change `type == WooPosProductModel.WooPosProductType.VARIABLE || type == WooPosProductModel.WooPosProductType.VARIATION` to `type is WooPosProductModel.WooPosProductType.Variable || type is WooPosProductModel.WooPosProductType.Variation`

4. **`WooPosItemsSearchViewModel.kt`** (line 411):
   - Change `type == WooPosProductModel.WooPosProductType.VARIABLE` to `type is WooPosProductModel.WooPosProductType.Variable`

Files that need updating (tests) — use same renaming pattern:

5. **`WooPosProductTestUtils.kt`** — default param: `WooPosProductType.Simple`
6. **`WooPosProductModelMapperTest.kt`** — all type references
7. **`WCProductToWooPosProductModelMapperTest.kt`** — type references
8. **`WooPosSearchByIdentifierResultConverterTest.kt`** — type references
9. **`WooPosSearchByIdentifierLocalTest.kt`** — type references
10. **`WooPosSearchByIdentifierTest.kt`** — type references
11. **`WooPosVariationsViewModelTest.kt`** — type references
12. **`WooPosProductsViewModelTest.kt`** — type references
13. **`WooPosProductsRemoteDataSourceTest.kt`** — type references
14. **`WooPosItemsSearchViewModelTest.kt`** — type references
15. **`WooPosProductsInDbDataSourceTest.kt`** — type references
16. **`WooPosProductsInMemoryCacheTest.kt`** — type references

**Step 3: Build and verify**

Run: `./gradlew assembleWasabiDebug`
Expected: BUILD SUCCESS

**Step 4: Run affected tests**

Run: `./gradlew :WooCommerce:testWasabiDebugUnitTest --tests "*.WooPosProductModelMapperTest" --tests "*.WooPosSearchByIdentifierResultConverterTest" --tests "*.WooPosProductsViewModelTest" --tests "*.WooPosItemsSearchViewModelTest"`
Expected: All PASS

**Step 5: Commit**

```bash
git add -A && git commit -m "Convert WooPosProductType from enum to sealed class"
```

---

### Task 2: Add `parentProductName` to `WooPosFtsSearchResult.Variation`

**Files:**
- Modify: `libs/fluxc-plugin/src/main/kotlin/org/wordpress/android/fluxc/store/pos/localcatalog/WooPosFtsSearchResult.kt`
- Modify: `libs/fluxc-plugin/src/main/kotlin/org/wordpress/android/fluxc/store/pos/localcatalog/WooPosLocalCatalogStore.kt` (line ~190)

**Step 1: Add field to the result model**

In `WooPosFtsSearchResult.kt`, change:
```kotlin
data class Variation(val entity: WooPosVariationEntity) : WooPosFtsSearchResult()
```
to:
```kotlin
data class Variation(
    val entity: WooPosVariationEntity,
    val parentProductName: String,
) : WooPosFtsSearchResult()
```

**Step 2: Pass `parentProductName` from the FTS entity in the store**

In `WooPosLocalCatalogStore.kt`, in `searchProductsFts()`, change (line ~190):
```kotlin
variationsMap[itemId]?.let { WooPosFtsSearchResult.Variation(it) }
```
to:
```kotlin
variationsMap[itemId]?.let {
    WooPosFtsSearchResult.Variation(
        entity = it,
        parentProductName = ftsEntity.name,
    )
}
```

The FTS entity's `name` field stores the parent product name for variations (see `WooPosSearchableFtsEntity` documentation).

**Step 3: Build and verify**

Run: `./gradlew assembleWasabiDebug`
Expected: BUILD SUCCESS

**Step 4: Commit**

```bash
git add -A && git commit -m "Add parentProductName to FTS search result variation"
```

---

### Task 3: Wire `parentProductName` through `WooPosProductModelMapper` and data source

**Files:**
- Modify: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/common/data/models/WooPosProductModelMapper.kt` — `fromVariationEntity()`
- Modify: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/home/items/search/WooPosProductsSearchInDbDataSource.kt` — `performFtsSearch()`

**Step 1: Add `parentProductName` parameter to `fromVariationEntity`**

In `WooPosProductModelMapper.kt`, change `fromVariationEntity` signature:
```kotlin
fun fromVariationEntity(entity: WooPosVariationEntity, parentProductName: String): WooPosProductModel {
```

And update the `type` assignment inside:
```kotlin
type = WooPosProductModel.WooPosProductType.Variation(parentProductName = parentProductName),
```

**Step 2: Update the FTS mapping in the data source**

In `WooPosProductsSearchInDbDataSource.kt`, in `performFtsSearch()`, change (line ~87):
```kotlin
is WooPosFtsSearchResult.Variation -> productMapper.fromVariationEntity(ftsResult.entity)
```
to:
```kotlin
is WooPosFtsSearchResult.Variation -> productMapper.fromVariationEntity(
    entity = ftsResult.entity,
    parentProductName = ftsResult.parentProductName,
)
```

**Step 3: Fix any other callers of `fromVariationEntity`**

Search for other calls to `fromVariationEntity`. If there are other callers that don't have `parentProductName` context, they need to be updated. Check `WooPosSearchByIdentifierResultConverter` and any barcode search paths.

**Step 4: Build and verify**

Run: `./gradlew assembleWasabiDebug`
Expected: BUILD SUCCESS

**Step 5: Run tests**

Run: `./gradlew :WooCommerce:testWasabiDebugUnitTest --tests "*.WooPosProductModelMapperTest"`
Expected: All PASS (update test to pass `parentProductName`)

**Step 6: Commit**

```bash
git add -A && git commit -m "Wire parentProductName through mapper and data source"
```

---

### Task 4: Handle variation type in search ViewModel

**Files:**
- Modify: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/home/items/search/WooPosItemsSearchViewModel.kt`
- Modify: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/home/items/WooPosItemSelectionViewState.kt`

**Step 1: Add `parentProductName` to the Variation view state**

In `WooPosItemSelectionViewState.kt`, update:
```kotlin
data class Variation(
    override val id: Long,
    override val name: String,
    override val price: String,
    override val imageUrl: String?,
    val productId: Long,
    val parentProductName: String,
) : Product(id, name, price, imageUrl)
```

**Step 2: Update `toViewModelProduct()` to handle variations**

In `WooPosItemsSearchViewModel.kt`, replace the `toViewModelProduct()` method (lines 410-427):

```kotlin
private suspend fun WooPosProductModel.toViewModelProduct(): WooPosItemSelectionViewState.Product =
    when (type) {
        is WooPosProductModel.WooPosProductType.Variable -> {
            WooPosItemSelectionViewState.Product.Variable(
                id = this.remoteId,
                name = this.name,
                price = priceFormat(this.pricing.displayPrice),
                imageUrl = this.firstImageUrl,
                numOfVariations = this.variationIds.size,
                variationIds = this.variationIds
            )
        }
        is WooPosProductModel.WooPosProductType.Variation -> {
            WooPosItemSelectionViewState.Product.Variation(
                id = this.remoteId,
                name = this.name,
                price = priceFormat(this.pricing.displayPrice),
                imageUrl = this.firstImageUrl,
                productId = this.parentId ?: 0L,
                parentProductName = type.parentProductName,
            )
        }
        else -> {
            WooPosItemSelectionViewState.Product.Simple(
                id = this.remoteId,
                name = this.name,
                price = priceFormat(this.pricing.displayPrice),
                imageUrl = this.firstImageUrl,
            )
        }
    }
```

**Step 3: Handle variation click in search results**

In `handleItemClicked()` (line 310-312), replace the `error()` with actual cart handling:

```kotlin
is WooPosItemSelectionViewState.Product.Variation -> {
    viewModelScope.launch {
        val itemData = ItemClickedData.Product.Variation(
            productId = item.productId,
            id = item.id,
        )
        childToParentEventSender.sendToParent(
            ChildToParentEvent.ItemClickedInItemsList(
                itemData = itemData,
                eventForTracking = WooPosAnalyticsEvent.Event.ItemAddedToCart(
                    item = itemData,
                    source = WooPosAnalyticsEventConstant.ItemsListSource.PRODUCT,
                    sourceType = sourceType,
                ),
            )
        )
    }
    storeRecentSearch()
}
```

**Step 4: Fix compilation in other ViewModels**

The `WooPosItemSelectionViewState.Product.Variation` constructor now requires `parentProductName`. Check other places that create `Variation` view states (e.g., `WooPosProductsViewModel` for the variations list) and pass an appropriate value (the parent product name is known in those contexts).

**Step 5: Build and verify**

Run: `./gradlew assembleWasabiDebug`
Expected: BUILD SUCCESS

**Step 6: Commit**

```bash
git add -A && git commit -m "Handle variation type in search ViewModel with cart support"
```

---

### Task 5: Update product card UI for search result variations

**Files:**
- Modify: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/home/items/WooPosItemsList.kt`

**Step 1: Update `ProductInfo` composable to show parent name for variations**

In the `ProductInfo` composable (around line 260), update the title to show parent name for variations:

```kotlin
@Composable
private fun ProductInfo(modifier: Modifier, item: Product) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .padding(
                top = WooPosSpacing.Medium.value,
                bottom = WooPosSpacing.Medium.value,
                end = WooPosSpacing.Medium.value
            ),
        verticalArrangement = Arrangement.Center
    ) {
        WooPosText(
            text = if (item is Product.Variation && item.parentProductName.isNotEmpty()) {
                item.parentProductName
            } else {
                item.name
            },
            style = WooPosTypography.BodyLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(WooPosSpacing.XSmall.value))
        when (item) {
            is Product.Simple -> SimpleProductDetails(item = item)
            is Product.Variable -> VariableProductDetails()
            is Product.Variation -> VariationProductDetails(item = item)
        }
    }
}
```

**Step 2: Update `VariationProductDetails` to show variation name and price**

Replace `VariationProductDetails` (around line 421):

```kotlin
@Composable
fun VariationProductDetails(item: Product.Variation) {
    if (item.parentProductName.isNotEmpty()) {
        WooPosText(
            text = item.name,
            style = WooPosTypography.BodyLarge,
            color = WooPosTheme.colors.onSurfaceVariantHighest,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(WooPosSpacing.XSmall.value))
    }
    WooPosText(
        text = item.price,
        style = WooPosTypography.BodyLarge,
        color = WooPosTheme.colors.onSurfaceVariantHighest,
    )
}
```

When `parentProductName` is present (FTS search result): shows variation attributes as first subtitle line, price below.
When `parentProductName` is empty (standard variation list): shows only price (existing behavior preserved).

**Step 3: Build and verify**

Run: `./gradlew assembleWasabiDebug`
Expected: BUILD SUCCESS

**Step 4: Commit**

```bash
git add -A && git commit -m "Update product card to show parent name for variation search results"
```

---

### Task 6: Update search placeholder behind feature flag

**Files:**
- Modify: `WooCommerce/src/main/res/values/strings.xml` — add new string
- Modify: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/home/items/WooPosItemsSearchHelper.kt`

**Step 1: Add new string resource**

In `WooCommerce/src/main/res/values/strings.xml`, add near the existing `woopos_search_products` string:
```xml
<string name="woopos_search_products_and_variations">Search products and variations</string>
```

**Step 2: Use feature flag to switch placeholder text**

In `WooPosItemsSearchHelper.kt`:

1. Add `IsPosProductsFtsEnabled` as a constructor dependency:
```kotlin
@ActivityRetainedScoped
class WooPosItemsSearchHelper @Inject constructor(
    private val resourceProvider: ResourceProvider,
    private val childToParentEventSender: WooPosChildrenToParentEventSender,
    private val parentToChildrenEventReceiver: WooPosParentToChildrenEventReceiver,
    private val productsDataSource: WooPosProductsDataSource,
    private val isFtsEnabled: IsPosProductsFtsEnabled,
) {
```

2. Update `updateToInitialOpenState()` (line 127-128):
```kotlin
val searchHintStringRes = when (viewStateFlow.value) {
    is WooPosItemsToolbarViewState.ProductList -> {
        if (isFtsEnabled()) {
            R.string.woopos_search_products_and_variations
        } else {
            R.string.woopos_search_products
        }
    }
    is WooPosItemsToolbarViewState.CouponList -> R.string.woopos_search_coupons
    is WooPosItemsToolbarViewState.VariationList -> error("Search is not applicable for variations list")
}
```

**Step 3: Build and verify**

Run: `./gradlew assembleWasabiDebug`
Expected: BUILD SUCCESS

**Step 4: Commit**

```bash
git add -A && git commit -m "Update search placeholder to include variations when FTS enabled"
```

---

### Task 7: Fix tests and run full verification

**Files:**
- Modify: Various test files that reference the changed types

**Step 1: Fix `WooPosProductTestUtils.kt`**

Ensure `generateWooPosProduct` default `productType` uses `WooPosProductType.Simple`. For tests that need variation type, pass `WooPosProductType.Variation(parentProductName = "Parent Product")`.

**Step 2: Fix `WooPosItemsSearchViewModelTest.kt`**

Update tests that create `Variation` view state objects to include `parentProductName`. Fix any tests around `handleItemClicked` that expect `error()` for variation clicks.

**Step 3: Fix `WooPosItemsSearchHelperTest.kt`**

Update the test to account for the new `IsPosProductsFtsEnabled` constructor dependency. Mock it.

**Step 4: Fix `WooPosProductModelMapperTest.kt`**

Update `mapProductType` tests — remove the `"variation" to VARIATION` mapping test since variations now need parentProductName and go through `fromVariationEntity`. Update `fromVariationEntity` tests to pass `parentProductName` and assert it's in the type.

**Step 5: Run all affected tests**

Run: `./gradlew :WooCommerce:testWasabiDebugUnitTest`
Expected: All PASS

**Step 6: Run detekt**

Run: `./gradlew detektAll --auto-correct`
Expected: BUILD SUCCESS

**Step 7: Commit**

```bash
git add -A && git commit -m "Fix tests for FTS search UI wiring"
```

---

## Key Decisions

- **Feature flag gating:** The search placeholder text change is gated behind `IsPosProductsFtsEnabled`. The variation display in product cards uses `parentProductName.isNotEmpty()` which is only set when FTS provides it — so the card behavior is implicitly gated.
- **No image fallback:** Variations with no image show the default placeholder icon.
- **Sealed class `WooPosProductType`:** Compile-time safety — `Variation` always carries `parentProductName`.
- **Click handling:** Variation search results add directly to cart (same as simple products), unlike variable products which open the variation picker.
