package com.woocommerce.android.ui.orders.filters

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.filters.SavedFilter
import com.woocommerce.android.ui.orders.OrderTestUtils.generateOrderStatusOptions
import com.woocommerce.android.ui.orders.filters.data.DateRange
import com.woocommerce.android.ui.orders.filters.data.DateRangeFilterOption
import com.woocommerce.android.ui.orders.filters.data.OrderFiltersRepository
import com.woocommerce.android.ui.orders.filters.data.OrderListFilterCategory
import com.woocommerce.android.ui.orders.filters.data.OrderStatusOption
import com.woocommerce.android.ui.orders.filters.data.SalesChannel
import com.woocommerce.android.ui.orders.filters.domain.GetDateRangeFilterOptions
import com.woocommerce.android.ui.orders.filters.domain.GetOrderStatusFilterOptions
import com.woocommerce.android.ui.orders.filters.domain.GetTrackingForFilterSelection
import com.woocommerce.android.ui.orders.filters.domain.IsSalesChannelFilterSupported
import com.woocommerce.android.ui.orders.filters.domain.SaveOrderFilterToHistory
import com.woocommerce.android.ui.orders.filters.model.OrderFilterCategoryListViewState
import com.woocommerce.android.ui.orders.filters.model.OrderFilterCategoryUiModel
import com.woocommerce.android.ui.orders.filters.model.OrderFilterEvent.OnShowOrders
import com.woocommerce.android.ui.orders.filters.model.OrderFilterEvent.OpenFilterHistory
import com.woocommerce.android.ui.orders.filters.model.OrderFilterEvent.ShowFilterOptionsForCategory
import com.woocommerce.android.ui.orders.filters.model.OrderFilterOptionUiModel
import com.woocommerce.android.ui.products.list.ProductListRepository
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.util.FeatureFlagRepository
import com.woocommerce.android.util.getOrAwaitValue
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.store.WCCustomerStore

@ExperimentalCoroutinesApi
class OrderFilterCategoriesViewModelTest : BaseUnitTest() {
    private lateinit var savedStateHandle: SavedStateHandle
    private val resourceProvider: ResourceProvider = mock()
    private val getOrderStatusFilterOptions: GetOrderStatusFilterOptions = mock()
    private val getDateRangeFilterOptions: GetDateRangeFilterOptions = mock()
    private val orderFilterRepository: OrderFiltersRepository = mock()
    private val getTrackingForFilterSelection: GetTrackingForFilterSelection = mock()
    private val dateUtils: DateUtils = mock()
    private val analyticsTraWrapper: AnalyticsTrackerWrapper = mock()
    private val productListRepository: ProductListRepository = mock()
    private val customerStore: WCCustomerStore = mock()
    private val selectedSite: SelectedSite = mock()
    private val isSalesChannelFilterSupported: IsSalesChannelFilterSupported = mock()
    private val saveOrderFilterToHistory: SaveOrderFilterToHistory = mock()
    private val orderFilterHistoryMapper: OrderFilterHistoryMapper = mock()
    private val featureFlagRepository: FeatureFlagRepository = mock()

    private lateinit var viewModel: OrderFilterCategoriesViewModel

    private val currentCategoryList
        get() = viewModel.categories.liveData.value!!.list

