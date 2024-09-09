package com.woocommerce.android.wear.complications.ordertotals

import android.icu.text.CompactDecimalFormat
import com.woocommerce.android.BaseUnitTest
import com.woocommerce.android.wear.ui.login.LoginRepository
import com.woocommerce.android.wear.ui.stats.datasource.StatsRepository
import kotlin.test.Test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCRevenueStatsModel

@OptIn(ExperimentalCoroutinesApi::class)
class FetchStatsForComplicationsTest : BaseUnitTest() {
    private lateinit var sut: FetchStatsForComplications
    private val coroutineScope: CoroutineScope = TestScope(coroutinesTestRule.testDispatcher)
    private val statsRepository: StatsRepository = mock()
    private val loginRepository: LoginRepository = mock()
    private val decimalFormat: CompactDecimalFormat = mock()

    @Before
    fun setUp() {
        sut = FetchStatsForComplications(coroutineScope, statsRepository, loginRepository, decimalFormat)
    }

    @Test
    fun `returns formatted order totals when site is selected`() = testBlocking {
        val site = SiteModel()
        val total = mock<WCRevenueStatsModel.Total> {
            on { totalSales } doReturn 100.0
        }
        val revenueStats = mock<WCRevenueStatsModel> {
            on { parseTotal() } doReturn total
        }
        whenever(loginRepository.selectedSiteFlow).thenReturn(MutableStateFlow(site))
        whenever(statsRepository.fetchRevenueStats(site)).thenReturn(Result.success(revenueStats))
        whenever(decimalFormat.format(100.0)).thenReturn("100")

        val result = sut(FetchStatsForComplications.StatType.ORDER_TOTALS)

        assertThat(result).isEqualTo("100")
    }

    @Test
    fun `returns default value when order totals fetch fails`() = testBlocking {
        val site = SiteModel()
        whenever(loginRepository.selectedSiteFlow).thenReturn(MutableStateFlow(site))
        whenever(statsRepository.fetchRevenueStats(site)).thenReturn(Result.failure(Exception()))

        val result = sut(FetchStatsForComplications.StatType.ORDER_TOTALS)

        assertThat(result).isEqualTo(FetchStatsForComplications.DEFAULT_EMPTY_VALUE)
    }

    @Test
    fun `returns order count when site is selected`() = testBlocking {
        val site = SiteModel()
        val total = mock<WCRevenueStatsModel.Total> {
            on { ordersCount } doReturn 10
        }
        val revenueStats = mock<WCRevenueStatsModel> {
            on { parseTotal() } doReturn total
        }
        whenever(loginRepository.selectedSiteFlow).thenReturn(MutableStateFlow(site))
        whenever(statsRepository.fetchRevenueStats(site)).thenReturn(Result.success(revenueStats))

        val result = sut(FetchStatsForComplications.StatType.ORDER_COUNT)

        assertThat(result).isEqualTo("10")
    }

    @Test
    fun `returns default value when order count fetch fails`() = testBlocking {
        val site = SiteModel()
        whenever(loginRepository.selectedSiteFlow).thenReturn(MutableStateFlow(site))
        whenever(statsRepository.fetchRevenueStats(site)).thenReturn(Result.failure(Exception()))

        val result = sut(FetchStatsForComplications.StatType.ORDER_COUNT)

        assertThat(result).isEqualTo(FetchStatsForComplications.DEFAULT_EMPTY_VALUE)
    }

    @Test
    fun `returns visitors count when site is selected`() = testBlocking {
        val site = SiteModel()
        whenever(loginRepository.selectedSiteFlow).thenReturn(MutableStateFlow(site))
        whenever(statsRepository.fetchVisitorStats(site)).thenReturn(Result.success(100))

        val result = sut(FetchStatsForComplications.StatType.VISITORS)

        assertThat(result).isEqualTo("100")
    }

    @Test
    fun `returns default value when visitors count fetch fails`() = testBlocking {
        val site = SiteModel()
        whenever(loginRepository.selectedSiteFlow).thenReturn(MutableStateFlow(site))
        whenever(statsRepository.fetchVisitorStats(site)).thenReturn(Result.failure(Exception()))

        val result = sut(FetchStatsForComplications.StatType.VISITORS)

        assertThat(result).isEqualTo(FetchStatsForComplications.DEFAULT_EMPTY_VALUE)
    }
}
