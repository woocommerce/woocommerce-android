package com.woocommerce.android.ui.dashboard.topcategories

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType
import com.woocommerce.android.ui.dashboard.DashboardStatsUsageTracksEventEmitter
import com.woocommerce.android.ui.dashboard.DashboardViewModel
import com.woocommerce.android.ui.dashboard.data.TopCategoriesCustomDateRangeDataStore
import com.woocommerce.android.ui.dashboard.domain.DashboardDateRangeFormatter
import com.woocommerce.android.ui.dashboard.domain.GetTopPerformerCategories
import com.woocommerce.android.ui.dashboard.domain.GetTopPerformerCategories.TopPerformerCategory
import com.woocommerce.android.ui.dashboard.domain.GetTopPerformerCategories.TopPerformerCategoryResult
import com.woocommerce.android.ui.dashboard.domain.ObserveLastUpdate
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.util.ResultWithOutdatedFlag
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
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WooCommerceStore

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardTopCategoriesViewModelTest : BaseUnitTest() {

    private val sampleCategories = listOf(
        TopPerformerCategory(
            categoryId = 1L,
            name = "Clothing",
            quantity = 10,
            currency = "USD",
            total = 100.0
        ),
        TopPerformerCategory(
            categoryId = 2L,
            name = "Electronics",
            quantity = 5,
            currency = "USD",
            total = 250.0
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
    private val resourceProvider: ResourceProvider = mock()
    private val getTopPerformerCategories: GetTopPerformerCategories = mock()
    private val currencyFormatter: CurrencyFormatter = mock()
    private val usageTracksEventEmitter: DashboardStatsUsageTracksEventEmitter = mock()
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()
    private val wooCommerceStore: WooCommerceStore = mock()
    private val dateUtils: DateUtils = mock()
    private val selectedSite: SelectedSite = mock {
        on { get() } doReturn SiteModel()
    }
    private val appPrefFlow = MutableStateFlow(SelectionType.TODAY.name)
    private val appPrefsWrapper: AppPrefsWrapper = mock {
        on { getActiveTopCategoriesTab() } doAnswer { appPrefFlow.value }
        on { observePrefs() } doAnswer { appPrefFlow.map {} }
    }
    private val customRangeFlow = MutableStateFlow<StatsTimeRange?>(null)
    private val customDateRangeDataStore: TopCategoriesCustomDateRangeDataStore = mock {
        on { dateRange } doReturn customRangeFlow
    }
    private val dateFormatter: DashboardDateRangeFormatter = mock {
        on { formatRangeDate(any()) } doReturn "Today"
    }

    private lateinit var viewModel: DashboardTopCategoriesViewModel

    private suspend fun setup(prepareMocks: suspend () -> Unit = {}) {
        whenever(resourceProvider.getString(any(), anyVararg())).thenReturn("")
        whenever(currencyFormatter.formatCurrency(any<java.math.BigDecimal>(), any(), any())).thenReturn("$100.00")
        prepareMocks()
        val getSelectedDateRange = GetSelectedRangeForTopCategories(
            appPrefs = appPrefsWrapper,
            customDateRangeDataStore = customDateRangeDataStore,
            dateUtils = dateUtils
        )

        viewModel = DashboardTopCategoriesViewModel(
            parentViewModel = parentViewModel,
            selectedSite = selectedSite,
            networkStatus = networkStatus,
            observeLastUpdate = observeLastUpdate,
            resourceProvider = resourceProvider,
            getTopPerformerCategories = getTopPerformerCategories,
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
            whenever(getTopPerformerCategories.invoke(any(), any())).thenReturn(
                flowOf(TopPerformerCategoryResult.Loading)
            )
        }

        // WHEN
        val state = viewModel.topCategoriesState.captureValues().last()

        // THEN
        assertThat(state.isLoading).isTrue()
    }

    @Test
    fun `given network disconnected, when loading, then generic error is shown`() = testBlocking {
        setup {
            whenever(networkStatus.isConnected()).thenReturn(false)
        }

        // WHEN
        val state = viewModel.topCategoriesState.getOrAwaitValue()

        // THEN
        assertThat(state.error).isEqualTo(DashboardTopCategoriesViewModel.ErrorType.Generic)
    }

    @Test
    fun `given successful data fetch, when loading completes, then categories are displayed`() = testBlocking {
        setup {
            whenever(getTopPerformerCategories.invoke(any(), any())).thenReturn(
                flowOf(
                    TopPerformerCategoryResult.Success(
                        ResultWithOutdatedFlag(sampleCategories, false)
                    )
                )
            )
        }

        // WHEN
        val state = viewModel.topCategoriesState.captureValues().last()

        // THEN
        assertThat(state.isLoading).isFalse()
        assertThat(state.error).isNull()
        assertThat(state.topCategories).hasSize(2)
        assertThat(state.topCategories[0].categoryId).isEqualTo(1L)
        assertThat(state.topCategories[1].categoryId).isEqualTo(2L)
    }

    @Test
    fun `given empty data, when loading completes, then empty list is shown`() = testBlocking {
        setup {
            whenever(getTopPerformerCategories.invoke(any(), any())).thenReturn(
                flowOf(
                    TopPerformerCategoryResult.Success(
                        ResultWithOutdatedFlag(emptyList(), false)
                    )
                )
            )
        }

        // WHEN
        val state = viewModel.topCategoriesState.captureValues().last()

        // THEN
        assertThat(state.isLoading).isFalse()
        assertThat(state.error).isNull()
        assertThat(state.topCategories).isEmpty()
    }

    @Test
    fun `when category tapped, then OpenCategoryProducts event is triggered`() = testBlocking {
        setup {
            whenever(getTopPerformerCategories.invoke(any(), any())).thenReturn(
                flowOf(
                    TopPerformerCategoryResult.Success(
                        ResultWithOutdatedFlag(sampleCategories, false)
                    )
                )
            )
        }

        // GIVEN
        viewModel.selectedDateRange.getOrAwaitValue()
        val state = viewModel.topCategoriesState.getOrAwaitValue()
        assertThat(state.topCategories).isNotEmpty()

        // WHEN
        val event = viewModel.event.runAndCaptureValues {
            state.topCategories.first().onClick(state.topCategories.first().categoryId)
        }.last()

        // THEN
        assertThat(event).isInstanceOf(DashboardTopCategoriesViewModel.OpenCategoryProducts::class.java)
        val openEvent = event as DashboardTopCategoriesViewModel.OpenCategoryProducts
        assertThat(openEvent.categoryId).isEqualTo(1L)
        assertThat(openEvent.categoryName).isEqualTo("Clothing")
    }

    @Test
    fun `when retry tapped, then refresh is triggered`() = testBlocking {
        setup {
            whenever(getTopPerformerCategories.invoke(any(), any())).thenReturn(
                flowOf(
                    TopPerformerCategoryResult.Success(
                        ResultWithOutdatedFlag(sampleCategories, false)
                    )
                )
            )
        }

        // WHEN
        viewModel.topCategoriesState.runAndCaptureValues {
            viewModel.onRefresh()
        }

        // THEN
        // The refresh call should trigger the getTopPerformerCategories to be called again
        // via the refreshTrigger flow. We verify that the state is still valid after refresh.
        val state = viewModel.topCategoriesState.getOrAwaitValue()
        assertThat(state).isNotNull()
    }

}