    @Before
    fun setup() = testBlocking {
        savedStateHandle = SavedStateHandle()
        givenResourceProviderReturnsNonEmptyStrings()
        givenOrderStatusOptionsAvailable()
        givenDateRangeFiltersAvailable()
        whenever(isSalesChannelFilterSupported.invoke()).thenReturn(false)
        whenever(orderFilterRepository.getCustomDateRangeDays()).thenReturn(0L to 0L)
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

        verify(orderFilterRepository).setSelectedFilters(
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

        verify(orderFilterRepository).setSelectedFilters(
            AN_ORDER_STATUS_FILTER_CATEGORY_WITH_SELECTED_FILTER.categoryKey,
            emptyList()
        )
    }

    @Test
    fun `Given some selected filters, when onClearFilters, then all filter options should be unselected`() {
        givenAFilterOptionHasBeenSelected(AN_ORDER_STATUS_FILTER_CATEGORY_WITH_SELECTED_FILTER)

        viewModel.onClearFilters()

        assertThat(allFilterOptionsAreUnselected()).isTrue()
    }

    @Test
    fun `When clear button clicked, then clear button should be hidden and toolbar title updated to default`() {
        whenever(resourceProvider.getString(R.string.orderfilters_filters_default_title))
            .thenReturn(DEFAULT_FILTER_TITLE)

        viewModel.onClearFilters()

        assertThat(viewModel.orderFilterCategoryViewState.getOrAwaitValue()).isEqualTo(
            OrderFilterCategoryListViewState(
                screenTitle = DEFAULT_FILTER_TITLE,
                displayClearButton = false
            )
        )
    }

    @Test
    fun `When clear button clicked, then clear filter even is tracked`() {
        viewModel.onClearFilters()

        verify(analyticsTraWrapper).track(
            AnalyticsEvent.ORDER_FILTER_LIST_CLEAR_MENU_BUTTON_TAPPED
        )
    }

    @Test
    fun `Given product filter is applied, when onClearProductFilter is called, then product filter is reset`() {
        viewModel.onClearProductFilter()

        assertThat(
            viewModel.categories.liveData.getOrAwaitValue().list.filter {
                it.categoryKey == OrderListFilterCategory.PRODUCT
            }
        )
            .allMatch { it.orderFilterOptions.none { it.isSelected } }
    }

    @Test
    fun `given sales channel filter is supported, when initialized, then sales channel options include all channels`() =
        testBlocking {
            // GIVEN
            savedStateHandle = SavedStateHandle()
            whenever(isSalesChannelFilterSupported.invoke()).thenReturn(true)
            whenever(orderFilterRepository.getCurrentFilterSelection(any())).thenReturn(emptyList())

            // WHEN
            initViewModel()

            // THEN
            val salesChannelFilter = currentCategoryList.find {
                it.categoryKey == OrderListFilterCategory.SALES_CHANNEL
            }
            assertThat(salesChannelFilter).isNotNull
            val filterKeys = salesChannelFilter!!.orderFilterOptions.map { it.key }
            assertThat(filterKeys).contains(
                SalesChannel.POS.key,
                SalesChannel.WEB_CHECKOUT.key,
                SalesChannel.WP_ADMIN.key,
                OrderFilterOptionUiModel.DEFAULT_ALL_KEY
            )
        }

    @Test
    fun `given sales channel filter is not supported, when initialized, then sales channel filter is not shown`() =
        testBlocking {
            // GIVEN
            savedStateHandle = SavedStateHandle()
            whenever(isSalesChannelFilterSupported.invoke()).thenReturn(false)

            // WHEN
            initViewModel()

            // THEN
            val salesChannelFilter = currentCategoryList.find {
                it.categoryKey == OrderListFilterCategory.SALES_CHANNEL
            }
            assertThat(salesChannelFilter).isNull()
        }

    @Test
    fun `when sales channel filter is selected, then filter options are updated`() = testBlocking {
        // GIVEN
        savedStateHandle = SavedStateHandle()
        whenever(isSalesChannelFilterSupported.invoke()).thenReturn(true)
        whenever(orderFilterRepository.getCurrentFilterSelection(any())).thenReturn(emptyList())
        initViewModel()

        // WHEN
        viewModel.onSalesChannelSelected(SalesChannel.WEB_CHECKOUT.key)

        // THEN
        val salesChannelFilter = currentCategoryList.find {
            it.categoryKey == OrderListFilterCategory.SALES_CHANNEL
        }
        assertThat(salesChannelFilter).isNotNull
        val selectedOption = salesChannelFilter!!.orderFilterOptions.find { it.isSelected }
        assertThat(selectedOption?.key).isEqualTo(SalesChannel.WEB_CHECKOUT.key)
    }

    @Test
    fun `when show orders is clicked, then the current filter is saved to history`() = testBlocking {
        givenAFilterOptionHasBeenSelected(AN_ORDER_STATUS_FILTER_CATEGORY_WITH_SELECTED_FILTER)

        viewModel.onShowOrdersClicked()

        verify(saveOrderFilterToHistory).invoke()
    }

    @Test
    fun `when filter history button clicked, then entry point is tracked and history is opened`() {
        viewModel.onFilterHistoryButtonClicked()

        verify(analyticsTraWrapper).track(
            AnalyticsEvent.FILTER_HISTORY_BUTTON_TAPPED,
            mapOf(AnalyticsTracker.KEY_SOURCE to AnalyticsTracker.VALUE_FILTER_HISTORY_SOURCE_ORDERS)
        )
        assertThat(viewModel.event.value).isEqualTo(OpenFilterHistory)
    }

    @Test
    fun `given a past filter, when selected, then its selection is applied to the repository`() = testBlocking {
        val availableStatusKey = generateOrderStatusOptions().first().statusKey
        whenever(orderFilterHistoryMapper.fromPayload(A_PAYLOAD)).thenReturn(
            OrderFilterHistoryMapper.OrderFilterHistoryData(
                selections = mapOf(OrderListFilterCategory.ORDER_STATUS.name to listOf(availableStatusKey)),
                customDateRangeStart = 0,
                customDateRangeEnd = 0
            )
        )

        viewModel.onPastFilterSelected(SavedFilter(readableString = A_READABLE_STRING, payload = A_PAYLOAD))

        verify(orderFilterRepository).setSelectedFilters(
            OrderListFilterCategory.ORDER_STATUS,
            listOf(availableStatusKey)
        )
        verify(orderFilterRepository).setCustomDateRange(0, 0)
    }

    @Test
    fun `given a past filter with an unavailable category, when selected, then that value is dropped`() = testBlocking {
        // Sales Channel is unavailable in this test (isSalesChannelFilterSupported = false).
        whenever(orderFilterHistoryMapper.fromPayload(A_PAYLOAD)).thenReturn(
            OrderFilterHistoryMapper.OrderFilterHistoryData(
                selections = mapOf(OrderListFilterCategory.SALES_CHANNEL.name to listOf(SalesChannel.POS.key))
            )
        )

        viewModel.onPastFilterSelected(SavedFilter(readableString = A_READABLE_STRING, payload = A_PAYLOAD))

        verify(orderFilterRepository).setSelectedFilters(OrderListFilterCategory.SALES_CHANNEL, emptyList())
    }

    @Test
    fun `given a past filter with an order status that no longer exists, when selected, then it is dropped`() =
        testBlocking {
            whenever(orderFilterHistoryMapper.fromPayload(A_PAYLOAD)).thenReturn(
                OrderFilterHistoryMapper.OrderFilterHistoryData(
                    selections = mapOf(OrderListFilterCategory.ORDER_STATUS.name to listOf("deleted-status"))
                )
            )

            viewModel.onPastFilterSelected(SavedFilter(readableString = A_READABLE_STRING, payload = A_PAYLOAD))

            verify(orderFilterRepository).setSelectedFilters(OrderListFilterCategory.ORDER_STATUS, emptyList())
        }

    @Test
    fun `given a past filter with a product, when selected, then the product value is applied as-is`() = testBlocking {
        whenever(orderFilterHistoryMapper.fromPayload(A_PAYLOAD)).thenReturn(
            OrderFilterHistoryMapper.OrderFilterHistoryData(
                selections = mapOf(OrderListFilterCategory.PRODUCT.name to listOf("123"))
            )
        )

        viewModel.onPastFilterSelected(SavedFilter(readableString = A_READABLE_STRING, payload = A_PAYLOAD))

        verify(orderFilterRepository).setSelectedFilters(OrderListFilterCategory.PRODUCT, listOf("123"))
    }

    @Test
    fun `given a past filter, when selected, then categories absent from it are cleared`() = testBlocking {
        whenever(orderFilterHistoryMapper.fromPayload(A_PAYLOAD)).thenReturn(
            OrderFilterHistoryMapper.OrderFilterHistoryData(
                selections = mapOf(OrderListFilterCategory.ORDER_STATUS.name to listOf(ANY_ORDER_STATUS_KEY))
            )
        )

        viewModel.onPastFilterSelected(SavedFilter(readableString = A_READABLE_STRING, payload = A_PAYLOAD))

        verify(orderFilterRepository).setSelectedFilters(OrderListFilterCategory.DATE_RANGE, emptyList())
        verify(orderFilterRepository).setSelectedFilters(OrderListFilterCategory.PRODUCT, emptyList())
        verify(orderFilterRepository).setSelectedFilters(OrderListFilterCategory.CUSTOMER, emptyList())
        verify(orderFilterRepository).setSelectedFilters(OrderListFilterCategory.SALES_CHANNEL, emptyList())
    }

    @Test
    fun `given a past filter with a custom date range, when selected, then the range is restored`() = testBlocking {
        whenever(orderFilterHistoryMapper.fromPayload(A_PAYLOAD)).thenReturn(
            OrderFilterHistoryMapper.OrderFilterHistoryData(
                selections = mapOf(OrderListFilterCategory.DATE_RANGE.name to listOf("custom_range")),
                customDateRangeStart = 111,
                customDateRangeEnd = 222
            )
        )

        viewModel.onPastFilterSelected(SavedFilter(readableString = A_READABLE_STRING, payload = A_PAYLOAD))

        verify(orderFilterRepository).setCustomDateRange(111, 222)
    }

    @Test
    fun `given an undecodable past filter, when selected, then the repository is not touched`() = testBlocking {
        whenever(orderFilterHistoryMapper.fromPayload(A_PAYLOAD)).thenReturn(null)

        viewModel.onPastFilterSelected(SavedFilter(readableString = A_READABLE_STRING, payload = A_PAYLOAD))

        verify(orderFilterRepository, never()).setSelectedFilters(any(), any())
        verify(orderFilterRepository, never()).setCustomDateRange(any(), any())
    }

    @Test
    fun `given a changed selection, when discarding, then the original custom date range is restored`() = testBlocking {
        savedStateHandle = SavedStateHandle()
        whenever(orderFilterRepository.getCustomDateRangeDays()).thenReturn(10L to 20L)
        initViewModel()
        givenAFilterOptionHasBeenSelected(AN_ORDER_STATUS_FILTER_CATEGORY_WITH_SELECTED_FILTER)

        viewModel.onBackPressed()
        (viewModel.event.value as MultiLiveEvent.Event.ShowDialog).positiveBtnAction?.onClick(null, 0)

        verify(orderFilterRepository).setCustomDateRange(10, 20)
    }

    private fun allFilterOptionsAreUnselected() = currentCategoryList
        .map {
            it.orderFilterOptions.any { filterOption ->
                filterOption.isSelected && filterOption.key != OrderFilterOptionUiModel.DEFAULT_ALL_KEY
            }
        }.none { it }

    private fun givenAFilterOptionHasBeenSelected(updatedFilters: OrderFilterCategoryUiModel) {
        viewModel.onFilterOptionsUpdated(updatedFilters)
    }

    private fun initViewModel() {
        viewModel = OrderFilterCategoriesViewModel(
            savedState = savedStateHandle,
            resourceProvider = resourceProvider,
            getOrderStatusFilterOptions = getOrderStatusFilterOptions,
            getDateRangeFilterOptions = getDateRangeFilterOptions,
            orderFilterRepository = orderFilterRepository,
            getTrackingForFilterSelection = getTrackingForFilterSelection,
            dateUtils = dateUtils,
            analyticsTraWrapper = analyticsTraWrapper,
            productRepository = productListRepository,
            customerStore = customerStore,
            selectedSite = selectedSite,
            isSalesChannelFilterSupported = isSalesChannelFilterSupported,
            saveOrderFilterToHistory = saveOrderFilterToHistory,
            orderFilterHistoryMapper = orderFilterHistoryMapper,
            featureFlagRepository = featureFlagRepository
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

    private fun givenResourceProviderReturnsNonEmptyStrings() {
        whenever(resourceProvider.getString(any())).thenReturn("AnyString")
        whenever(resourceProvider.getString(any(), any(), any())).thenReturn("AnyString")
    }

    private companion object {
        const val DEFAULT_FILTER_TITLE = "Title"
        const val ANY_ORDER_STATUS_KEY = "OrderStatusOptionKey"
        const val A_PAYLOAD = "payload"
        const val A_READABLE_STRING = "readable"
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
