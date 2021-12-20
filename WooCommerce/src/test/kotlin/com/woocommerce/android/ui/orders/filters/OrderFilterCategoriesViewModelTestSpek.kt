package com.woocommerce.android.ui.orders.filters

import androidx.arch.core.executor.ArchTaskExecutor
import androidx.arch.core.executor.TaskExecutor
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.ui.orders.OrderTestUtils
import com.woocommerce.android.ui.orders.filters.data.*
import com.woocommerce.android.ui.orders.filters.domain.GetDateRangeFilterOptions
import com.woocommerce.android.ui.orders.filters.domain.GetOrderStatusFilterOptions
import com.woocommerce.android.ui.orders.filters.domain.GetTrackingForFilterSelection
import com.woocommerce.android.ui.orders.filters.model.OrderFilterCategoryListViewState
import com.woocommerce.android.ui.orders.filters.model.OrderFilterCategoryUiModel
import com.woocommerce.android.ui.orders.filters.model.OrderFilterEvent
import com.woocommerce.android.ui.orders.filters.model.OrderFilterOptionUiModel
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.util.getOrAwaitValue
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.setMain
import org.assertj.core.api.Assertions.assertThat
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.gherkin.Feature
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class OrderFilterCategoriesViewModelTestSpek : Spek({
    Feature("Order filters tests") {
        val savedStateHandle = SavedStateHandle()
        val resourceProvider: ResourceProvider = mock()
        val getOrderStatusFilterOptions: GetOrderStatusFilterOptions = mock()
        val getDateRangeFilterOptions: GetDateRangeFilterOptions = mock()
        val orderFilterRepository: OrderFiltersRepository = mock()
        val getTrackingForFilterSelection: GetTrackingForFilterSelection = mock()
        val dateUtils: DateUtils = mock()

        lateinit var viewModel: OrderFilterCategoriesViewModel

        beforeFeature {
            Dispatchers.setMain(TestCoroutineDispatcher())
            ArchTaskExecutor.getInstance().setDelegate(object : TaskExecutor() {
                override fun executeOnDiskIO(runnable: Runnable) {
                    runnable.run()
                }

                override fun isMainThread(): Boolean {
                    return true
                }

                override fun postToMainThread(runnable: Runnable) {
                    runnable.run()
                }
            })
        }

        beforeEachScenario {
            whenever(getDateRangeFilterOptions.invoke()).thenReturn(DATE_RANGE_OPTIONS)
            runBlockingTest {
                whenever(getOrderStatusFilterOptions.invoke()).thenReturn(ORDER_STATUS_OPTIONS)
            }
            whenever(resourceProvider.getString(any(), any(), any())).thenReturn("AnyString")
            whenever(resourceProvider.getString(any())).thenReturn("AnyString")
            viewModel = OrderFilterCategoriesViewModel(
                savedStateHandle,
                resourceProvider,
                getOrderStatusFilterOptions,
                getDateRangeFilterOptions,
                orderFilterRepository,
                getTrackingForFilterSelection,
                dateUtils
            )
        }

//        beforeEachTest {  } Not needed for this test suite

        afterFeature { ArchTaskExecutor.getInstance().setDelegate(null) }

        Scenario("Selecting filter category") {
            When("Category is selected") {
                viewModel.onFilterCategorySelected(AN_ORDER_STATUS_FILTER_CATEGORY_WITH_SELECTED_FILTER)
            }

            Then("Screen title should be updated") {
                assertThat(viewModel.event.value).isEqualTo(
                    OrderFilterEvent.ShowFilterOptionsForCategory(
                        AN_ORDER_STATUS_FILTER_CATEGORY_WITH_SELECTED_FILTER
                    )
                )
            }
        }

        Scenario("Clicking on show orders button") {
            Given("A filter option is selected") {
                viewModel.onFilterOptionsUpdated(AN_ORDER_STATUS_FILTER_CATEGORY_WITH_SELECTED_FILTER)
            }
            When("Show order button clicked") {
                viewModel.onShowOrdersClicked()
            }

            Then("Selected filters are saved") {
                verify(orderFilterRepository).setSelectedFilters(
                    AN_ORDER_STATUS_FILTER_CATEGORY_WITH_SELECTED_FILTER.categoryKey,
                    listOf(SELECTED_ORDER_STATUS_FILTER_OPTION.key)
                )
            }
            Then("Trigger OnShowOrders event") {
                assertThat(viewModel.event.value).isEqualTo(OrderFilterEvent.OnShowOrders)
            }
        }

        Scenario("Clicking on clear button") {
            beforeEachGroup {
                //We can add specific general setup for the group
                whenever(resourceProvider.getString(R.string.orderfilters_filters_default_title))
                    .thenReturn(DEFAULT_FILTER_TITLE)
            }
            Given("A filter option is selected") {
                viewModel.onFilterOptionsUpdated(AN_ORDER_STATUS_FILTER_CATEGORY_WITH_SELECTED_FILTER)
            }
            When("Clear button is clicked") {
                viewModel.onClearFilters()
            }

            Then("All filter options should be unselected") {
                assertTrue(allFilterOptionsAreUnselected(viewModel))
            }
            Then("Clear button should be hidden and toolbar title updated to default") {
                assertThat(viewModel.orderFilterCategoryViewState.getOrAwaitValue()).isEqualTo(
                    OrderFilterCategoryListViewState(
                        screenTitle = DEFAULT_FILTER_TITLE,
                        displayClearButton = false
                    )
                )
            }
        }
    }
})

private fun allFilterOptionsAreUnselected(viewModel: OrderFilterCategoriesViewModel) =
    viewModel.categories.liveData.value!!.list
        .map {
            it.orderFilterOptions.any { filterOption ->
                filterOption.isSelected && filterOption.key != OrderFilterOptionUiModel.DEFAULT_ALL_KEY
            }
        }.all { true }

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
val ORDER_STATUS_OPTIONS = OrderTestUtils.generateOrderStatusOptions()
    .map {
        OrderStatusOption(
            key = it.statusKey,
            label = it.label,
            statusCount = it.statusCount,
            isSelected = false
        )
    }
val DATE_RANGE_OPTIONS =
    listOf(DateRange.TODAY, DateRange.LAST_2_DAYS, DateRange.LAST_7_DAYS, DateRange.LAST_30_DAYS)
        .map {
            DateRangeFilterOption(
                dateRange = it,
                isSelected = false,
                startDate = 0,
                endDate = 0
            )
        }
