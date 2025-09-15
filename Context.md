# Task Context

## Initial Request
User requested to add logging for caught exceptions in `WooPosVariation.kt`: "in WooPosVariation.kt when exception is cought, let's log it."

## User Follow-up Request
User requested refactoring: "Having an instance of gson per instance feels a bit off. Can you create a separate mapper class?"

## Completed Steps

### 1. Added Exception Logging to WooPosVariation.kt
- Added import for `WooLog` utility: `import com.woocommerce.android.util.WooLog`
- Added logging to three functions that catch `JsonSyntaxException`:
  - `WCProductVariationModel.toWooPosVariation()` - image parsing: `WooLog.w(WooLog.T.POS, "Failed to parse image JSON for variation ${remoteVariationId.value}: ${e.message}")`
  - `WCPosVariationModel.toWooPosVariation()` - attributes parsing: `WooLog.w(WooLog.T.POS, "Failed to parse attributes JSON for variation ${remoteVariationId.value}: ${e.message}")`
  - `parseAttributesJson()` - JSON parsing: `WooLog.w(WooLog.T.POS, "Failed to parse attributes JSON: ${e.message}")`

### 2. Fixed Test Compilation Issues
During testing, discovered compilation errors in test files due to type mismatches after the transition from `ProductVariation` to `WooPosVariation`:

#### WooPosCartViewModelTest.kt
- Fixed type mismatch where `ProductVariation` objects were being passed to methods expecting `WooPosVariation`
- Added import: `import com.woocommerce.android.ui.woopos.common.data.toWooPosVariation`
- Updated variation creation from `ProductTestUtils.generateProductVariation()` to `ProductTestUtils.generateProductVariation().toWooPosVariation()`
- Removed unused `ProductVariation` import after conversion

#### WooPosVariationsViewModelTest.kt
- Fixed constructor parameter order and missing parameters for `WooPosVariationsViewModel`
- Added imports for missing dependencies:
  - `import com.woocommerce.android.ui.woopos.home.items.variations.WooPosVariationsInDbDataSource`
  - `import com.woocommerce.android.ui.woopos.featureflags.WooPosLocalCatalogM1Enabled`
- Added missing mock declarations:
  - `private val variationsInDbDataSource: WooPosVariationsInDbDataSource = mock()`
  - `private val wooPosLocalCatalogM1Enabled: WooPosLocalCatalogM1Enabled = mock()`
- Updated constructor call to include all required parameters in correct order

### 3. Code Quality Verification
- All unit tests pass: `./gradlew :WooCommerce:testWasabiDebugUnitTest --tests "*WooPosVariation*"`
- All linting issues resolved: `./gradlew detektAll --auto-correct`
- Code compilation successful with no errors

## Outcome
- Primary objective completed: Exception logging added to all `JsonSyntaxException` catch blocks in `WooPosVariation.kt`
- Secondary issues resolved: Test compilation errors fixed to ensure codebase remains functional
- Code quality maintained: No linting issues, all tests pass

### 3. Refactored JSON Parsing to Separate Mapper Class
Following user feedback about having a Gson instance per function, created a dedicated mapper class:

#### WooPosVariationAttributeMapper.kt (ORIGINAL MAPPER)
- Created singleton mapper class with centralized Gson instance management
- Added `parseAttributesJson()` method to handle JSON parsing logic
- Implemented singleton pattern with thread-safe getInstance() method
- Centralized exception handling and logging for JSON parsing failures

### 4. Complete Extraction to Full Variation Mapper
Following user request to "extract all the parsing and mapping functions from the WooPosVariation.kt to the mapper":

#### WooPosVariationMapper.kt (RENAMED & EXPANDED)
- Renamed from `WooPosVariationAttributeMapper.kt` to reflect expanded scope
- Extracted all conversion functions from extension functions to mapper methods:
  - `fromProductVariation(productVariation: ProductVariation): WooPosVariation`
  - `fromWCProductVariationModel(model: WCProductVariationModel): WooPosVariation`
  - `fromWCPosVariationModel(model: WCPosVariationModel): WooPosVariation`
  - `getNameForPOS(variation: WooPosVariation, parentProduct: Product?, resourceProvider: ResourceProvider): String`
  - `getName(variation: WooPosVariation, parentProduct: Product?): String`
- Centralized all business logic, JSON parsing, and conversion logic in one place
- Made `parseAttributesJson()` private since it's now an internal implementation detail

#### Updated WooPosVariation.kt (SIMPLIFIED)
- Removed all business logic and conversion implementations
- Kept only the data class definitions and simple extension function delegates
- Extension functions now simply call mapper methods for backward compatibility:
  ```kotlin
  fun ProductVariation.toWooPosVariation(): WooPosVariation =
      WooPosVariationMapper.getInstance().fromProductVariation(this)
  ```
- Significantly reduced file size and complexity
- Maintained API compatibility while centralizing all logic

### 5. Fixed Failing Unit Tests in WooPosVariationsViewModelTest.kt

