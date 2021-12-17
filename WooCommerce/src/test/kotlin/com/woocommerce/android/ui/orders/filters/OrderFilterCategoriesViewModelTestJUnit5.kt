package com.woocommerce.android.ui.orders.filters

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.ui.orders.OrderTestUtils
import com.woocommerce.android.ui.orders.filters.data.OrderFiltersRepository
import com.woocommerce.android.ui.orders.filters.data.OrderListFilterCategory
import com.woocommerce.android.ui.orders.filters.data.OrderStatusOption
import com.woocommerce.android.ui.orders.filters.domain.GetDateRangeFilterOptions
import com.woocommerce.android.ui.orders.filters.domain.GetOrderStatusFilterOptions
import com.woocommerce.android.ui.orders.filters.domain.GetTrackingForFilterSelection
import com.woocommerce.android.ui.orders.filters.model.OrderFilterCategoryUiModel
import com.woocommerce.android.ui.orders.filters.model.OrderFilterEvent
import com.woocommerce.android.ui.orders.filters.model.OrderFilterOptionUiModel
import com.woocommerce.android.util.CoroutinesTestExtension
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.util.InstantExecutorExtension
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@ExtendWith(InstantExecutorExtension::class)
class OrderFilterCategoriesViewModelTestJUnit5 {
    private val savedStateHandle: SavedStateHandle = SavedStateHandle()
    private val resourceProvider: ResourceProvider = mock()
    private val getOrderStatusFilterOptions: GetOrderStatusFilterOptions = mock()
    private val getDateRangeFilterOptions: GetDateRangeFilterOptions = mock()
    private val orderFilterRepository: OrderFiltersRepository = mock()
    private val getTrackingForFilterSelection: GetTrackingForFilterSelection = mock()
    private val dateUtils: DateUtils = mock()

    private lateinit var viewModel: OrderFilterCategoriesViewModel

    @RegisterExtension @JvmField
    val coroutinesTestExtension = CoroutinesTestExtension()

    @BeforeEach
    fun setup() = runBlockingTest {
        givenResourceProviderReturnsNonEmptyStrings()
        givenOrderStatusOptionsAvailable()
        initViewModel()
    }

    @Nested
    inner class FilterCategorySelected {
        @Test
        @DisplayName("When filter category is selected, then update screen title")
        fun selectCategoryUpdatesScreen() {
            initViewModel()
            viewModel.onFilterCategorySelected(AN_ORDER_STATUS_FILTER_CATEGORY_WITH_SELECTED_FILTER)

            Assertions.assertThat(viewModel.event.value).isEqualTo(
                OrderFilterEvent.ShowFilterOptionsForCategory(
                    AN_ORDER_STATUS_FILTER_CATEGORY_WITH_SELECTED_FILTER
                )
            )
        }
    }

    private fun initViewModel() {
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

    private suspend fun givenOrderStatusOptionsAvailable() {
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

    private fun givenResourceProviderReturnsNonEmptyStrings() {
        whenever(resourceProvider.getString(any())).thenReturn("AnyString")
        whenever(resourceProvider.getString(any(), any(), any())).thenReturn("AnyString")
    }

    private companion object {
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
