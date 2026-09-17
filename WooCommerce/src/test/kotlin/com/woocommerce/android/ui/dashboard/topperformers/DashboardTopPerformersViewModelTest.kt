package com.woocommerce.android.ui.dashboard.topperformers

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsUpdateDataStore.AnalyticData
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType
import com.woocommerce.android.ui.dashboard.DashboardViewModel
import com.woocommerce.android.ui.dashboard.DashboardViewModel.RefreshEvent
import com.woocommerce.android.ui.dashboard.data.TopPerformersCustomDateRangeDataStore
import com.woocommerce.android.ui.dashboard.domain.GetTopPerformers
import com.woocommerce.android.ui.dashboard.domain.GetTopPerformers.TopPerformerResult
import com.woocommerce.android.ui.dashboard.domain.ObserveLastUpdate
import com.woocommerce.android.ui.dashboard.topperformers.DashboardTopPerformersViewModel.ErrorType
import com.woocommerce.android.util.CalendarHelper
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.util.LocalizedDatePatternsTestRule
import com.woocommerce.android.util.ResultWithOutdatedFlag
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyVararg
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.Calendar

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardTopPerformersViewModelTest : BaseUnitTest() {
    @get:Rule
    val localizedDatePatterns = LocalizedDatePatternsTestRule()

    private val parentRefreshTrigger = MutableSharedFlow<RefreshEvent>(extraBufferCapacity = 1)
    private val parentViewModel: DashboardViewModel = mock {
        on { refreshTrigger } doReturn parentRefreshTrigger
    }
    private val networkStatus: NetworkStatus = mock {
        on { isConnected() } doReturn true
    }
    private val getTopPerformers: GetTopPerformers = mock()
    private val observeLastUpdate: ObserveLastUpdate = mock {
        on { invoke(any(), eq(AnalyticData.TOP_PERFORMERS)) } doReturn flowOf(LAST_UPDATE)
    }
    private val resourceProvider: ResourceProvider = mock {
        on { getString(any(), anyVararg()) } doReturn ""
    }
    private val appPrefsWrapper: AppPrefsWrapper = mock {
        val prefsChangesFlow = MutableStateFlow(SelectionType.TODAY.name)
        on { observePrefs() } doAnswer { prefsChangesFlow.map { } }
        on { getActiveTopPerformersTab() } doAnswer { prefsChangesFlow.value }
    }
    private val customDateRangeDataStore: TopPerformersCustomDateRangeDataStore = mock {
        on { dateRange } doReturn flowOf(null)
    }
    private val calendarHelper: CalendarHelper = mock {
        on { getCalendarForSelectedSite() } doReturn Calendar.getInstance()
    }
    private val dateUtils: DateUtils = mock()

    private lateinit var viewModel: DashboardTopPerformersViewModel

    private suspend fun setup(prepareMocks: suspend () -> Unit = {}) {
        prepareMocks()
        viewModel = DashboardTopPerformersViewModel(
            savedState = SavedStateHandle(),
            parentViewModel = parentViewModel,
            selectedSite = mock(),
            networkStatus = networkStatus,
            observeLastUpdate = observeLastUpdate,
            resourceProvider = resourceProvider,
            getTopPerformers = getTopPerformers,
            currencyFormatter = mock(),
            usageTracksEventEmitter = mock(),
            analyticsTrackerWrapper = mock(),
            wooCommerceStore = mock(),
            dateUtils = dateUtils,
            appPrefsWrapper = appPrefsWrapper,
            customDateRangeDataStore = customDateRangeDataStore,
            dateFormatter = mock(),
            getSelectedDateRange = GetSelectedRangeForTopPerformers(
                appPrefs = appPrefsWrapper,
                customDateRangeDataStore = customDateRangeDataStore,
                dateUtils = dateUtils,
                calendarHelper = calendarHelper
            )
        )
    }

    @Test
    fun `given fetching top performers fails, when the card loads, then the error is shown`() = testBlocking {
        setup {
            whenever(getTopPerformers.invoke(any(), any())).thenReturn(flowOf(TopPerformerResult.Error(Exception())))
        }

        val state = viewModel.topPerformersState.value

        assertThat(state?.error).isEqualTo(ErrorType.Generic)
        assertThat(state?.isLoading).isFalse()
    }

    @Test
    fun `given the card shows an error, when a retry succeeds, then the error is cleared`() = testBlocking {
        setup {
            whenever(getTopPerformers.invoke(any(), any()))
                .thenReturn(flowOf(TopPerformerResult.Error(Exception())))
                .thenReturn(flowOf(TopPerformerResult.Success(ResultWithOutdatedFlag(TOP_PERFORMERS))))
        }

        viewModel.onRefresh()

        val state = viewModel.topPerformersState.value
        assertThat(state?.error).isNull()
        assertThat(state?.isLoading).isFalse()
        assertThat(state?.topPerformers?.map { it.productId }).isEqualTo(TOP_PERFORMERS.map { it.productId })
    }

    @Test
    fun `given an offline pull to refresh showed an error, when a retry succeeds online, then the error is cleared`() =
        testBlocking {
            setup {
                whenever(getTopPerformers.invoke(any(), any()))
                    .thenReturn(flowOf(TopPerformerResult.Success(ResultWithOutdatedFlag(TOP_PERFORMERS))))
                whenever(networkStatus.isConnected()).thenReturn(true, false, true)
            }
            parentRefreshTrigger.tryEmit(RefreshEvent(isForced = true))

            viewModel.onRefresh()

            val state = viewModel.topPerformersState.value
            assertThat(state?.error).isNull()
            assertThat(state?.topPerformers?.map { it.productId }).isEqualTo(TOP_PERFORMERS.map { it.productId })
        }

    @Test
    fun `given the card shows an error, when a reload starts loading, then the error is cleared`() = testBlocking {
        setup {
            whenever(getTopPerformers.invoke(any(), any()))
                .thenReturn(flowOf(TopPerformerResult.Error(Exception())))
                .thenReturn(flowOf(TopPerformerResult.Loading))
        }

        parentRefreshTrigger.tryEmit(RefreshEvent())

        val state = viewModel.topPerformersState.value
        assertThat(state?.error).isNull()
        assertThat(state?.isLoading).isTrue()
    }

    private companion object {
        const val LAST_UPDATE = 1690382344865L
        val TOP_PERFORMERS = listOf(
            GetTopPerformers.TopPerformerProduct(
                productId = 134,
                name = "Shirt",
                quantity = 4,
                currency = "USD",
                total = 10.50,
                imageUrl = null
            )
        )
    }
}
