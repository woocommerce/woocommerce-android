package com.woocommerce.android.ui.products.inventory

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.ProductVariation
import com.woocommerce.android.model.UiString
import com.woocommerce.android.ui.orders.creation.CodeScannerStatus
import com.woocommerce.android.ui.orders.creation.GoogleBarcodeFormatMapper
import com.woocommerce.android.ui.products.ProductTestUtils
import com.woocommerce.android.ui.products.details.ProductDetailRepository
import com.woocommerce.android.ui.products.inventory.ScanToUpdateInventoryViewModel.ViewState.QuickInventoryBottomSheetHidden
import com.woocommerce.android.ui.products.inventory.ScanToUpdateInventoryViewModel.ViewState.QuickInventoryBottomSheetVisible
import com.woocommerce.android.ui.products.variations.VariationDetailRepository
import com.woocommerce.android.util.observeForTesting
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.eq
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.store.WCProductStore
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class ScanToUpdateInventoryViewModelTest : BaseUnitTest() {
    private val fetchProductBySKU: FetchProductBySKU = mock()
    private val savedStateHandle: SavedStateHandle = SavedStateHandle()
    private val resourceProvider: ResourceProvider = mock()
    private val productRepo: ProductDetailRepository = mock()
    private val variationRepo: VariationDetailRepository = mock()
    private val tracker: AnalyticsTrackerWrapper = mock()

    private lateinit var sut: ScanToUpdateInventoryViewModel

    @Before
    fun setUp() {
        sut = ScanToUpdateInventoryViewModel(
            fetchProductBySKU = fetchProductBySKU,
            savedState = savedStateHandle,
            resourceProvider = resourceProvider,
            productRepository = productRepo,
            variationRepository = variationRepo,
            tracker = tracker
        )
    }

    @Test
    fun `when screen opened, then bottom sheet should be hidden`() = testBlocking {
        sut.viewState.test {
            awaitItem().apply {
                assertIs<QuickInventoryBottomSheetHidden>(this)
            }
        }
    }

    @Test
    fun `when barcode successfully scanned, then should stop accepting new barcodes`() = testBlocking {
        sut.onBarcodeScanningResult(
            CodeScannerStatus.Success(
                "123",
                GoogleBarcodeFormatMapper.BarcodeFormat.FormatEAN8
            )
        )
        sut.onBarcodeScanningResult(
            CodeScannerStatus.Success(
                "123",
                GoogleBarcodeFormatMapper.BarcodeFormat.FormatEAN8
            )
        )
        verify(fetchProductBySKU, times(1)).invoke(any(), any())
    }

    @Test
    fun `given barcode successfully scanned, when product not found by sku, then should show error snackbar`() =
        testBlocking {
            whenever(fetchProductBySKU(any(), any())).thenReturn(Result.failure(Throwable()))
            whenever(
                resourceProvider.getString(
                    R.string.scan_to_update_inventory_unable_to_find_product,
                    "123"
                )
            ).thenReturn("Product with SKU: 123 not found. Please try again.")
            sut.event.observeForTesting {
                sut.onBarcodeScanningResult(
                    CodeScannerStatus.Success(
                        "123",
                        GoogleBarcodeFormatMapper.BarcodeFormat.FormatEAN8
                    )
                )
                sut.event.value.apply {
                    assertIs<MultiLiveEvent.Event.ShowUiStringSnackbar>(this)
                    assertEquals(
                        "Product with SKU: 123 not found. Please try again.",
                        (message as UiString.UiStringText).text
                    )
                }
            }
        }

    @Test
    fun `given barcode successfully scanned, when product not found by sku, then should hide progress bar and bottomsheet`() =
        testBlocking {
            whenever(fetchProductBySKU(any(), any())).thenReturn(Result.failure(Throwable()))
            whenever(
                resourceProvider.getString(
                    R.string.scan_to_update_inventory_unable_to_find_product,
                    "123"
                )
            ).thenReturn("Product with SKU: 123 not found. Please try again.")
            sut.viewState.test {
                sut.onBarcodeScanningResult(
                    CodeScannerStatus.Success(
                        "123",
                        GoogleBarcodeFormatMapper.BarcodeFormat.FormatEAN8
                    )
                )
                assertEquals(QuickInventoryBottomSheetHidden, awaitItem())
            }
        }

    @Test
    fun `given barcode successfully scanned, when product found by sku, then should show bottom sheet`() =
        testBlocking {
            whenever(fetchProductBySKU(any(), any())).thenReturn(
                Result.success(ProductTestUtils.generateProduct(isStockManaged = true))
            )
            sut.onBarcodeScanningResult(
                CodeScannerStatus.Success(
                    "123",
                    GoogleBarcodeFormatMapper.BarcodeFormat.FormatEAN8
                )
            )
            sut.viewState.test {
                awaitItem().apply {
                    assertIs<QuickInventoryBottomSheetVisible>(this)
                }
            }
        }

    @Test
    fun `given barcode successfully scanned, when corresponding product is not stock managed, then should display correct bottom sheet`() =
        testBlocking {
            whenever(fetchProductBySKU(any(), any())).thenReturn(
                Result.success(ProductTestUtils.generateProduct(isStockManaged = false).copy(sku = "123"))
            )
            sut.onBarcodeScanningResult(
                CodeScannerStatus.Success(
                    "123",
                    GoogleBarcodeFormatMapper.BarcodeFormat.FormatEAN8
                )
            )
            sut.viewState.test {
                awaitItem().apply {
                    assertIs<QuickInventoryBottomSheetVisible>(this)
                }
            }
        }

    @Test
    fun `given bottom sheet shown, when bottom sheet dismissed, then should should start scanning again`() =
        testBlocking {
            whenever(fetchProductBySKU(any(), any())).thenReturn(
                Result.success(ProductTestUtils.generateProduct(isStockManaged = true))
            )
            sut.onBarcodeScanningResult(
                CodeScannerStatus.Success(
                    "123",
                    GoogleBarcodeFormatMapper.BarcodeFormat.FormatEAN8
                )
            )
            sut.viewState.test {
                awaitItem().apply {
                    assertIs<QuickInventoryBottomSheetVisible>(this)
                }
            }
            sut.onBottomSheetDismissed()
            sut.viewState.test {
                awaitItem().apply {
                    assertIs<QuickInventoryBottomSheetHidden>(this)
                }
            }
            sut.onBarcodeScanningResult(
                CodeScannerStatus.Success(
                    "123",
                    GoogleBarcodeFormatMapper.BarcodeFormat.FormatEAN8
                )
            )
            verify(fetchProductBySKU, times(2)).invoke(any(), any())
        }

    @Test
    fun `given bottom sheet with product shown, when increment quantity clicked, then should should update product`() =
        testBlocking {
            val originalProduct = ProductTestUtils.generateProduct(isStockManaged = true)
            whenever(fetchProductBySKU(any(), any())).thenReturn(
                Result.success(originalProduct)
            )
            whenever(productRepo.getProduct(any())).thenReturn(originalProduct)
            sut.onBarcodeScanningResult(
                CodeScannerStatus.Success(
                    "123",
                    GoogleBarcodeFormatMapper.BarcodeFormat.FormatEAN8
                )
            )
            whenever(productRepo.updateProduct(any())).thenReturn(Pair(true, null))
            whenever(
                resourceProvider.getString(
                    R.string.scan_to_update_inventory_success_snackbar,
                    "${originalProduct.stockQuantity.toInt()} ➡ ${originalProduct.stockQuantity.toInt() + 1}"
                )
            ).thenReturn("Quantity updated")
            sut.viewState.test {
                awaitItem().apply {
                    assertIs<QuickInventoryBottomSheetVisible>(this)
                }
            }

            sut.onIncrementQuantityClicked()

            val expectedProduct =
                originalProduct.copy(stockQuantity = (originalProduct.stockQuantity.toInt() + 1).toDouble())
            verify(productRepo).updateProduct(expectedProduct)
        }

    @Test
    fun `given bottom sheet with product shown, when quantity entered manually, then should update product`() =
        testBlocking {
            val originalProduct = ProductTestUtils.generateProduct(isStockManaged = true)
            whenever(fetchProductBySKU(any(), any())).thenReturn(
                Result.success(originalProduct)
            )
            whenever(productRepo.getProduct(any())).thenReturn(originalProduct)
            sut.onBarcodeScanningResult(
                CodeScannerStatus.Success(
                    "123",
                    GoogleBarcodeFormatMapper.BarcodeFormat.FormatEAN8
                )
            )
            whenever(productRepo.updateProduct(any())).thenReturn(Pair(true, null))
            whenever(
                resourceProvider.getString(
                    R.string.scan_to_update_inventory_success_snackbar,
                    "${originalProduct.stockQuantity.toInt()} ➡ 999"
                )
            ).thenReturn("Quantity updated")
            sut.viewState.test {
                awaitItem().apply {
                    assertIs<QuickInventoryBottomSheetVisible>(this)
                }
            }

            sut.onManualQuantityEntered("999")
            sut.onUpdateQuantityClicked()

            val expectedProduct = originalProduct.copy(stockQuantity = (999).toDouble())
            verify(productRepo).updateProduct(expectedProduct)
        }

    @Test
    fun `given quantity updated, when undo action triggered, then should set quantity back to original`() =
        testBlocking {
            val originalQuantity = 5
            val newQuantity = 6
            val productId = 1L
            val product = ProductTestUtils.generateProduct(
                isStockManaged = true,
            )

            whenever(fetchProductBySKU(any(), any())).thenReturn(Result.success(product))
            whenever(productRepo.getProduct(productId)).thenReturn(product)
            whenever(productRepo.updateProduct(any())).thenReturn(Pair(true, null))
            whenever(
                resourceProvider.getString(
                    eq(R.string.scan_to_update_inventory_success_snackbar),
                    any()
                )
            ).thenReturn("Quantity updated from $originalQuantity to $newQuantity")
            whenever(
                resourceProvider.getString(
                    eq(R.string.scan_to_update_inventory_undo_snackbar)
                )
            ).thenReturn("Undo successful")

            val events = mutableListOf<MultiLiveEvent.Event>()
            sut.event.observeForever {
                events.add(it)
            }

            sut.onBarcodeScanningResult(
                CodeScannerStatus.Success(
                    product.sku,
                    GoogleBarcodeFormatMapper.BarcodeFormat.FormatEAN8
                )
            )

            sut.onIncrementQuantityClicked()

            (events.first() as MultiLiveEvent.Event.ShowUndoSnackbar).undoAction.onClick(null)

            verify(productRepo).updateProduct(product.copy(stockQuantity = originalQuantity.toDouble()))
        }

    @Test
    fun `given bottom sheet with variation shown, when increment quantity clicked, then should should update product`() =
        testBlocking {
            val productId = 1L
            val variationId = 2L
            val originalProduct =
                ProductTestUtils.generateProduct(isStockManaged = true, productId = variationId, parentID = productId)
                    .copy(stockQuantity = 1.0)
            whenever(fetchProductBySKU(any(), any())).thenReturn(
                Result.success(originalProduct)
            )
            whenever(productRepo.getProduct(any())).thenReturn(originalProduct)
            val originalVariation =
                ProductTestUtils.generateProductVariation(productId = productId, variationId = variationId)
                    .copy(stockQuantity = originalProduct.stockQuantity, isStockManaged = true)
            whenever(variationRepo.getVariationOrNull(productId, variationId)).thenReturn(originalVariation)
            whenever(variationRepo.updateVariation(any())).thenReturn(
                WCProductStore.OnVariationUpdated(
                    1,
                    1,
                    variationId
                )
            )
            whenever(
                resourceProvider.getString(
                    R.string.scan_to_update_inventory_success_snackbar,
                    "${originalProduct.stockQuantity.toInt()} ➡ ${originalProduct.stockQuantity.toInt() + 1}"
                )
            ).thenReturn("Quantity updated")
            sut.onBarcodeScanningResult(
                CodeScannerStatus.Success(
                    "123",
                    GoogleBarcodeFormatMapper.BarcodeFormat.FormatEAN8
                )
            )
            sut.viewState.test {
                awaitItem().apply {
                    assertIs<QuickInventoryBottomSheetVisible>(this)
                }
            }

            sut.onIncrementQuantityClicked()
            sut.onUpdateQuantityClicked()

            val argumentCaptor = argumentCaptor<ProductVariation>()
            verify(variationRepo).updateVariation(argumentCaptor.capture())
            assertEquals(originalVariation.stockQuantity + 1, argumentCaptor.firstValue.stockQuantity)
        }

    @Test
    fun `given bottom sheet with variation shown, when quantity entered manually, then should update product`() =
        testBlocking {
            val originalProduct = ProductTestUtils.generateProduct(isStockManaged = true, productId = 2, parentID = 1)
            whenever(fetchProductBySKU(any(), any())).thenReturn(
                Result.success(originalProduct)
            )
            whenever(productRepo.getProduct(any())).thenReturn(originalProduct)
            val originalVariation =
                ProductTestUtils.generateProductVariation(productId = 1, variationId = 2)
                    .copy(stockQuantity = 1.0, isStockManaged = true)
            whenever(variationRepo.getVariationOrNull(1, 2)).thenReturn(originalVariation)
            whenever(variationRepo.updateVariation(any())).thenReturn(WCProductStore.OnVariationUpdated(1, 1, 2))
            whenever(
                resourceProvider.getString(
                    R.string.scan_to_update_inventory_success_snackbar,
                    "${originalProduct.stockQuantity.toInt()} ➡ 999"
                )
            ).thenReturn("Quantity updated")

            sut.onBarcodeScanningResult(
                CodeScannerStatus.Success(
                    "123",
                    GoogleBarcodeFormatMapper.BarcodeFormat.FormatEAN8
                )
            )

            sut.viewState.test {
                awaitItem().apply {
                    assertIs<QuickInventoryBottomSheetVisible>(this)
                }
            }

            sut.onManualQuantityEntered("999")
            sut.onUpdateQuantityClicked()

            val expectedVariation = originalVariation.copy(stockQuantity = (999).toDouble())
            verify(variationRepo).updateVariation(expectedVariation)
        }

    @Test
    fun `given barcode scanned, when variation is found which is stock-managed, then should show bottom sheet`() =
        testBlocking {
            whenever(fetchProductBySKU(any(), any())).thenReturn(
                Result.success(
                    ProductTestUtils.generateProduct(
                        isStockManaged = true,
                        parentID = 1,
                        productId = 2
                    ).copy(sku = "123")
                )
            )
            whenever(variationRepo.getVariationOrNull(1, 2)).thenReturn(
                ProductTestUtils.generateProductVariation(
                    productId = 1,
                    variationId = 2
                ).copy(isStockManaged = true)
            )
            sut.viewState.test {
                sut.onBarcodeScanningResult(
                    CodeScannerStatus.Success(
                        "123",
                        GoogleBarcodeFormatMapper.BarcodeFormat.FormatEAN8
                    )
                )
                awaitItem().apply {
                    assertIs<QuickInventoryBottomSheetHidden>(this)
                }
                awaitItem().apply {
                    assertIs<QuickInventoryBottomSheetVisible>(this)
                }
            }
        }

    @Test
    fun `when variation is found which is not stock-managed, then should show bottom sheet`() =
        testBlocking {
            whenever(fetchProductBySKU(any(), any())).thenReturn(
                Result.success(
                    ProductTestUtils.generateProduct(
                        isStockManaged = true,
                        parentID = 1,
                        productId = 2
                    ).copy(sku = "123")
                )
            )
            whenever(variationRepo.getVariationOrNull(1, 2)).thenReturn(
                ProductTestUtils.generateProductVariation(
                    productId = 1,
                    variationId = 2
                ).copy(isStockManaged = false)
            )
            sut.viewState.test {
                sut.onBarcodeScanningResult(
                    CodeScannerStatus.Success(
                        "123",
                        GoogleBarcodeFormatMapper.BarcodeFormat.FormatEAN8
                    )
                )
                awaitItem().apply {
                    assertIs<QuickInventoryBottomSheetHidden>(this)
                }
                awaitItem().apply {
                    assertIs<QuickInventoryBottomSheetVisible>(this)
                }
            }
        }

    @Test
    fun `when manual quantity update button tapped, than trigger proper tracking event`() = testBlocking {
        sut.onManualQuantityEntered("999")
        sut.onUpdateQuantityClicked()
        verify(tracker).track(AnalyticsEvent.PRODUCT_QUICK_INVENTORY_UPDATE_MANUAL_QUANTITY_UPDATE_TAPPED)
    }

    @Test
    fun `when bottom sheet dismissed, then trigger proper tracking event`() = testBlocking {
        sut.onBottomSheetDismissed()
        verify(tracker).track(AnalyticsEvent.PRODUCT_QUICK_INVENTORY_UPDATE_DISMISSED)
    }

    @Test
    fun `when view prod details btn is clicked, then trigger proper tracking event`() = testBlocking {
        sut.onViewProductDetailsClicked()

        verify(tracker).track(AnalyticsEvent.PRODUCT_QUICK_INVENTORY_VIEW_PRODUCT_DETAILS_TAPPED)
    }

    @Test fun `given item is stock-managed, when bottom sheet is shown, then track proper event`() =
        testBlocking {
            val product = ProductTestUtils.generateProduct(isStockManaged = true)
            whenever(fetchProductBySKU(any(), any())).thenReturn(Result.success(product))

            sut.onBarcodeScanningResult(
                CodeScannerStatus.Success(
                    product.sku,
                    GoogleBarcodeFormatMapper.BarcodeFormat.FormatEAN8
                )
            )

            verify(tracker).track(
                AnalyticsEvent.PRODUCT_QUICK_INVENTORY_UPDATE_BOTTOM_SHEET_SHOWN,
                mapOf(AnalyticsTracker.KEY_ITEM_STOCK_MANAGED to true)
            )
        }

    @Test fun `given item is not stock-managed, when bottom sheet is shown, then track proper event`() =
        testBlocking {
            val product = ProductTestUtils.generateProduct(isStockManaged = false)
            whenever(fetchProductBySKU(any(), any())).thenReturn(Result.success(product))

            sut.onBarcodeScanningResult(
                CodeScannerStatus.Success(
                    product.sku,
                    GoogleBarcodeFormatMapper.BarcodeFormat.FormatEAN8
                )
            )

            verify(tracker).track(
                AnalyticsEvent.PRODUCT_QUICK_INVENTORY_UPDATE_BOTTOM_SHEET_SHOWN,
                mapOf(AnalyticsTracker.KEY_ITEM_STOCK_MANAGED to false)
            )
        }
}
