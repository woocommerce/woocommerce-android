package com.woocommerce.android.ui.products.inventory

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.woocommerce.android.R
import com.woocommerce.android.model.UiString
import com.woocommerce.android.ui.orders.creation.CodeScannerStatus
import com.woocommerce.android.ui.orders.creation.GoogleBarcodeFormatMapper
import com.woocommerce.android.ui.products.ProductDetailRepository
import com.woocommerce.android.ui.products.ProductTestUtils
import com.woocommerce.android.ui.products.inventory.ScanToUpdateInventoryViewModel.ViewState.BarcodeScanning
import com.woocommerce.android.ui.products.inventory.ScanToUpdateInventoryViewModel.ViewState.ProductLoading
import com.woocommerce.android.util.observeForTesting
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertIsNot

@OptIn(ExperimentalCoroutinesApi::class)
class ScanToUpdateInventoryViewModelTest : BaseUnitTest() {
    private val fetchProductBySKU: FetchProductBySKU = mock()
    private val savedStateHandle: SavedStateHandle = SavedStateHandle()
    private val resourceProvider: ResourceProvider = mock()
    private val repo: ProductDetailRepository = mock()

    private lateinit var sut: ScanToUpdateInventoryViewModel

    @Before
    fun setUp() {
        sut = ScanToUpdateInventoryViewModel(
            fetchProductBySKU = fetchProductBySKU,
            savedState = savedStateHandle,
            resourceProvider = resourceProvider,
            productRepository = repo,
        )
    }

    @Test
    fun `when screen opened, then should start scanning`() = testBlocking {
        sut.viewState.test {
            awaitItem().apply {
                assertIs<BarcodeScanning>(this)
            }
        }
    }

    @Test
    fun `when barcode successfully scanned, then should stop scanning`() = testBlocking {
        sut.onBarcodeScanningResult(
            CodeScannerStatus.Success(
                "123",
                GoogleBarcodeFormatMapper.BarcodeFormat.FormatEAN8
            )
        )
        sut.viewState.test {
            awaitItem().apply {
                assertIsNot<BarcodeScanning>(this)
            }
        }
    }

    @Test
    fun `given barcode successfully scanned, when product not found by sku, should show error snackbar`() =
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
    fun `given barcode successfully scanned, when product found by sku, should show bottom sheet`() =
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
                    assertIs<ScanToUpdateInventoryViewModel.ViewState.ProductLoaded>(this)
                }
            }
        }

    @Test
    fun `given barcode successfully scanned, when corresponding product is not stock managed, should show snackbar with error`() =
        testBlocking {
            whenever(fetchProductBySKU(any(), any())).thenReturn(
                Result.success(ProductTestUtils.generateProduct(isStockManaged = false))
            )
            whenever(
                resourceProvider.getString(
                    R.string.scan_to_update_inventory_product_not_stock_managed,
                    "123"
                )
            ).thenReturn("Product with SKU: 123 is not stock-managed. Please try again.")
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
                        "Product with SKU: 123 is not stock managed. Please try again.",
                        (message as UiString.UiStringText).text
                    )
                }
            }
        }

    @Test
    fun `given barcode successfully scanned, when corresponding product is not stock managed, should start scanning again`() =
        testBlocking {
            whenever(
                resourceProvider.getString(
                    R.string.scan_to_update_inventory_product_not_stock_managed,
                    "123"
                )
            ).thenReturn("Product with SKU: 123 is not stock-managed. Please try again.")
            val product = ProductTestUtils.generateProduct(isStockManaged = false).copy(
                sku = "123"
            )
            whenever(fetchProductBySKU(any(), any())).thenReturn(
                Result.success(product)
            )
            sut.onBarcodeScanningResult(
                CodeScannerStatus.Success(
                    "123",
                    GoogleBarcodeFormatMapper.BarcodeFormat.FormatEAN8
                )
            )
            sut.viewState.test {
                awaitItem().apply {
                    assertIs<ProductLoading>(this)
                }
                awaitItem().apply {
                    assertIs<BarcodeScanning>(this)
                }
            }
        }

    @Test
    fun `given bottom sheet shown, when bottom sheet dismissed, should should start scanning again`() = testBlocking {
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
                assertIs<ScanToUpdateInventoryViewModel.ViewState.ProductLoaded>(this)
            }
        }
        sut.onBottomSheetDismissed()
        sut.viewState.test {
            awaitItem().apply {
                assertIs<BarcodeScanning>(this)
            }
        }
    }

    @Test
    fun `given bottom sheet shown, when increment quantity clicked, then should should update product`() = testBlocking {
        val originalProduct = ProductTestUtils.generateProduct(isStockManaged = true)
        whenever(fetchProductBySKU(any(), any())).thenReturn(
            Result.success(originalProduct)
        )
        whenever(repo.getProduct(any())).thenReturn(originalProduct)
        sut.onBarcodeScanningResult(
            CodeScannerStatus.Success(
                "123",
                GoogleBarcodeFormatMapper.BarcodeFormat.FormatEAN8
            )
        )
        whenever(repo.updateProduct(any())).thenReturn(true)
        whenever(resourceProvider.getString(
            R.string.scan_to_update_inventory_success_snackbar,
            "${originalProduct.stockQuantity} ➡ ${originalProduct.stockQuantity + 1}"
        )).thenReturn("Quantity updated")
        sut.viewState.test {
            awaitItem().apply {
                assertIs<ScanToUpdateInventoryViewModel.ViewState.ProductLoaded>(this)
            }
        }

        sut.onIncrementQuantityClicked()

        val expectedProduct = originalProduct.copy(stockQuantity = originalProduct.stockQuantity + 1)
        verify(repo).updateProduct(expectedProduct)
    }
}
