package com.woocommerce.android.ui.products

import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.products.UpdateProductStockStatusViewModel.ProductStockStatusInfo
import com.woocommerce.android.ui.products.UpdateProductStockStatusViewModel.StockStatusState
import com.woocommerce.android.ui.products.UpdateProductStockStatusViewModel.UpdateStockStatusUiState
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UpdateProductStockStatusViewModelTest : BaseUnitTest() {
    private val productListRepository: ProductListRepository = mock()
    private val analyticsTracker: AnalyticsTrackerWrapper = mock()

    private lateinit var viewModel: UpdateProductStockStatusViewModel

    private fun setupViewModel(selectedProductIds: List<Long>) {
        viewModel = UpdateProductStockStatusViewModel(
            savedStateHandle = UpdateProductStockStatusFragmentArgs(
                selectedProductIds = selectedProductIds.toLongArray()
            ).toSavedStateHandle(),
            productListRepository = productListRepository,
            analyticsTracker = analyticsTracker
        )
    }

    @Test
    fun `given product ids,  when viewModel is initialized, then it loads product stock statuses`() = testBlocking {
        // Given
        val selectedProductIds = listOf(1L, 2L)
        whenever(productListRepository.fetchStockStatuses(selectedProductIds)).thenReturn(
            selectedProductIds.map { id ->
                ProductStockStatusInfo(
                    productId = id,
                    stockStatus = ProductStockStatus.InStock,
                    manageStock = false
                )
            }
        )

        // When
        setupViewModel(selectedProductIds)

        var state: UpdateStockStatusUiState? = null
        viewModel.viewState.observeForever { state = it }

        // Then
        assertThat(state?.productsToUpdateCount).isEqualTo(selectedProductIds.size)
    }

    @Test
    fun `given mixed stock statuses, When viewModel is initialized, Then ui state reflects mixed status`() =
        testBlocking {
            // Given
            val selectedProductIds = listOf(1L, 2L)
            whenever(productListRepository.fetchStockStatuses(selectedProductIds)).thenReturn(
                listOf(
                    ProductStockStatusInfo(
                        productId = 1L,
                        stockStatus = ProductStockStatus.InStock,
                        manageStock = false
                    ),
                    ProductStockStatusInfo(
                        productId = 2L,
                        stockStatus = ProductStockStatus.OutOfStock,
                        manageStock = false
                    )
                )
            )
            setupViewModel(selectedProductIds)

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
            whenever(productListRepository.fetchStockStatuses(selectedProductIds)).thenReturn(
                selectedProductIds.map { id ->
                    ProductStockStatusInfo(
                        productId = id,
                        stockStatus = commonStockStatus,
                        manageStock = false
                    )
                }
            )

            // When
            setupViewModel(selectedProductIds)

            var state: UpdateStockStatusUiState? = null
            viewModel.viewState.observeForever { state = it }

            // Then
            assertThat(state?.currentStockStatusState).isEqualTo(StockStatusState.Common(commonStockStatus))
        }
}
