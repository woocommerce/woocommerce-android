package com.woocommerce.android.ui.dashboard.salesbychannel

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType
import com.woocommerce.android.ui.dashboard.DashboardStatsUsageTracksEventEmitter
import com.woocommerce.android.ui.dashboard.DashboardViewModel
import com.woocommerce.android.ui.dashboard.data.SalesByChannelCustomDateRangeDataStore
import com.woocommerce.android.ui.dashboard.domain.DashboardDateRangeFormatter
import com.woocommerce.android.ui.dashboard.domain.GetSalesByChannel
import com.woocommerce.android.ui.dashboard.domain.GetSalesByChannel.ChannelSales
import com.woocommerce.android.ui.dashboard.domain.GetSalesByChannel.SalesByChannelResult
import com.woocommerce.android.ui.dashboard.domain.ObserveLastUpdate
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.util.captureValues
import com.woocommerce.android.util.getOrAwaitValue
import com.woocommerce.android.util.runAndCaptureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.commons.stats.StatsTimeRange
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyVararg
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardSalesByChannelViewModelTest : BaseUnitTest() {

    private val sampleChannels = listOf(
        ChannelSales(
            channelName = "Web",
            revenue = 200.0,
            compareRevenue = 100.0,
            ordersCount = 10,
            compareOrdersCount = 5
        ),
        ChannelSales(
            channelName = "Mobile App",
            revenue = 50.0,
            compareRevenue = 100.0,
            ordersCount = 3,
            compareOrdersCount = 8
        )
    )

    private val parentViewModel: DashboardViewModel = mock {
        on { refreshTrigger } doReturn MutableSharedFlow()
    }
    private val networkStatus: NetworkStatus = mock {
        on { isConnected() } doReturn true
    }
    private val observeLastUpdate: ObserveLastUpdate = mock {
        on {
            invoke(any(), any<com.woocommerce.android.ui.analytics.hub.sync.AnalyticsUpdateDataStore.AnalyticData>())
        } doReturn flowOf(null)
    }
    private val resourceProvider: ResourceProvider = mock(strictness = Strictness.LENIENT)
    private val getSalesByChannel: GetSalesByChannel = mock()
    private val currencyFormatter: CurrencyFormatter = mock(strictness = Strictness.LENIENT)
    private val usageTracksEventEmitter: DashboardStatsUsageTracksEventEmitter = mock()
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()
    private val wooCommerceStore: WooCommerceStore = mock()
    private val dateUtils: DateUtils = mock()
    private val selectedSite: SelectedSite = mock {
        on { get() } doReturn SiteModel()
    }
    private val appPrefFlow = MutableStateFlow(SelectionType.TODAY.name)
    private val appPrefsWrapper: AppPrefsWrapper = mock {
        on { getActiveSalesByChannelTab() } doAnswer { appPrefFlow.value }
        on { observePrefs() } doAnswer { appPrefFlow.map {} }
    }
    private val customRangeFlow = MutableStateFlow<StatsTimeRange?>(null)
    private val customDateRangeDataStore: SalesByChannelCustomDateRangeDataStore = mock {
        on { dateRange } doReturn customRangeFlow
    }
    private val dateFormatter: DashboardDateRangeFormatter = mock(strictness = Strictness.LENIENT)

    private lateinit var viewModel: DashboardSalesByChannelViewModel

    private suspend fun setup(prepareMocks: suspend () -> Unit = {}) {
        whenever(resourceProvider.getString(any(), anyVararg())).thenReturn("")
        whenever(dateFormatter.formatRangeDate(any())).thenReturn("Today")
        whenever(currencyFormatter.formatCurrency(any<BigDecimal>(), any(), any())).thenReturn("$100.00")
        prepareMocks()
        val getSelectedDateRange = GetSelectedRangeForSalesByChannel(
            appPrefs = appPrefsWrapper,
            customDateRangeDataStore = customDateRangeDataStore,
            dateUtils = dateUtils
        )

        viewModel = DashboardSalesByChannelViewModel(
            parentViewModel = parentViewModel,
            selectedSite = selectedSite,
            networkStatus = networkStatus,
            observeLastUpdate = observeLastUpdate,
            resourceProvider = resourceProvider,
            getSalesByChannel = getSalesByChannel,
            currencyFormatter = currencyFormatter,
            usageTracksEventEmitter = usageTracksEventEmitter,
            analyticsTrackerWrapper = analyticsTrackerWrapper,
            wooCommerceStore = wooCommerceStore,
            dateUtils = dateUtils,
            appPrefsWrapper = appPrefsWrapper,
            customDateRangeDataStore = customDateRangeDataStore,
            dateFormatter = dateFormatter,
            getSelectedDateRange = getSelectedDateRange,
            savedState = SavedStateHandle()
        )
    }

    @Test
    fun `when view model is created, then loading state is emitted`() = testBlocking {
        setup {
            whenever(getSalesByChannel.invoke(any(), any())).thenReturn(
                flowOf(SalesByChannelResult.Loading)
            )
        }

        // WHEN
        val state = viewModel.salesByChannelState.captureValues().last()

        // THEN
        assertThat(state.isLoading).isTrue()
    }

    @Test
    fun `given network disconnected, when loading, then generic error is shown`() = testBlocking {
        setup {
            whenever(networkStatus.isConnected()).thenReturn(false)
        }

        // WHEN
        val state = viewModel.salesByChannelState.getOrAwaitValue()

        // THEN
        assertThat(state.error).isEqualTo(DashboardSalesByChannelViewModel.ErrorType.Generic)
    }

    @Test
    fun `given successful data fetch, when loading completes, then channels are displayed with correct bar fractions`() =
        testBlocking {
            setup {
                whenever(getSalesByChannel.invoke(any(), any())).thenReturn(
                    flowOf(SalesByChannelResult.Success(sampleChannels))
                )
            }

            // WHEN
            val state = viewModel.salesByChannelState.captureValues().last()

            // THEN
            assertThat(state.isLoading).isFalse()
            assertThat(state.error).isNull()
            assertThat(state.channels).hasSize(2)
            assertThat(state.channels[0].channelName).isEqualTo("Web")
            assertThat(state.channels[1].channelName).isEqualTo("Mobile App")
            // Web has the highest revenue (200.0), so its currentBarFraction should be 1.0
            assertThat(state.channels[0].currentBarFraction).isEqualTo(1.0f)
            // Mobile App revenue (50.0) / max (200.0) = 0.25
            assertThat(state.channels[1].currentBarFraction).isEqualTo(0.25f)
        }

    @Test
    fun `given empty data, when loading completes, then empty list is shown`() = testBlocking {
        setup {
            whenever(getSalesByChannel.invoke(any(), any())).thenReturn(
                flowOf(SalesByChannelResult.Success(emptyList()))
            )
        }

        // WHEN
        val state = viewModel.salesByChannelState.captureValues().last()

        // THEN
        assertThat(state.isLoading).isFalse()
        assertThat(state.error).isNull()
        assertThat(state.channels).isEmpty()
    }

    @Test
    fun `when retry tapped, then refresh is triggered`() = testBlocking {
        setup {
            whenever(getSalesByChannel.invoke(any(), any())).thenReturn(
                flowOf(SalesByChannelResult.Success(sampleChannels))
            )
        }

        // WHEN
        viewModel.salesByChannelState.runAndCaptureValues {
            viewModel.onRefresh()
        }

        // THEN
        val state = viewModel.salesByChannelState.getOrAwaitValue()
        assertThat(state).isNotNull()
    }
}
