package com.woocommerce.android.ui.orders.filters

import com.woocommerce.android.ui.orders.filters.data.DateRange
import com.woocommerce.android.ui.orders.filters.data.OrderFiltersRepository
import com.woocommerce.android.ui.orders.filters.data.OrderListFilterCategory
import com.woocommerce.android.ui.orders.filters.domain.GetTrackingForFilterSelection
import com.woocommerce.android.ui.orders.filters.domain.SaveOrderFilterToHistory
import com.woocommerce.android.ui.orders.filters.model.OrderFilterCategoryUiModel
import com.woocommerce.android.ui.orders.filters.model.OrderFilterEvent.OnShowOrders
import com.woocommerce.android.ui.orders.filters.model.OrderFilterEvent.ShowCustomDateRangePicker
import com.woocommerce.android.ui.orders.filters.model.OrderFilterOptionUiModel
import com.woocommerce.android.ui.orders.filters.model.OrderFilterOptionUiModel.Companion.DEFAULT_ALL_KEY
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
    private val orderFilterRepository: OrderFiltersRepository = mock {
        on { getCustomDateRangeDays() } doReturn Pair(0L, 0L)
    }
    private val getTrackingForFilterSelection: GetTrackingForFilterSelection = mock()
    private val saveOrderFilterToHistory: SaveOrderFilterToHistory = mock()
    private val dateUtils: DateUtils = mock {
        on { toDisplayMediumDate(any()) } doReturn "A date"
    }

    private fun createViewModel(category: OrderFilterCategoryUiModel = A_CATEGORY) = OrderFilterOptionsViewModel(
        savedState = OrderFilterOptionsFragmentArgs(filterCategory = category).toSavedStateHandle(),
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

    @Test
    fun `when custom range is clicked, then the date picker is shown and the selection is unchanged`() = testBlocking {
        val viewModel = createViewModel(DATE_RANGE_CATEGORY)

        viewModel.onFilterOptionSelected(CUSTOM_RANGE_OPTION)

        assertThat(viewModel.event.value).isEqualTo(ShowCustomDateRangePicker(0L, 0L))
        assertThat(viewModel.selectedOptionKeys()).containsExactly(DEFAULT_ALL_KEY)
    }

    @Test
    fun `given the date picker was closed without saving, when show orders is clicked, then no date range is saved`() =
        testBlocking {
            val viewModel = createViewModel(DATE_RANGE_CATEGORY)
            viewModel.onFilterOptionSelected(CUSTOM_RANGE_OPTION)

            viewModel.onShowOrdersClicked()

            verify(orderFilterRepository).setSelectedFilters(OrderListFilterCategory.DATE_RANGE, emptyList())
        }

    @Test
    fun `when a custom range is saved, then custom range is selected with the range`() = testBlocking {
        val viewModel = createViewModel(DATE_RANGE_CATEGORY)
        viewModel.onFilterOptionSelected(CUSTOM_RANGE_OPTION)

        viewModel.onCustomDateRangeChanged(startDay = 20698L, endDay = 20713L)

        val customRange = viewModel.viewState.liveData.value?.filterOptions
            ?.single { it.key == DateRange.CUSTOM_RANGE.filterKey }
        assertThat(viewModel.selectedOptionKeys()).containsExactly(DateRange.CUSTOM_RANGE.filterKey)
        assertThat(customRange?.displayValue).isEqualTo("A date - A date")
        verify(orderFilterRepository).setCustomDateRange(20698L, 20713L)
    }

    private fun OrderFilterOptionsViewModel.selectedOptionKeys() =
        viewState.liveData.value?.filterOptions?.filter { it.isSelected }?.map { it.key }

    private companion object {
        val A_CATEGORY = OrderFilterCategoryUiModel(
            categoryKey = OrderListFilterCategory.ORDER_STATUS,
            displayName = "",
            displayValue = "",
            orderFilterOptions = listOf(
                OrderFilterOptionUiModel(key = "processing", displayName = "Processing", isSelected = true)
            )
        )
        val CUSTOM_RANGE_OPTION = OrderFilterOptionUiModel(
            key = DateRange.CUSTOM_RANGE.filterKey,
            displayName = "Custom Range"
        )
        val DATE_RANGE_CATEGORY = OrderFilterCategoryUiModel(
            categoryKey = OrderListFilterCategory.DATE_RANGE,
            displayName = "",
            displayValue = "",
            orderFilterOptions = listOf(
                OrderFilterOptionUiModel(key = DEFAULT_ALL_KEY, displayName = "All", isSelected = true),
                OrderFilterOptionUiModel(key = DateRange.TODAY.filterKey, displayName = "Today"),
                CUSTOM_RANGE_OPTION
            )
        )
    }
}
