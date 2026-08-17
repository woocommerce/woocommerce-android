package com.woocommerce.android.ui.orders.filters.domain

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.filters.FilterHistoryRepository
import com.woocommerce.android.ui.filters.FilterHistoryType
import com.woocommerce.android.ui.orders.filters.OrderFilterHistoryMapper
import com.woocommerce.android.ui.orders.filters.data.OrderFiltersRepository
import com.woocommerce.android.ui.orders.filters.data.OrderListFilterCategory
import com.woocommerce.android.ui.orders.filters.data.OrderListFilterCategory.CUSTOMER
import com.woocommerce.android.ui.orders.filters.data.OrderListFilterCategory.PRODUCT
import com.woocommerce.android.ui.orders.filters.data.OrderListFilterCategory.SALES_CHANNEL
import com.woocommerce.android.ui.orders.filters.data.OrderStatusOption
import com.woocommerce.android.ui.orders.filters.data.SalesChannel
import com.woocommerce.android.ui.orders.filters.model.OrderFilterCategoryUiModel
import com.woocommerce.android.ui.products.list.ProductListRepository
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.model.customer.WCCustomerModel
import org.wordpress.android.fluxc.store.WCCustomerStore

@OptIn(ExperimentalCoroutinesApi::class)
class SaveOrderFilterToHistoryTest : BaseUnitTest() {
    private val featureFlagRepository: FeatureFlagRepository = mock()
    private val orderFiltersRepository: OrderFiltersRepository = mock()
    private val getOrderStatusFilterOptions: GetOrderStatusFilterOptions = mock()
    private val getDateRangeFilterOptions: GetDateRangeFilterOptions = mock()
    private val dateUtils: DateUtils = mock()
    private val resourceProvider: ResourceProvider = mock()
    private val productListRepository: ProductListRepository = mock()
    private val customerStore: WCCustomerStore = mock()
    private val selectedSite: SelectedSite = mock()
    private val filterHistoryRepository: FilterHistoryRepository = mock()
    private val orderFilterHistoryMapper: OrderFilterHistoryMapper = mock()

    private val sut = SaveOrderFilterToHistory(
        featureFlagRepository = featureFlagRepository,
        orderFiltersRepository = orderFiltersRepository,
        getOrderStatusFilterOptions = getOrderStatusFilterOptions,
        getDateRangeFilterOptions = getDateRangeFilterOptions,
        dateUtils = dateUtils,
        resourceProvider = resourceProvider,
        productListRepository = productListRepository,
        customerStore = customerStore,
        selectedSite = selectedSite,
        filterHistoryRepository = filterHistoryRepository,
        orderFilterHistoryMapper = orderFilterHistoryMapper,
        appCoroutineScope = CoroutineScope(coroutinesTestRule.testDispatcher)
    )

    @Test
    fun `given feature flag disabled, when invoked, then nothing is saved`() = testBlocking {
        whenever(featureFlagRepository.isEnabled(FeatureFlag.FILTER_HISTORY)).thenReturn(false)

        sut()

        verify(filterHistoryRepository, never()).save(any(), any(), any())
    }

    @Test
    fun `given no filter is selected, when invoked, then nothing is saved`() = testBlocking {
        whenever(featureFlagRepository.isEnabled(FeatureFlag.FILTER_HISTORY)).thenReturn(true)
        whenever(getOrderStatusFilterOptions.invoke()).thenReturn(
            listOf(OrderStatusOption("processing", "Processing", statusCount = 0, isSelected = false))
        )
        whenever(getDateRangeFilterOptions.invoke()).thenReturn(emptyList())
        whenever(orderFiltersRepository.productFilter).thenReturn(null)
        whenever(orderFiltersRepository.customerFilter).thenReturn(null)
        whenever(orderFiltersRepository.getCurrentFilterSelection(OrderListFilterCategory.SALES_CHANNEL))
            .thenReturn(emptyList())

        sut()

        verify(filterHistoryRepository, never()).save(any(), any(), any())
    }

    @Test
    fun `given a filter is selected, when invoked, then it is saved to history`() = testBlocking {
        whenever(featureFlagRepository.isEnabled(FeatureFlag.FILTER_HISTORY)).thenReturn(true)
        whenever(getOrderStatusFilterOptions.invoke()).thenReturn(
            listOf(OrderStatusOption("processing", "Processing", statusCount = 0, isSelected = true))
        )
        whenever(getDateRangeFilterOptions.invoke()).thenReturn(emptyList())
        whenever(orderFiltersRepository.productFilter).thenReturn(null)
        whenever(orderFiltersRepository.customerFilter).thenReturn(null)
        whenever(orderFiltersRepository.getCurrentFilterSelection(OrderListFilterCategory.SALES_CHANNEL))
            .thenReturn(emptyList())
        whenever(orderFiltersRepository.getCustomDateRangeDays()).thenReturn(0L to 0L)
        whenever(orderFilterHistoryMapper.toPayload(any(), any(), any())).thenReturn(A_PAYLOAD)

        sut()

        verify(filterHistoryRepository).save(FilterHistoryType.ORDERS, A_PAYLOAD, A_READABLE_STRING)
    }