Following user request: "I need you to fix the remaining failing tests in WooPosVariationsViewModelTest.kt. The tests are failing because they need `advanceUntilIdle()` added after `createViewModel()` calls for async operations to complete."

#### Issues Identified and Fixed:

1. **Async Timing Issues**: Multiple tests were failing with `ClassCastException` when trying to cast the first emitted state to `WooPosVariationsViewState.Content`. The issue was that `createViewModel()` starts async operations, but tests were immediately checking the view state before async operations completed.

2. **Mock Setup Issues**: The test `given no parent product, when getNameForPOS is called, then it returns variation attributes` was failing because the mock was not being invoked due to imprecise type matching in the mock declaration.

#### Solutions Implemented:

1. **Added `advanceUntilIdle()` calls**: Added `advanceUntilIdle()` after `createViewModel()` calls in the following tests:
   - `given variations, when load more succeeds, then pagination state is updated`
   - `given fetching variations first page and load more call is not happening, when view model created, then view state updated correctly`
   - `given variation clicked and source is product list, when item clicked, then sends event with product list source`
   - `given search results screen, when variation clicked, then sends event with search results as source`
   - `when pull to refresh triggered, then should track event`

2. **Fixed Mock Type Matching**: Changed the mock declaration from:
   ```kotlin
   on { getNameForPOS(any(), any(), any()) } doAnswer { ... }
   ```
   to:
   ```kotlin
   on { getNameForPOS(any<WooPosVariation>(), anyOrNull(), any<ResourceProvider>()) } doAnswer { ... }
   ```
   This ensures the mock is invoked correctly with the specific parameter types.

#### Verification:
- All 20 tests in `WooPosVariationsViewModelTest.kt` now pass
- The specific failing test `given no parent product, when getNameForPOS is called, then it returns variation attributes` now correctly returns "Color: Blue, Size: M" as expected

### 6. Fixed Failing Unit Tests in WooPosCartViewModelTest.kt

Following user request: "Fix the 3 failing tests in WooPosCartViewModelTest.kt that are showing: 1. `given barcode scanned, when variation found, then variation added to cart` - NullPointerException at line 1349; 2. `when back from checkout to cart, then coupon validation states should be reset to UNKNOWN` - UnfinishedStubbingException at line 70; 3. `given empty cart, when variation clicked, then should add variation to cart` - NullPointerException at line 149"

#### Issues Identified and Fixed:

1. **Missing WooPosVariationMapper Mock Setup**: The `variationMapper` mock was declared but not properly configured to handle calls to `fromProductVariation()` method.

2. **Missing Imports**: The test file was missing the `doAnswer` import from mockito-kotlin and the `ProductVariation` import for type references.

3. **NullPointerExceptions**: The `toWooPosVariation(variationMapper)` extension function calls were failing because the mock mapper's `fromProductVariation()` method was not implemented.

#### Solutions Implemented:

1. **Added Missing Import**: Added `import org.mockito.kotlin.doAnswer` to the imports.

2. **Added Missing Model Imports**: Added `import com.woocommerce.android.model.ProductVariation` and `import com.woocommerce.android.ui.woopos.common.data.WooPosVariation`.

3. **Implemented Mock Mapper**: Replaced the simple `mock()` declaration with a complete mock implementation:
   ```kotlin
   private val variationMapper: WooPosVariationMapper = mock {
       on { fromProductVariation(any()) } doAnswer { invocation ->
           val productVariation = invocation.arguments[0] as ProductVariation
           WooPosVariation(
               remoteVariationId = productVariation.remoteVariationId,
               remoteProductId = productVariation.remoteProductId,
               globalUniqueId = productVariation.globalUniqueId,
               price = productVariation.price,
               image = productVariation.image?.let { WooPosVariation.WooPosVariationImage(it.source) },
               attributes = productVariation.attributes.map {
                   WooPosVariation.WooPosVariationAttribute(
                       id = it.id,
                       name = it.name,
                       option = it.option
                   )
               },
               isVisible = productVariation.isVisible,
               isDownloadable = productVariation.isDownloadable
           )
       }
   }
   ```

#### Verification:
- All 3 originally failing tests now pass
- All tests in `WooPosCartViewModelTest` continue to pass (no regressions introduced)
- The mock properly converts `ProductVariation` objects to `WooPosVariation` objects using the same logic as the real mapper

## Files Modified
1. `/WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/common/data/WooPosVariation.kt` - Added exception logging, completely refactored to delegate to mapper
2. `/WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/common/data/WooPosVariationMapper.kt` - NEW: Complete conversion and mapping utility (renamed from AttributeMapper)
3. `/WooCommerce/src/test/kotlin/com/woocommerce/android/ui/woopos/home/cart/WooPosCartViewModelTest.kt` - Fixed type mismatches and WooPosVariationMapper mock setup
4. `/WooCommerce/src/test/kotlin/com/woocommerce/android/ui/woopos/home/variations/WooPosVariationsViewModelTest.kt` - Fixed constructor parameters and async timing issues