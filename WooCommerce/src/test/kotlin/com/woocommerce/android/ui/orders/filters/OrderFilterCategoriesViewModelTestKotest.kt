package com.woocommerce.android.ui.orders.filters

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.CoroutinesTestExtension
import com.woocommerce.android.InstantExecutorExtension
import com.woocommerce.android.R
import com.woocommerce.android.ui.orders.OrderTestUtils
import com.woocommerce.android.ui.orders.filters.data.*
import com.woocommerce.android.ui.orders.filters.domain.GetDateRangeFilterOptions
import com.woocommerce.android.ui.orders.filters.domain.GetOrderStatusFilterOptions
import com.woocommerce.android.ui.orders.filters.domain.GetTrackingForFilterSelection
import com.woocommerce.android.ui.orders.filters.model.OrderFilterCategoryListViewState
import com.woocommerce.android.ui.orders.filters.model.OrderFilterCategoryUiModel
import com.woocommerce.android.ui.orders.filters.model.OrderFilterEvent
import com.woocommerce.android.ui.orders.filters.model.OrderFilterEvent.OnShowOrders
import com.woocommerce.android.ui.orders.filters.model.OrderFilterOptionUiModel
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.util.getOrAwaitValue
import com.woocommerce.android.viewmodel.ResourceProvider
import io.kotest.common.runBlocking
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class OrderFilterCategoriesViewModelTestKotest : BehaviorSpec() {
    init {
        val testDispatcher = TestCoroutineDispatcher()
        extension(CoroutinesTestExtension(testDispatcher))
        extension(InstantExecutorExtension())

        val savedStateHandle = SavedStateHandle()
        val resourceProvider: ResourceProvider = mock()
        val getOrderStatusFilterOptions: GetOrderStatusFilterOptions = mock()
        val getDateRangeFilterOptions: GetDateRangeFilterOptions = mock()
        val orderFilterRepository: OrderFiltersRepository = mock()
        val getTrackingForFilterSelection: GetTrackingForFilterSelection = mock()
        val dateUtils: DateUtils = mock()

        lateinit var viewModel: OrderFilterCategoriesViewModel

        givenResourceProviderReturnsNonEmptyStrings(resourceProvider)

        given("Order filters are available") {
            runBlocking {
                givenDateRangeFiltersAvailable(getDateRangeFilterOptions)
                givenOrderStatusOptionsAvailable(getOrderStatusFilterOptions)
            }
            viewModel = OrderFilterCategoriesViewModel(
                savedStateHandle,
                resourceProvider,
                getOrderStatusFilterOptions,
                getDateRangeFilterOptions,
                orderFilterRepository,
                getTrackingForFilterSelection,
                dateUtils
            )
            and("No filters selected") {
                `when`("On show orders clicked") {
                    viewModel.onShowOrdersClicked()

                    then("An empty list is saved") {
                        verify(orderFilterRepository).setSelectedFilters(
                            AN_ORDER_STATUS_FILTER_CATEGORY_WITH_SELECTED_FILTER.categoryKey,
                            emptyList()
                        )
                    }
                }
            }
            `when`("A filter category is selected") {
                viewModel.onFilterCategorySelected(AN_ORDER_STATUS_FILTER_CATEGORY_WITH_SELECTED_FILTER)

                then("Screen title is updated") {
                    viewModel.event.value shouldBe
                        OrderFilterEvent.ShowFilterOptionsForCategory(
                            AN_ORDER_STATUS_FILTER_CATEGORY_WITH_SELECTED_FILTER
                        )
                }
            }
            and("A filter option is selected") {
                viewModel.onFilterOptionsUpdated(AN_ORDER_STATUS_FILTER_CATEGORY_WITH_SELECTED_FILTER)

                `when`("On show orders clicked") {
                    viewModel.onShowOrdersClicked()

                    then("Selected filters are saved") {
                        verify(orderFilterRepository).setSelectedFilters(
                            AN_ORDER_STATUS_FILTER_CATEGORY_WITH_SELECTED_FILTER.categoryKey,
                            listOf(SELECTED_ORDER_STATUS_FILTER_OPTION.key)
                        )
                    }
                    then("Trigger OnShowOrders event") {
                        viewModel.event.value shouldBe OnShowOrders
                    }
                }
                `when`("On clear button clicked") {
                    viewModel.onClearFilters()

                    then("All filter options should be unselected") {
                        allFilterOptionsAreUnselected(viewModel).shouldBeTrue()
                    }
                    then("Clear button should be hidden and toolbar title updated to default") {
                        viewModel.orderFilterCategoryViewState.getOrAwaitValue() shouldBe
                            OrderFilterCategoryListViewState(
                                screenTitle = DEFAULT_FILTER_TITLE,
                                displayClearButton = false
                            )
                    }
                }
            }
        }
    }

    private suspend fun givenOrderStatusOptionsAvailable(getOrderStatusFilterOptions: GetOrderStatusFilterOptions) {
        whenever(getOrderStatusFilterOptions.invoke()).thenReturn(
            OrderTestUtils.generateOrderStatusOptions()
                .map {
                    OrderStatusOption(
                        key = it.statusKey,
                        label = it.label,
                        statusCount = it.statusCount,
                        isSelected = false
                    )
                }
        )
    }

    private fun givenDateRangeFiltersAvailable(getDateRangeFilterOptions: GetDateRangeFilterOptions) {
        whenever(getDateRangeFilterOptions.invoke()).thenReturn(
            listOf(DateRange.TODAY, DateRange.LAST_2_DAYS, DateRange.LAST_7_DAYS, DateRange.LAST_30_DAYS)
                .map {
                    DateRangeFilterOption(
                        dateRange = it,
                        isSelected = false,
                        startDate = 0,
                        endDate = 0
                    )
                }
        )
    }

    private fun givenResourceProviderReturnsNonEmptyStrings(resourceProvider: ResourceProvider) {
        whenever(resourceProvider.getString(any())).thenReturn("AnyString")
        whenever(resourceProvider.getString(any(), any(), any())).thenReturn("AnyString")
        whenever(resourceProvider.getString(R.string.orderfilters_filters_default_title)).thenReturn(
            DEFAULT_FILTER_TITLE
        )
    }

    private fun allFilterOptionsAreUnselected(viewModel: OrderFilterCategoriesViewModel) =
        viewModel.categories.liveData.value!!.list
            .map {
                it.orderFilterOptions.any { filterOption ->
                    filterOption.isSelected && filterOption.key != OrderFilterOptionUiModel.DEFAULT_ALL_KEY
                }
            }.all { true }

    private companion object {
        const val DEFAULT_FILTER_TITLE = "Title"
        const val ANY_ORDER_STATUS_KEY = "OrderStatusOptionKey"
        val SELECTED_ORDER_STATUS_FILTER_OPTION = OrderFilterOptionUiModel(
            key = ANY_ORDER_STATUS_KEY,
            displayName = "OrderStatus",
            isSelected = true
        )
        val A_LIST_OF_ORDER_STATUS_FILTER_OPTIONS = listOf(SELECTED_ORDER_STATUS_FILTER_OPTION)
        val AN_ORDER_STATUS_FILTER_CATEGORY_WITH_SELECTED_FILTER = OrderFilterCategoryUiModel(
            categoryKey = OrderListFilterCategory.ORDER_STATUS,
            displayName = "",
            displayValue = "",
            orderFilterOptions = A_LIST_OF_ORDER_STATUS_FILTER_OPTIONS
        )
    }
}
