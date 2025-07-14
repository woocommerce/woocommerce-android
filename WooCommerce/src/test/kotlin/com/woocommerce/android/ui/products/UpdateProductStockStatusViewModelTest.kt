package com.woocommerce.android.ui.products

import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.products.UpdateProductStockStatusViewModel.ProductStockStatusInfo
import com.woocommerce.android.ui.products.UpdateProductStockStatusViewModel.StockStatusState
import com.woocommerce.android.ui.products.UpdateProductStockStatusViewModel.UpdateStockStatusExitState
import com.woocommerce.android.ui.products.UpdateProductStockStatusViewModel.UpdateStockStatusResult
import com.woocommerce.android.ui.products.UpdateProductStockStatusViewModel.UpdateStockStatusUiState
import com.woocommerce.android.ui.products.list.ProductListRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UpdateProductStockStatusViewModelTest : BaseUnitTest() {
    private val productListRepository: ProductListRepository = mock()
    private val analyticsTracker: AnalyticsTrackerWrapper = mock()

    private lateinit var viewModel: UpdateProductStockStatusViewModel
    private val resourceProvider: ResourceProvider = mock {
        on { getString(any()) } doAnswer { invocation ->
            when (invocation.arguments[0] as Int) {
                R.string.product_update_stock_status_update_count_singular -> {
                    "Stock status will be updated for 1 product."
                }

                R.string.product_update_stock_status_ignored_count_singular -> {
                    "1 product with managed stock quantity will be ignored."
                }

                R.string.product_update_stock_status_variable_ignored_count_singular -> {
                    "1 variable product will be ignored."
                }

                else -> "Unexpected resource ID"
            }
        }
        on { getString(any(), any()) } doAnswer { invocation ->
            val resourceId = invocation.arguments[0] as Int
            val formatArg = invocation.arguments[1]
            when (resourceId) {
                R.string.product_update_stock_status_update_count -> {
                    "Stock status will be updated for $formatArg products."
                }

                R.string.product_update_stock_status_ignored_count -> {
                    "$formatArg products with managed stock quantity will be ignored."
                }

                R.string.product_update_stock_status_variable_ignored_count -> {
                    "$formatArg variable products will be ignored."
                }

                else -> "Default String with $formatArg"
            }
        }
    }

    @Test
    fun `given mixed stock statuses, when viewModel is initialized, then ui state reflects mixed status`() =
        testBlocking {
            // Given
            val stockStatusInfos = listOf(
                ProductStockStatusInfo(
                    productId = 1L,
                    stockStatus = ProductStockStatus.InStock,
                    manageStock = false,
                    isVariable = false
                ),
                ProductStockStatusInfo(
                    productId = 2L,
                    stockStatus = ProductStockStatus.OutOfStock,
                    manageStock = false,
                    isVariable = false
                )
            )
            mockFetchStockStatuses(stockStatusInfos)
            setupViewModel(stockStatusInfos.map { it.productId })

            // When
            var state: UpdateStockStatusUiState? = null
            viewModel.viewState.observeForever { state = it }

            // Then
            assertThat(state?.currentStockStatusState).isInstanceOf(StockStatusState.Mixed::class.java)
        }

    @Test
    fun `given products with the same stock status, when viewModel is init, then state reflects common status`() =
        testBlocking {
            // Given
            val selectedProductIds = listOf(1L, 2L)
            val commonStockStatus = ProductStockStatus.InStock
            mockFetchStockStatuses(selectedProductIds, commonStockStatus, false)

            // When
            setupViewModel(selectedProductIds)

            var state: UpdateStockStatusUiState? = null
            viewModel.viewState.observeForever { state = it }

            // Then
            assertThat(state?.currentStockStatusState).isEqualTo(StockStatusState.Common(commonStockStatus))
        }

    @Test
    fun `given viewModel is init, when setCurrentStockStatus is called, Then UI state updates current stock status`() =
        testBlocking {
            // Given
            val selectedProductIds = listOf(1L)
            mockFetchStockStatuses(selectedProductIds, ProductStockStatus.InStock, false)

            setupViewModel(selectedProductIds)
            val newStockStatus = ProductStockStatus.OutOfStock

            // When
            viewModel.onStockStatusSelected(newStockStatus)

            var state: UpdateStockStatusUiState? = null
            viewModel.viewState.observeForever { state = it }

            // Then
            assertThat(state?.currentProductStockStatus).isEqualTo(newStockStatus)
        }

    @Test
    fun `given viewModel is init, when updateStockStatusForProducts is called then analytics event is tracked`() =
        testBlocking {
            // Given
            val selectedProductIds = listOf(1L, 2L)
            mockFetchStockStatuses(selectedProductIds, ProductStockStatus.InStock, false)
            setupViewModel(selectedProductIds)

            // When
            viewModel.onDoneButtonClicked()

            // Then
            verify(analyticsTracker).track(AnalyticsEvent.PRODUCT_STOCK_STATUSES_UPDATE_DONE_TAPPED)
        }

    @Test
    fun `when stock status is updated, then ExitWithResult event is dispatched`() = testBlocking {
        // Given
        val selectedProductIds = listOf(1L, 2L)
        mockFetchStockStatuses(selectedProductIds, ProductStockStatus.InStock, false)
        setupViewModel(selectedProductIds)

        mockBulkUpdateStockStatus(selectedProductIds, ProductStockStatus.InStock, UpdateStockStatusResult.Updated)

        var event: MultiLiveEvent.Event? = null
        viewModel.event.observeForever { event = it }

        // When
        viewModel.onDoneButtonClicked()

        // Then
        assertThat(event).isInstanceOf(MultiLiveEvent.Event.ExitWithResult::class.java)
        assertThat((event as MultiLiveEvent.Event.ExitWithResult<*>).data).isEqualTo(
            UpdateStockStatusExitState.Success
        )
    }

    @Test
    fun `when stock status update fails, then ExitWithResult event with Error is dispatched`() = testBlocking {
        // Given
        val selectedProductIds = listOf(1L, 2L)
        mockFetchStockStatuses(selectedProductIds, ProductStockStatus.InStock, false)
        setupViewModel(selectedProductIds)
        mockBulkUpdateStockStatus(selectedProductIds, ProductStockStatus.InStock, UpdateStockStatusResult.Error)

        var event: MultiLiveEvent.Event? = null
        viewModel.event.observeForever { event = it }

        // When
        viewModel.onDoneButtonClicked()

        // Then
        assertThat(event).isInstanceOf(MultiLiveEvent.Event.ExitWithResult::class.java)
        assertThat((event as MultiLiveEvent.Event.ExitWithResult<*>).data).isEqualTo(
            UpdateStockStatusExitState.Error
        )
    }

    @Test
    fun `when stock status update is managed products, then ExitWithResult event with Error is dispatched`() =
        testBlocking {
            // Given
            val selectedProductIds = listOf(1L, 2L)
            mockFetchStockStatuses(selectedProductIds, ProductStockStatus.InStock, false)
            setupViewModel(selectedProductIds)
            mockBulkUpdateStockStatus(
                selectedProductIds,
                ProductStockStatus.InStock,
                UpdateStockStatusResult.IsManagedProducts
            )

            var event: MultiLiveEvent.Event? = null
            viewModel.event.observeForever { event = it }

            // When
            viewModel.onDoneButtonClicked()

            // Then
            assertThat(event).isInstanceOf(MultiLiveEvent.Event.ExitWithResult::class.java)
            assertThat((event as MultiLiveEvent.Event.ExitWithResult<*>).data).isEqualTo(
                UpdateStockStatusExitState.Error
            )
        }

    @Test
    fun `when one product is eligible for update, correct singular status message is shown`() = testBlocking {
        // Given
        val stockStatusInfos = listOf(
            ProductStockStatusInfo(
                productId = 1L,
                stockStatus = ProductStockStatus.InStock,
                manageStock = false,
                isVariable = false
            )
        )
        mockFetchStockStatusesWithManageStock(stockStatusInfos)
        setupViewModel(stockStatusInfos.map { it.productId })

        // When
        var state: UpdateStockStatusUiState? = null
        viewModel.viewState.observeForever { state = it }

        // Then
        val expectedMessage = "Stock status will be updated for 1 product."
        assertThat(state?.statusMessage).isEqualTo(expectedMessage)
    }

    @Test
    fun `when all products are eligible for update, correct status message is shown`() = testBlocking {
        // Given
        val stockStatusInfos = listOf(
            ProductStockStatusInfo(
                productId = 1L,
                stockStatus = ProductStockStatus.InStock,
                manageStock = false,
                isVariable = false
            ),
            ProductStockStatusInfo(
                productId = 2L,
                stockStatus = ProductStockStatus.OutOfStock,
                manageStock = false,
                isVariable = false
            )
        )
        mockFetchStockStatusesWithManageStock(stockStatusInfos)
        setupViewModel(stockStatusInfos.map { it.productId })

        // When
        var state: UpdateStockStatusUiState? = null
        viewModel.viewState.observeForever { state = it }

        // Then
        val expectedMessage = "Stock status will be updated for 2 products."
        assertThat(state?.statusMessage).isEqualTo(expectedMessage)
    }

    @Test
    fun `when some products have managed stock, correct status message is shown`() = testBlocking {
        // Given
        val stockStatusInfos = listOf(
            ProductStockStatusInfo(
                productId = 1L,
                stockStatus = ProductStockStatus.InStock,
                manageStock = false,
                isVariable = false
            ),
            ProductStockStatusInfo(
                productId = 2L,
                stockStatus = ProductStockStatus.OutOfStock,
                manageStock = true,
                isVariable = false
            )
        )
        mockFetchStockStatusesWithManageStock(stockStatusInfos)
        setupViewModel(stockStatusInfos.map { it.productId })

        // When
        var state: UpdateStockStatusUiState? = null
        viewModel.viewState.observeForever { state = it }

        // Then
        val expectedMessage =
            "Stock status will be updated for 1 product. 1 product with managed stock quantity will be ignored."
        assertThat(state?.statusMessage).isEqualTo(expectedMessage)
    }

    @Test
    fun `when one product is eligible for update and multiple are ignored, correct status messages are shown`() =
        testBlocking {
            // Given
            val stockStatusInfos = listOf(
                ProductStockStatusInfo(
                    productId = 1L,
                    stockStatus = ProductStockStatus.InStock,
                    manageStock = false,
                    isVariable = false
                ),
                ProductStockStatusInfo(
                    productId = 2L,
                    stockStatus = ProductStockStatus.OutOfStock,
                    manageStock = true,
                    isVariable = false
                ),
                ProductStockStatusInfo(
                    productId = 3L,
                    stockStatus = ProductStockStatus.OutOfStock,
                    manageStock = true,
                    isVariable = false
                )
            )
            mockFetchStockStatusesWithManageStock(stockStatusInfos)
            setupViewModel(stockStatusInfos.map { it.productId })

            // When
            var state: UpdateStockStatusUiState? = null
            viewModel.viewState.observeForever { state = it }

            // Then
            val expectedMessage =
                "Stock status will be updated for 1 product. 2 products with managed stock quantity will be ignored."
            assertThat(state?.statusMessage).isEqualTo(expectedMessage)
        }

    @Test
    fun `when variable products are ignored, correct status messages are shown`() = testBlocking {
        // Given
        val stockStatusInfos = listOf(
            ProductStockStatusInfo(
                productId = 1L,
                stockStatus = ProductStockStatus.InStock,
                manageStock = false,
                isVariable = false
            ),
            ProductStockStatusInfo(
                productId = 2L,
                stockStatus = ProductStockStatus.OutOfStock,
                manageStock = false,
                isVariable = true
            )
        )
        mockFetchStockStatusesWithManageStock(stockStatusInfos)
        setupViewModel(stockStatusInfos.map { it.productId })

        // When
        var state: UpdateStockStatusUiState? = null
        viewModel.viewState.observeForever { state = it }

        // Then
        val expectedMessage =
            "Stock status will be updated for 1 product. 1 variable product will be ignored."
        assertThat(state?.statusMessage).isEqualTo(expectedMessage)
    }

    @Test
    fun `when only variable products are selected, update is blocked and correct event is dispatched`() = testBlocking {
        // Given
        val selectedProductIds = listOf(1L, 2L)
        mockFetchStockStatuses(selectedProductIds, ProductStockStatus.InStock, false, isVariable = true)
        setupViewModel(selectedProductIds)
        mockBulkUpdateStockStatus(
            selectedProductIds,
            ProductStockStatus.InStock,
            UpdateStockStatusResult.IsVariableProducts
        )

        var event: MultiLiveEvent.Event? = null
        viewModel.event.observeForever { event = it }

        // When
        viewModel.onDoneButtonClicked()

        // Then
        assertThat(event).isInstanceOf(MultiLiveEvent.Event.ExitWithResult::class.java)
        assertThat((event as MultiLiveEvent.Event.ExitWithResult<*>).data).isEqualTo(
            UpdateStockStatusExitState.Error
        )
    }

    @Test
    fun `given all products are OutOfStock, when viewModel is initialized, then OutOfStock is pre-selected`() =
        testBlocking {
            // Given
            val stockStatusInfos = listOf(
                ProductStockStatusInfo(
                    productId = 1L,
                    stockStatus = ProductStockStatus.OutOfStock,
                    manageStock = false,
                    isVariable = false
                ),
                ProductStockStatusInfo(
                    productId = 2L,
                    stockStatus = ProductStockStatus.OutOfStock,
                    manageStock = false,
                    isVariable = false
                )
            )
            mockFetchStockStatuses(stockStatusInfos)
            setupViewModel(stockStatusInfos.map { it.productId })

            // When
            var state: UpdateStockStatusUiState? = null
            viewModel.viewState.observeForever { state = it }

            // Then
            assertThat(state?.currentProductStockStatus).isEqualTo(ProductStockStatus.OutOfStock)
        }

    @Test
    fun `given products have mixed stock statuses, when viewModel is initialized, then InStock is pre-selected`() =
        testBlocking {
            // Given
            val stockStatusInfos = listOf(
                ProductStockStatusInfo(
                    productId = 1L,
                    stockStatus = ProductStockStatus.OutOfStock,
                    manageStock = false,
                    isVariable = false
                ),
                ProductStockStatusInfo(
                    productId = 2L,
                    stockStatus = ProductStockStatus.InStock,
                    manageStock = false,
                    isVariable = false
                )
            )
            mockFetchStockStatuses(stockStatusInfos)
            setupViewModel(stockStatusInfos.map { it.productId })

            // When
            var state: UpdateStockStatusUiState? = null
            viewModel.viewState.observeForever { state = it }

            // Then
            assertThat(state?.currentProductStockStatus).isEqualTo(ProductStockStatus.InStock)
        }

    private fun setupViewModel(selectedProductIds: List<Long>) {
        viewModel = UpdateProductStockStatusViewModel(
            savedStateHandle = UpdateProductStockStatusFragmentArgs(
                selectedProductIds = selectedProductIds.toLongArray()
            ).toSavedStateHandle(),
            productListRepository = productListRepository,
            analyticsTracker = analyticsTracker,
            resourceProvider = resourceProvider
        )
    }

    private suspend fun mockBulkUpdateStockStatus(
        selectedProductIds: List<Long>,
        stockStatus: ProductStockStatus,
        result: UpdateStockStatusResult
    ) {
        whenever(productListRepository.bulkUpdateStockStatus(selectedProductIds, stockStatus)).thenReturn(result)
    }

    private suspend fun mockFetchStockStatusesWithManageStock(
        stockStatusInfos: List<ProductStockStatusInfo>
    ) {
        val productIds = stockStatusInfos.map { it.productId }
        whenever(productListRepository.fetchStockStatuses(productIds)).thenReturn(stockStatusInfos)
    }

    private suspend fun mockFetchStockStatuses(
        selectedProductIds: List<Long>,
        stockStatus: ProductStockStatus,
        manageStock: Boolean,
        isVariable: Boolean = false
    ) {
        whenever(productListRepository.fetchStockStatuses(selectedProductIds)).thenReturn(
            selectedProductIds.map { id ->
                ProductStockStatusInfo(
                    productId = id,
                    stockStatus = stockStatus,
                    manageStock = manageStock,
                    isVariable = isVariable
                )
            }
        )
    }

    private suspend fun mockFetchStockStatuses(stockStatusInfos: List<ProductStockStatusInfo>) {
        val productIds = stockStatusInfos.map { it.productId }
        whenever(productListRepository.fetchStockStatuses(productIds)).thenReturn(stockStatusInfos)
    }
}
