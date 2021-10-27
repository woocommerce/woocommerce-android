package com.woocommerce.android.ui.orders.filters

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.ui.orders.OrderTestUtils.generateOrderStatusOptions
import com.woocommerce.android.ui.orders.filters.data.OrderFiltersRepository
import com.woocommerce.android.ui.orders.filters.data.OrderFiltersRepository.OrderListFilterCategory.ORDER_STATUS
import com.woocommerce.android.ui.orders.filters.ui.OrderFilterListViewModel
import com.woocommerce.android.ui.orders.filters.ui.model.FilterListCategoryUiModel
import com.woocommerce.android.ui.orders.filters.ui.model.OrderFilterCategoryListViewState
import com.woocommerce.android.ui.orders.filters.ui.model.OrderFilterListEvent.ShowOrderStatusFilterOptions
import com.woocommerce.android.ui.orders.filters.ui.model.OrderListFilterOptionUiModel
import com.woocommerce.android.ui.orders.list.OrderListRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
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
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class OrderFilterListViewModelTest : BaseUnitTest() {
    private val resourceProvider: ResourceProvider = mock()
    private val savedStateHandle: SavedStateHandle = SavedStateHandle()
    private val orderListRepository: OrderListRepository = mock()
    private val orderFilterRepository: OrderFiltersRepository = mock()

    private lateinit var viewModel: OrderFilterListViewModel

    @Before
    fun setup() = testBlocking {
        givenResourceProviderReturnsNonEmptyStrings()
        givenOrderStatusOptionsAvailable()
        initViewModel()
    }

    @Test
    fun `When filter category is selected, then update screen title`() {
        viewModel.onFilterCategorySelected(AN_ORDER_STATUS_FILTER_CATEGORY)

        assertThat(viewModel.orderFilterOptionScreenTitle.value).isEqualTo(ORDER_STATUS_FILTERS_TITLE)
    }

    @Test
    fun `When filter category is selected, then update order filter options`() {
        viewModel.onFilterCategorySelected(AN_ORDER_STATUS_FILTER_CATEGORY)

        assertThat(viewModel.event.value).isEqualTo(ShowOrderStatusFilterOptions)
        assertThat(viewModel.orderOptionsFilter.value).isEqualTo(A_LIST_OF_ORDER_STATUS_FILTER_OPTIONS)
    }

    @Test
    fun `When show orders is clicked, then selected filters are updated and exit with result`() {
        givenFilterCategoryHasBeenSelected(AN_ORDER_STATUS_FILTER_CATEGORY)
        givenAFilterOptionHasBeenSelected(AN_ORDER_STATUS_FILTER_OPTION)

        viewModel.onShowOrdersClicked()

        assertThat(viewModel.event.value).isEqualTo(ExitWithResult(true))
        verify(orderFilterRepository).updateSelectedFilters(
            mapOf(AN_ORDER_STATUS_FILTER_CATEGORY.categoryKey to listOf(AN_ORDER_STATUS_FILTER_OPTION.key))
        )
    }

    @Test
    fun `Given no filters are selected, when show orders is clicked, then an empty map is saved`() {
        viewModel.onShowOrdersClicked()

        verify(orderFilterRepository).updateSelectedFilters(emptyMap())
    }

    @Test
    fun `Given no filters are selected, when show orders is clicked, then exit with result`() {
        viewModel.onShowOrdersClicked()

        assertThat(viewModel.event.value).isEqualTo(ExitWithResult(false))
    }

    @Test
    fun `Given some selected filters, when onClearFilters, then all filter options should be unselected`() {
        whenever(resourceProvider.getString(R.string.orderfilters_filters_default_title))
            .thenReturn(DEFAULT_FILTER_TITLE)
        givenAFilterOptionHasBeenSelected(AN_ORDER_STATUS_FILTER_OPTION)

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
                filterOption.isSelected && filterOption.key != OrderListFilterOptionUiModel.DEFAULT_ALL_KEY
            }
        }?.all { true } ?: false

    private fun givenFilterCategoryHasBeenSelected(anOrderStatusFilterCategory: FilterListCategoryUiModel) {
        viewModel.onFilterCategorySelected(anOrderStatusFilterCategory)
    }

    private fun givenAFilterOptionHasBeenSelected(selectedFilterOption: OrderListFilterOptionUiModel) {
        viewModel.onFilterOptionSelected(selectedFilterOption)
    }

    private fun initViewModel() {
        viewModel = OrderFilterListViewModel(
            savedStateHandle,
            resourceProvider,
            orderListRepository,
            orderFilterRepository
        )
    }

    private suspend fun givenOrderStatusOptionsAvailable() {
        whenever(orderListRepository.getCachedOrderStatusOptions()).thenReturn(
            generateOrderStatusOptions()
                .map { it.statusKey to it }
                .toMap()
        )
    }

    private fun givenResourceProviderReturnsNonEmptyStrings() {
        whenever(resourceProvider.getString(any())).thenReturn("AnyString")
        whenever(resourceProvider.getString(any(), any())).thenReturn("AnyString")
        whenever(resourceProvider.getString(any(), any(), any())).thenReturn("AnyString")
        whenever(resourceProvider.getString(R.string.orderfilters_filter_order_status_options_title))
            .thenReturn(ORDER_STATUS_FILTERS_TITLE)
    }

    private companion object {
        const val DEFAULT_FILTER_TITLE = "Title"
        const val ORDER_STATUS_FILTERS_TITLE = "Order status"
        val A_LIST_OF_ORDER_STATUS_SELECTED_FILTERS = listOf(
            CoreOrderStatus.PENDING.value,
            CoreOrderStatus.PENDING.value,
            CoreOrderStatus.PENDING.value,
        )
        const val ANY_ORDER_STATUS_KEY = "OrderStatusOptionKey"
        val AN_ORDER_STATUS_FILTER_OPTION = OrderListFilterOptionUiModel(
            key = ANY_ORDER_STATUS_KEY,
            displayName = "OrderStatus",
            isSelected = false
        )
        val A_LIST_OF_ORDER_STATUS_FILTER_OPTIONS = listOf(AN_ORDER_STATUS_FILTER_OPTION)
        val AN_ORDER_STATUS_FILTER_CATEGORY = FilterListCategoryUiModel(
            categoryKey = ORDER_STATUS,
            displayName = "",
            displayValue = "",
            orderFilterOptions = A_LIST_OF_ORDER_STATUS_FILTER_OPTIONS
        )
    }
}
