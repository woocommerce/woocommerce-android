package com.woocommerce.android.ui.orders.filters

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.ui.orders.OrderTestUtils.generateOrderStatusOptions
import com.woocommerce.android.ui.orders.filters.data.DateRange
import com.woocommerce.android.ui.orders.filters.data.DateRangeFilterOption
import com.woocommerce.android.ui.orders.filters.data.GetDateRangeFilterOptions
import com.woocommerce.android.ui.orders.filters.data.GetOrderStatusFilterOptions
import com.woocommerce.android.ui.orders.filters.data.OrderFiltersRepository
import com.woocommerce.android.ui.orders.filters.data.OrderListFilterCategory
import com.woocommerce.android.ui.orders.filters.data.OrderStatusOption
import com.woocommerce.android.ui.orders.filters.model.OrderFilterCategoryListViewState
import com.woocommerce.android.ui.orders.filters.model.OrderFilterCategoryUiModel
import com.woocommerce.android.ui.orders.filters.model.OrderFilterEvent.OnShowOrders
import com.woocommerce.android.ui.orders.filters.model.OrderFilterEvent.ShowFilterOptionsForCategory
import com.woocommerce.android.ui.orders.filters.model.OrderFilterOptionUiModel
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class OrderFilterCategoriesViewModelTest : BaseUnitTest() {
    private val savedStateHandle: SavedStateHandle = SavedStateHandle()
    private val resourceProvider: ResourceProvider = mock()
    private val getOrderStatusFilterOptions: GetOrderStatusFilterOptions = mock()
    private val getDateRangeFilterOptions: GetDateRangeFilterOptions = mock()
    private val orderFilterRepository: OrderFiltersRepository = mock()

    private lateinit var viewModel: OrderFilterCategoriesViewModel

    @Before
    fun setup() = testBlocking {
        givenResourceProviderReturnsNonEmptyStrings()
        givenOrderStatusOptionsAvailable()
        givenDateRangeFiltersAvailable()
        initViewModel()
    }

    @Test
    fun `When filter category is selected, then update screen title`() {
        viewModel.onFilterCategorySelected(AN_ORDER_STATUS_FILTER_CATEGORY_WITH_SELECTED_FILTER)

        assertThat(viewModel.event.value).isEqualTo(
            ShowFilterOptionsForCategory(
                AN_ORDER_STATUS_FILTER_CATEGORY_WITH_SELECTED_FILTER
            )
        )
    }

    @Test
    fun `When show orders is clicked, then selected filters are saved`() {
        givenAFilterOptionHasBeenSelected(AN_ORDER_STATUS_FILTER_CATEGORY_WITH_SELECTED_FILTER)

        viewModel.onShowOrdersClicked()

        verify(orderFilterRepository).updateSelectedFilters(
            AN_ORDER_STATUS_FILTER_CATEGORY_WITH_SELECTED_FILTER.categoryKey,
            listOf(SELECTED_ORDER_STATUS_FILTER_OPTION.key)
        )
    }

    @Test
    fun `When show orders is clicked, then trigger OnShowOrders event`() {
        givenAFilterOptionHasBeenSelected(AN_ORDER_STATUS_FILTER_CATEGORY_WITH_SELECTED_FILTER)

        viewModel.onShowOrdersClicked()

        assertThat(viewModel.event.value).isEqualTo(OnShowOrders)
    }

    @Test
    fun `Given no filters are selected, when show orders is clicked, then an empty list is saved`() {
        viewModel.onShowOrdersClicked()

        verify(orderFilterRepository).updateSelectedFilters(
            AN_ORDER_STATUS_FILTER_CATEGORY_WITH_SELECTED_FILTER.categoryKey,
            emptyList()
        )
    }


    @Test
    fun `Given some selected filters, when onClearFilters, then all filter options should be unselected`() {
        givenAFilterOptionHasBeenSelected(AN_ORDER_STATUS_FILTER_CATEGORY_WITH_SELECTED_FILTER)

        viewModel.onClearFilters()

        assertTrue(allFilterOptionsAreUnselected())
    }

    @Test
    fun `When clear button clicked, then clear button should be hidden and toolbar title updated`() {
        whenever(resourceProvider.getString(R.string.orderfilters_filters_default_title))
            .thenReturn(DEFAULT_FILTER_TITLE)

        viewModel.onClearFilters()

        assertThat(viewModel.orderFilterCategoryViewState.value).isEqualTo(
            OrderFilterCategoryListViewState(
                screenTitle = DEFAULT_FILTER_TITLE,
                displayClearButton = false
            )
        )
    }

    private fun allFilterOptionsAreUnselected() = viewModel.orderFilterCategories.value
        ?.map {
            it.orderFilterOptions.any { filterOption ->
                filterOption.isSelected && filterOption.key != OrderFilterOptionUiModel.DEFAULT_ALL_KEY
            }
        }?.all { true } ?: false

    private fun givenAFilterOptionHasBeenSelected(updatedFilters: OrderFilterCategoryUiModel) {
        viewModel.onFilterOptionsUpdated(updatedFilters)
    }

    private fun initViewModel() {
        viewModel = OrderFilterCategoriesViewModel(
            savedStateHandle,
            resourceProvider,
            getOrderStatusFilterOptions,
            getDateRangeFilterOptions,
            orderFilterRepository
        )
    }

    private suspend fun givenOrderStatusOptionsAvailable() {
        whenever(getOrderStatusFilterOptions.invoke()).thenReturn(
            generateOrderStatusOptions()
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

    private fun givenDateRangeFiltersAvailable() {
        whenever(getDateRangeFilterOptions.invoke()).thenReturn(
            listOf(DateRange.TODAY, DateRange.LAST_2_DAYS, DateRange.THIS_WEEK, DateRange.THIS_MONTH)
                .map {
                    DateRangeFilterOption(
                        dateRange = it,
                        isSelected = false
                    )
                }
        )
    }

    private fun givenResourceProviderReturnsNonEmptyStrings() {
        whenever(resourceProvider.getString(any())).thenReturn("AnyString")
        whenever(resourceProvider.getString(any(), any())).thenReturn("AnyString")
        whenever(resourceProvider.getString(any(), any(), any())).thenReturn("AnyString")
    }

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
