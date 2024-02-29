package com.woocommerce.android.ui.products

import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.products.UpdateProductStockStatusViewModel.ProductStockStatusInfo
import com.woocommerce.android.ui.products.UpdateProductStockStatusViewModel.StockStatusState
import com.woocommerce.android.ui.products.UpdateProductStockStatusViewModel.UpdateStockStatusExitState
import com.woocommerce.android.ui.products.UpdateProductStockStatusViewModel.UpdateStockStatusResult
import com.woocommerce.android.ui.products.UpdateProductStockStatusViewModel.UpdateStockStatusUiState
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UpdateProductStockStatusViewModelTest : BaseUnitTest() {
    private val productListRepository: ProductListRepository = mock()
    private val analyticsTracker: AnalyticsTrackerWrapper = mock()

    private lateinit var viewModel: UpdateProductStockStatusViewModel

    @Test
    fun `given product ids,  when viewModel is initialized, then it loads product stock statuses`() = testBlocking {
        // Given
        val selectedProductIds = listOf(1L, 2L)
        mockFetchStockStatuses(selectedProductIds, ProductStockStatus.InStock, false)

        // When
        setupViewModel(selectedProductIds)

        var state: UpdateStockStatusUiState? = null
        viewModel.viewState.observeForever { state = it }

        // Then
        assertThat(state?.productsToUpdateCount).isEqualTo(selectedProductIds.size)
    }

    @Test
    fun `given mixed stock statuses, when viewModel is initialized, then ui state reflects mixed status`() =
        testBlocking {
            // Given
            val stockStatusInfos = listOf(
                ProductStockStatusInfo(productId = 1L, stockStatus = ProductStockStatus.InStock, manageStock = false),
                ProductStockStatusInfo(productId = 2L, stockStatus = ProductStockStatus.OutOfStock, manageStock = false)
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
            viewModel.setCurrentStockStatus(newStockStatus)

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
            viewModel.updateStockStatusForProducts()

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
        viewModel.updateStockStatusForProducts()

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
        viewModel.updateStockStatusForProducts()

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
            viewModel.updateStockStatusForProducts()

            // Then
            assertThat(event).isInstanceOf(MultiLiveEvent.Event.ExitWithResult::class.java)
            assertThat((event as MultiLiveEvent.Event.ExitWithResult<*>).data).isEqualTo(
                UpdateStockStatusExitState.Error
            )
        }

    @Test
    fun `when all products are eligible for update, correct products count is shown`() = testBlocking {
        // Given
        val stockStatusInfos = listOf(
            ProductStockStatusInfo(productId = 1L, stockStatus = ProductStockStatus.InStock, manageStock = false),
            ProductStockStatusInfo(productId = 2L, stockStatus = ProductStockStatus.OutOfStock, manageStock = false)
        )
        mockFetchStockStatusesWithManageStock(stockStatusInfos)
        setupViewModel(stockStatusInfos.map { it.productId })

        // When
        var state: UpdateStockStatusUiState? = null
        viewModel.viewState.observeForever { state = it }

        // Then
        assertThat(state?.productsToUpdateCount).isEqualTo(2)
        assertThat(state?.ignoredProductsCount).isEqualTo(0)
    }

    @Test
    fun `when some products have managed stock, correct products count and ignored count are shown`() = testBlocking {
        // Given
        val stockStatusInfos = listOf(
            ProductStockStatusInfo(productId = 1L, stockStatus = ProductStockStatus.InStock, manageStock = false),
            ProductStockStatusInfo(productId = 2L, stockStatus = ProductStockStatus.OutOfStock, manageStock = true)
        )
        mockFetchStockStatusesWithManageStock(stockStatusInfos)
        setupViewModel(stockStatusInfos.map { it.productId })

        // When
        var state: UpdateStockStatusUiState? = null
        viewModel.viewState.observeForever { state = it }

        // Then
        assertThat(state?.productsToUpdateCount).isEqualTo(1)
        assertThat(state?.ignoredProductsCount).isEqualTo(1)
    }

    private fun setupViewModel(selectedProductIds: List<Long>) {
        viewModel = UpdateProductStockStatusViewModel(
            savedStateHandle = UpdateProductStockStatusFragmentArgs(
                selectedProductIds = selectedProductIds.toLongArray()
            ).toSavedStateHandle(),
            productListRepository = productListRepository,
            analyticsTracker = analyticsTracker
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
        manageStock: Boolean
    ) {
        whenever(productListRepository.fetchStockStatuses(selectedProductIds)).thenReturn(
            selectedProductIds.map { id ->
                ProductStockStatusInfo(
                    productId = id,
                    stockStatus = stockStatus,
                    manageStock = manageStock
                )
            }
        )
    }

    private suspend fun mockFetchStockStatuses(stockStatusInfos: List<ProductStockStatusInfo>) {
        val productIds = stockStatusInfos.map { it.productId }
        whenever(productListRepository.fetchStockStatuses(productIds)).thenReturn(stockStatusInfos)
    }
}
