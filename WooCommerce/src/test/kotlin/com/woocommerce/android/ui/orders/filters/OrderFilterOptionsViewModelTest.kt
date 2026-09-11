package com.woocommerce.android.ui.orders.filters

import com.woocommerce.android.ui.orders.filters.data.OrderFiltersRepository
import com.woocommerce.android.ui.orders.filters.data.OrderListFilterCategory
import com.woocommerce.android.ui.orders.filters.domain.GetTrackingForFilterSelection
import com.woocommerce.android.ui.orders.filters.domain.SaveOrderFilterToHistory
import com.woocommerce.android.ui.orders.filters.model.OrderFilterCategoryUiModel
import com.woocommerce.android.ui.orders.filters.model.OrderFilterEvent.OnShowOrders
import com.woocommerce.android.ui.orders.filters.model.OrderFilterOptionUiModel
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
class OrderFilterOptionsViewModelTest : BaseUnitTest() {
    private val resourceProvider: ResourceProvider = mock {
        on { getString(any()) } doReturn "AnyString"
    }
    private val orderFilterRepository: OrderFiltersRepository = mock()
    private val getTrackingForFilterSelection: GetTrackingForFilterSelection = mock()
    private val saveOrderFilterToHistory: SaveOrderFilterToHistory = mock()
    private val dateUtils: DateUtils = mock()

    private fun createViewModel() = OrderFilterOptionsViewModel(
        savedState = OrderFilterOptionsFragmentArgs(filterCategory = A_CATEGORY).toSavedStateHandle(),
        resourceProvider = resourceProvider,
        orderFilterRepository = orderFilterRepository,
        getTrackingForFilterSelection = getTrackingForFilterSelection,
        saveOrderFilterToHistory = saveOrderFilterToHistory,
        dateUtils = dateUtils
    )

    @Test
    fun `when show orders is clicked, then the current filter is saved to history`() = testBlocking {
        val viewModel = createViewModel()

        viewModel.onShowOrdersClicked()

        verify(saveOrderFilterToHistory).invoke()
    }

    @Test
    fun `when show orders is clicked, then OnShowOrders event is triggered`() = testBlocking {
        val viewModel = createViewModel()

        viewModel.onShowOrdersClicked()

        assertThat(viewModel.event.value).isEqualTo(OnShowOrders)
    }

    @Test
    fun `when show orders is clicked, then the selection is persisted`() = testBlocking {
        val viewModel = createViewModel()

        viewModel.onShowOrdersClicked()

        verify(orderFilterRepository).setSelectedFilters(OrderListFilterCategory.ORDER_STATUS, listOf("processing"))
    }

    private companion object {
        val A_CATEGORY = OrderFilterCategoryUiModel(
            categoryKey = OrderListFilterCategory.ORDER_STATUS,
            displayName = "",
            displayValue = "",
            orderFilterOptions = listOf(
                OrderFilterOptionUiModel(key = "processing", displayName = "Processing", isSelected = true)
            )
        )
    }
}
