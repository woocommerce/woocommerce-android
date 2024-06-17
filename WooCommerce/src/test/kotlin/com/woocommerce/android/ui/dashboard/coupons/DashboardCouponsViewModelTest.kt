package com.woocommerce.android.ui.dashboard.coupons

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.Coupon
import com.woocommerce.android.model.CouponPerformanceReport
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRange
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType
import com.woocommerce.android.ui.coupons.CouponRepository
import com.woocommerce.android.ui.dashboard.DashboardViewModel
import com.woocommerce.android.ui.dashboard.DashboardViewModel.RefreshEvent
import com.woocommerce.android.ui.dashboard.data.CouponsCustomDateRangeDataStore
import com.woocommerce.android.ui.dashboard.domain.DashboardDateRangeFormatter
import com.woocommerce.android.ui.products.ParameterRepository
import com.woocommerce.android.ui.products.models.SiteParameters
import com.woocommerce.android.util.CouponUtils
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.util.captureValues
import com.woocommerce.android.util.getOrAwaitValue
import com.woocommerce.android.util.runAndCaptureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardCouponsViewModelTest : BaseUnitTest() {
    private val sampleCouponReports = (1L..3L).map {
        CouponPerformanceReport(it, 10, BigDecimal.TEN)
    }
    private val sampleCoupons = sampleCouponReports.map {
        Coupon(
            id = it.couponId,
            productIds = emptyList(),
            categoryIds = emptyList(),
            restrictions = Coupon.CouponRestrictions(
                excludedProductIds = emptyList(),
                excludedCategoryIds = emptyList(),
                restrictedEmails = emptyList()
            )
        )
    }

    private val parentViewModel: DashboardViewModel = mock {
        on { refreshTrigger } doReturn emptyFlow()
    }
    private val couponRepository: CouponRepository = mock {
        onBlocking { fetchMostActiveCoupons(any(), any()) } doReturn Result.success(sampleCouponReports)
        onBlocking { getCoupons(any()) } doReturn sampleCoupons
        onBlocking { fetchCoupons(any(), any(), any()) } doReturn Result.success(false)
        on { observeCoupons(any()) } doReturn flowOf(sampleCoupons)
    }
    private val couponUtils: CouponUtils = mock {
        on { generateSummary(any(), any()) } doReturn "Coupon summary"
    }
    private val appPrefs: AppPrefsWrapper = mock {
        val prefFlow = MutableStateFlow(SelectionType.TODAY.name)

        on { getActiveCouponsTab() } doAnswer { prefFlow.value }
        on { setActiveCouponsTab(any()) } doAnswer { prefFlow.value = it.arguments[0] as String }
        on { observePrefs() } doAnswer { prefFlow.map { Unit } }
    }
    private val dateUtils: DateUtils = mock()
    private val parameterRepository: ParameterRepository = mock {
        onBlocking { getParameters() } doReturn SiteParameters(
            currencyCode = "USD",
            currencySymbol = "$",
            currencyFormattingParameters = null,
            weightUnit = "kg",
            dimensionUnit = "cm",
            gmtOffset = 0f
        )
    }
    private val dateRangeFormatter: DashboardDateRangeFormatter = mock()
    private val customDateRangeDataStore: CouponsCustomDateRangeDataStore = mock {
        val rangeFlow = MutableStateFlow<StatsTimeRange?>(null)
        on { dateRange } doReturn rangeFlow
        onBlocking { updateDateRange(any()) } doAnswer { rangeFlow.value = it.arguments[0] as StatsTimeRange }
    }
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()

    private lateinit var viewModel: DashboardCouponsViewModel

    suspend fun setup(prepareMocks: suspend () -> Unit = {}) {
        prepareMocks()
        val getSelectedDateRange = GetSelectedRangeForCoupons(
            appPrefs = appPrefs,
            customDateRangeDataStore = customDateRangeDataStore,
            dateUtils = dateUtils
        )

        viewModel = DashboardCouponsViewModel(
            savedStateHandle = SavedStateHandle(),
            parentViewModel = parentViewModel,
            couponRepository = couponRepository,
            couponUtils = couponUtils,
            appPrefs = appPrefs,
            getSelectedRange = getSelectedDateRange,
            parameterRepository = parameterRepository,
            coroutineDispatchers = coroutinesTestRule.testDispatchers,
            dateRangeFormatter = dateRangeFormatter,
            customDateRangeDataStore = customDateRangeDataStore,
            analyticsTrackerWrapper = analyticsTrackerWrapper
        )
    }

    @Test
    fun `when initiating card, then show loading`() = testBlocking {
        setup()

        val state = viewModel.viewState.captureValues().first()

        assertThat(state).isInstanceOf(DashboardCouponsViewModel.State.Loading::class.java)
    }

    @Test
    fun `given coupons loaded successfully, when initiating card, then show coupons`() = testBlocking {
        setup()

        val state = viewModel.viewState.captureValues().last()

        assertThat(state).isInstanceOf(DashboardCouponsViewModel.State.Loaded::class.java)
        assertThat((state as DashboardCouponsViewModel.State.Loaded).coupons.map { it.id })
            .isEqualTo(sampleCoupons.map { it.id })
    }

    @Test
    fun `given coupons failed to load, when initiating card, then show error`() = testBlocking {
        setup {
            whenever(couponRepository.fetchMostActiveCoupons(any(), any())) doReturn Result.failure(Exception())
        }

        val state = viewModel.viewState.captureValues().last()

        assertThat(state).isInstanceOf(DashboardCouponsViewModel.State.Error::class.java)
    }

    @Test
    fun `when loading coupons, then use in-memory cache when possible`() = testBlocking {
        setup()

        viewModel.viewState.runAndCaptureValues {
            viewModel.onTabSelected(SelectionType.WEEK_TO_DATE)
            viewModel.onTabSelected(SelectionType.TODAY)
        }

        verify(couponRepository, times(2)).fetchMostActiveCoupons(any(), any())
    }

    @Test
    fun `when force refreshing, then ignore local cache`() = testBlocking {
        val refreshTrigger = MutableSharedFlow<RefreshEvent>(extraBufferCapacity = 1)
        setup {
            whenever(parentViewModel.refreshTrigger) doReturn refreshTrigger
        }

        viewModel.viewState.runAndCaptureValues {
            refreshTrigger.tryEmit(RefreshEvent(isForced = true))
        }

        verify(couponRepository, times(2)).fetchMostActiveCoupons(any(), any())
    }

    @Test
    fun `when changing tabs, then persist selected tab`() = testBlocking {
        setup()

        viewModel.onTabSelected(SelectionType.WEEK_TO_DATE)

        assertThat(appPrefs.getActiveCouponsTab()).isEqualTo(SelectionType.WEEK_TO_DATE.name)
    }

    @Test
    fun `when setting custom date range, then update data store`() = testBlocking {
        val newRange = StatsTimeRange(Date(), Date().apply { time += 1000 })
        setup()

        viewModel.onCustomRangeSelected(newRange)

        assertThat(customDateRangeDataStore.dateRange.first()).isEqualTo(newRange)
    }

    @Test
    fun `when loading card, then show current range formatted`() = testBlocking {
        setup {
            whenever(dateRangeFormatter.formatRangeDate(any())) doReturn "Formatted date range"
        }

        val dateRangeState = viewModel.dateRangeState.getOrAwaitValue()

        assertThat(dateRangeState.rangeFormatted).isEqualTo("Formatted date range")
    }

    @Test
    fun `when tapping on a coupon, then open coupon details`() = testBlocking {
        setup()

        val event = viewModel.event.runAndCaptureValues {
            viewModel.onCouponClicked(sampleCoupons.first().id)
        }.last()

        assertThat(event).isEqualTo(DashboardCouponsViewModel.ViewCouponDetails(sampleCoupons.first().id))
    }

    @Test
    fun `when tapping view all, then open coupons list`() = testBlocking {
        setup()

        val event = viewModel.event.runAndCaptureValues {
            viewModel.onViewAllClicked()
        }.last()

        assertThat(event).isEqualTo(DashboardCouponsViewModel.ViewAllCoupons)
    }
}