    @Test
    fun `given a selected order status with a count, when invoked, then the label ignores the count`() = testBlocking {
        whenever(featureFlagRepository.isEnabled(FeatureFlag.FILTER_HISTORY)).thenReturn(true)
        whenever(getOrderStatusFilterOptions.invoke()).thenReturn(
            listOf(OrderStatusOption("processing", "Processing", statusCount = 12, isSelected = true))
        )
        whenever(getDateRangeFilterOptions.invoke()).thenReturn(emptyList())
        whenever(orderFiltersRepository.productFilter).thenReturn(null)
        whenever(orderFiltersRepository.customerFilter).thenReturn(null)
        whenever(orderFiltersRepository.getCurrentFilterSelection(OrderListFilterCategory.SALES_CHANNEL))
            .thenReturn(emptyList())
        whenever(orderFiltersRepository.getCustomDateRangeDays()).thenReturn(0L to 0L)

        sut()

        val readableCaptor = argumentCaptor<String>()
        verify(filterHistoryRepository).save(any(), anyOrNull(), readableCaptor.capture())
        assertThat(readableCaptor.firstValue).isEqualTo("Processing")
    }

    @Test
    fun `given a selected product, when invoked, then the product name is used as the label`() = testBlocking {
        givenNothingSelected()
        whenever(orderFiltersRepository.productFilter).thenReturn(PRODUCT_ID)
        whenever(productListRepository.getProduct(PRODUCT_ID)).thenReturn(WCProductModel().copy(name = "Widget"))

        sut()

        val option = capturedCategory(PRODUCT).orderFilterOptions.first()
        assertThat(option.key).isEqualTo(PRODUCT_ID.toString())
        assertThat(option.displayName).isEqualTo("Widget")
    }

    @Test
    fun `given a selected customer, when invoked, then the customer name is used as the label`() = testBlocking {
        givenNothingSelected()
        whenever(orderFiltersRepository.customerFilter).thenReturn(CUSTOMER_ID)
        whenever(selectedSite.get()).thenReturn(SiteModel())
        whenever(customerStore.getCustomerByRemoteId(any(), eq(CUSTOMER_ID)))
            .thenReturn(WCCustomerModel(firstName = "John", lastName = "Doe"))

        sut()

        val option = capturedCategory(CUSTOMER).orderFilterOptions.first()
        assertThat(option.key).isEqualTo(CUSTOMER_ID.toString())
        assertThat(option.displayName).isEqualTo("John Doe")
    }

    @Test
    fun `given selected sales channels, when invoked, then known keys are mapped and unknown keys dropped`() =
        testBlocking {
            givenNothingSelected()
            whenever(orderFiltersRepository.getCurrentFilterSelection(SALES_CHANNEL))
                .thenReturn(listOf(SalesChannel.POS.key, "unknown_channel"))
            whenever(resourceProvider.getString(any())).thenReturn("Point of Sale")

            sut()

            val options = capturedCategory(SALES_CHANNEL).orderFilterOptions
            assertThat(options.map { it.key }).containsExactly(SalesChannel.POS.key)
            assertThat(options.first().displayName).isEqualTo("Point of Sale")
        }

    @Test
    fun `given a custom date range, when invoked, then its bounds are forwarded to the payload`() = testBlocking {
        givenNothingSelected()
        whenever(getOrderStatusFilterOptions.invoke()).thenReturn(
            listOf(OrderStatusOption("processing", "Processing", statusCount = 0, isSelected = true))
        )
        whenever(orderFiltersRepository.getCustomDateRangeDays()).thenReturn(111L to 222L)

        sut()

        verify(orderFilterHistoryMapper).toPayload(any(), eq(111L), eq(222L))
    }

    private suspend fun givenNothingSelected() {
        whenever(featureFlagRepository.isEnabled(FeatureFlag.FILTER_HISTORY)).thenReturn(true)
        whenever(getOrderStatusFilterOptions.invoke()).thenReturn(emptyList())
        whenever(getDateRangeFilterOptions.invoke()).thenReturn(emptyList())
        whenever(orderFiltersRepository.productFilter).thenReturn(null)
        whenever(orderFiltersRepository.customerFilter).thenReturn(null)
        whenever(orderFiltersRepository.getCurrentFilterSelection(SALES_CHANNEL)).thenReturn(emptyList())
        whenever(orderFiltersRepository.getCustomDateRangeDays()).thenReturn(0L to 0L)
    }

    private fun capturedCategory(categoryKey: OrderListFilterCategory): OrderFilterCategoryUiModel {
        val categoriesCaptor = argumentCaptor<List<OrderFilterCategoryUiModel>>()
        verify(orderFilterHistoryMapper).toPayload(categoriesCaptor.capture(), any(), any())
        return categoriesCaptor.firstValue.first { it.categoryKey == categoryKey }
    }

    private companion object {
        const val A_PAYLOAD = "payload"
        const val A_READABLE_STRING = "Processing"
        const val PRODUCT_ID = 123L
        const val CUSTOMER_ID = 456L
    }
}
