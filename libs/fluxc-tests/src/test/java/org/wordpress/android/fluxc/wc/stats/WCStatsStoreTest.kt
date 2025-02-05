package org.wordpress.android.fluxc.wc.stats

import androidx.room.Room
import com.yarolegovich.wellsql.WellSql
import junit.framework.TestCase.assertFalse
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.anyOf
import org.hamcrest.CoreMatchers.not
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.SingleStoreWellSqlConfigForTests
import org.wordpress.android.fluxc.UnitTestUtils
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCNewVisitorStatsModel
import org.wordpress.android.fluxc.model.WCRevenueStatsModel
import org.wordpress.android.fluxc.model.WCVisitorStatsSummary
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bundlestats.BundleStatsApiResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bundlestats.BundleStatsRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bundlestats.BundleStatsTotals
import org.wordpress.android.fluxc.network.rest.wpcom.wc.orderstats.OrderStatsRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.orderstats.VisitorStatsSummaryApiResponse
import org.wordpress.android.fluxc.persistence.WCAndroidDatabase
import org.wordpress.android.fluxc.persistence.WCVisitorStatsSqlUtils
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import org.wordpress.android.fluxc.store.WCStatsStore
import org.wordpress.android.fluxc.store.WCStatsStore.FetchRevenueStatsPayload
import org.wordpress.android.fluxc.store.WCStatsStore.FetchRevenueStatsResponsePayload
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity
import org.wordpress.android.fluxc.tools.initCoroutineEngine
import org.wordpress.android.fluxc.utils.DateUtils
import org.wordpress.android.fluxc.utils.SiteUtils.getCurrentDateTimeForSite
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.hamcrest.CoreMatchers.`is` as isEqual
import java.util.Locale

@Suppress("LargeClass")
@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class WCStatsStoreTest {
    private val mockOrderStatsRestClient = mock<OrderStatsRestClient>()
    private val mockBundleStatsRestClient = mock<BundleStatsRestClient>()
    private val appContext = RuntimeEnvironment.application.applicationContext
    private lateinit var wcStatsStore: WCStatsStore

    @Before
    fun setUp() {
        val config = SingleStoreWellSqlConfigForTests(
                appContext,
                listOf(
                    WCRevenueStatsModel::class.java,
                    WCNewVisitorStatsModel::class.java
                ),
                WellSqlConfig.ADDON_WOOCOMMERCE
        )
        WellSql.init(config)
        config.reset()

        val database = Room.inMemoryDatabaseBuilder(appContext, WCAndroidDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        wcStatsStore = WCStatsStore(
            dispatcher = Dispatcher(),
            wcOrderStatsClient = mockOrderStatsRestClient,
            bundleStatsRestClient = mockBundleStatsRestClient,
            coroutineEngine = initCoroutineEngine(),
            visitorSummaryStatsDao = database.visitorSummaryStatsDao
        )
    }

    @Test
    fun testGetQuantityForDays() {
        val quantity1 = wcStatsStore.getVisitorStatsQuantity(
            StatsGranularity.DAYS, "2018-01-25",
            "2018-01-28"
        )
        assertEquals(4, quantity1)

        val quantity2 = wcStatsStore.getVisitorStatsQuantity(
            StatsGranularity.DAYS, "2018-01-01",
            "2018-01-01"
        )
        assertEquals(1, quantity2)

        val quantity3 = wcStatsStore.getVisitorStatsQuantity(
            StatsGranularity.DAYS, "2018-01-01",
            "2018-01-31"
        )
        assertEquals(31, quantity3)

        val quantity4 = wcStatsStore.getVisitorStatsQuantity(
            StatsGranularity.DAYS, "2018-01-28",
            "2018-01-25"
        )
        assertEquals(4, quantity4)

        val quantity5 = wcStatsStore.getVisitorStatsQuantity(
            StatsGranularity.DAYS, "2018-01-01",
            "2018-01-01"
        )
        assertEquals(1, quantity5)

        val quantity6 = wcStatsStore.getVisitorStatsQuantity(
            StatsGranularity.DAYS, "2018-01-31",
            "2018-01-01"
        )
        assertEquals(31, quantity6)
    }

    @Suppress("LongMethod")
    @Test
    fun testGetQuantityForWeeks() {
        val quantity1 = wcStatsStore.getVisitorStatsQuantity(
            StatsGranularity.WEEKS, "2018-10-22",
            "2018-10-23"
        )
        assertEquals(1, quantity1)

        val quantity2 = wcStatsStore.getVisitorStatsQuantity(
            StatsGranularity.WEEKS, "2017-01-01",
            "2018-01-01"
        )
        assertEquals(53, quantity2)

        val quantity3 = wcStatsStore.getVisitorStatsQuantity(
            StatsGranularity.WEEKS, "2019-01-20",
            "2019-01-13"
        )
        assertEquals(2, quantity3)

        val quantity4 = wcStatsStore.getVisitorStatsQuantity(
            StatsGranularity.WEEKS, "2017-01-01",
            "2018-03-01"
        )
        assertEquals(61, quantity4)

        val quantity5 = wcStatsStore.getVisitorStatsQuantity(
            StatsGranularity.WEEKS, "2018-01-01",
            "2018-01-31"
        )
        assertEquals(5, quantity5)

        val quantity6 = wcStatsStore.getVisitorStatsQuantity(
            StatsGranularity.WEEKS, "2018-12-01",
            "2018-12-31"
        )
        assertEquals(6, quantity6)

        val quantity7 = wcStatsStore.getVisitorStatsQuantity(
            StatsGranularity.WEEKS, "2018-11-01",
            "2018-11-30"
        )
        assertEquals(5, quantity7)

        val inverseQuantity1 = wcStatsStore.getVisitorStatsQuantity(
            StatsGranularity.WEEKS, "2018-10-23",
            "2018-10-22"
        )
        assertEquals(1, inverseQuantity1)

        val inverseQuantity2 = wcStatsStore.getVisitorStatsQuantity(
            StatsGranularity.WEEKS, "2018-01-01",
            "2017-01-01"
        )
        assertEquals(53, inverseQuantity2)

        val inverseQuantity3 = wcStatsStore.getVisitorStatsQuantity(
            StatsGranularity.WEEKS, "2019-01-13",
            "2019-01-20"
        )
        assertEquals(2, inverseQuantity3)

        val inverseQuantity4 = wcStatsStore.getVisitorStatsQuantity(
            StatsGranularity.WEEKS, "2018-03-01",
            "2017-01-01"
        )
        assertEquals(61, inverseQuantity4)

        val inverseQuantity5 = wcStatsStore.getVisitorStatsQuantity(
            StatsGranularity.WEEKS, "2018-01-31",
            "2018-01-01"
        )
        assertEquals(5, inverseQuantity5)

        val inverseQuantity6 = wcStatsStore.getVisitorStatsQuantity(
            StatsGranularity.WEEKS, "2018-12-31",
            "2018-12-01"
        )
        assertEquals(6, inverseQuantity6)

        val inverseQuantity7 = wcStatsStore.getVisitorStatsQuantity(
            StatsGranularity.WEEKS, "2018-11-30",
            "2018-11-01"
        )
        assertEquals(5, inverseQuantity7)
    }

    @Suppress("LongMethod")
    @Test
    fun testGetQuantityForMonths() {
        val quantity1 = wcStatsStore.getVisitorStatsQuantity(
            StatsGranularity.MONTHS, "2018-10-22",
            "2018-10-23"
        )
        assertEquals(1, quantity1)

        val quantity2 = wcStatsStore.getVisitorStatsQuantity(
            StatsGranularity.MONTHS, "2017-01-01",
            "2018-01-01"
        )
        assertEquals(13, quantity2)

        val quantity3 = wcStatsStore.getVisitorStatsQuantity(
            StatsGranularity.MONTHS, "2018-01-01",
            "2018-01-01"
        )
        assertEquals(1, quantity3)

        val quantity4 = wcStatsStore.getVisitorStatsQuantity(
            StatsGranularity.MONTHS, "2017-01-01",
            "2018-03-01"
        )
        assertEquals(15, quantity4)

        val quantity5 = wcStatsStore.getVisitorStatsQuantity(
            StatsGranularity.MONTHS, "2017-01-01",
            "2018-01-31"
        )
        assertEquals(13, quantity5)

        val quantity6 = wcStatsStore.getVisitorStatsQuantity(
            StatsGranularity.MONTHS, "2018-12-31",
            "2019-01-01"
        )
        assertEquals(2, quantity6)

        val inverseQuantity1 = wcStatsStore.getVisitorStatsQuantity(
            StatsGranularity.MONTHS, "2018-10-23",
            "2018-10-22"
        )
        assertEquals(1, inverseQuantity1)

        val inverseQuantity2 = wcStatsStore.getVisitorStatsQuantity(
            StatsGranularity.MONTHS, "2018-01-01",
            "2017-01-01"
        )
        assertEquals(13, inverseQuantity2)

        val inverseQuantity3 = wcStatsStore.getVisitorStatsQuantity(
            StatsGranularity.MONTHS, "2018-01-01",
            "2018-01-01"
        )
        assertEquals(1, inverseQuantity3)

        val inverseQuantity4 = wcStatsStore.getVisitorStatsQuantity(
            StatsGranularity.MONTHS, "2018-03-01",
            "2017-01-01"
        )
        assertEquals(15, inverseQuantity4)

        val inverseQuantity5 = wcStatsStore.getVisitorStatsQuantity(
            StatsGranularity.MONTHS, "2018-01-31",
            "2017-01-01"
        )
        assertEquals(13, inverseQuantity5)

        val inverseQuantity6 = wcStatsStore.getVisitorStatsQuantity(
            StatsGranularity.MONTHS, "2019-01-01",
            "2018-12-31"
        )
        assertEquals(2, inverseQuantity6)
    }

    @Suppress("LongMethod")
    @Test
    fun testGetQuantityForYears() {
        val quantity1 = wcStatsStore.getVisitorStatsQuantity(
            StatsGranularity.YEARS, "2017-01-01",
            "2018-01-01"
        )
        assertEquals(2, quantity1)

        val quantity2 = wcStatsStore.getVisitorStatsQuantity(
            StatsGranularity.YEARS, "2017-01-01",
            "2018-03-01"
        )
        assertEquals(2, quantity2)

        val quantity3 = wcStatsStore.getVisitorStatsQuantity(
            StatsGranularity.YEARS, "2017-01-01",
            "2018-01-05"
        )
        assertEquals(2, quantity3)

        val quantity4 = wcStatsStore.getVisitorStatsQuantity(
            StatsGranularity.YEARS, "2017-01-01",
            "2019-03-01"
        )
        assertEquals(3, quantity4)

        val quantity5 = wcStatsStore.getVisitorStatsQuantity(
            StatsGranularity.YEARS, "2015-03-05",
            "2017-01-01"
        )
        assertEquals(3, quantity5)

        val quantity6 = wcStatsStore.getVisitorStatsQuantity(
            StatsGranularity.YEARS, "2018-12-31",
            "2019-01-01"
        )
        assertEquals(2, quantity6)

        val quantity7 = wcStatsStore.getVisitorStatsQuantity(
            StatsGranularity.YEARS, "2019-01-25",
            "2019-01-25"
        )
        assertEquals(1, quantity7)

        val inverseQuantity1 = wcStatsStore.getVisitorStatsQuantity(
            StatsGranularity.YEARS, "2018-01-01",
            "2017-01-01"
        )
        assertEquals(2, inverseQuantity1)

        val inverseQuantity2 = wcStatsStore.getVisitorStatsQuantity(
            StatsGranularity.YEARS, "2018-03-01",
            "2017-01-01"
        )
        assertEquals(2, inverseQuantity2)

        val inverseQuantity3 = wcStatsStore.getVisitorStatsQuantity(
            StatsGranularity.YEARS, "2018-01-05",
            "2017-01-01"
        )
        assertEquals(2, inverseQuantity3)

        val inverseQuantity4 = wcStatsStore.getVisitorStatsQuantity(
            StatsGranularity.YEARS, "2019-03-01",
            "2017-01-01"
        )
        assertEquals(3, inverseQuantity4)

        val inverseQuantity5 = wcStatsStore.getVisitorStatsQuantity(
            StatsGranularity.YEARS, "2017-01-01",
            "2015-03-05"
        )
        assertEquals(3, inverseQuantity5)

        val inverseQuantity6 = wcStatsStore.getVisitorStatsQuantity(
            StatsGranularity.YEARS, "2019-01-01",
            "2018-12-31"
        )
        assertEquals(2, inverseQuantity6)
    }

    @Test
    fun testFetchCurrentDayRevenueStatsDate() = runBlocking {
        val plus12SiteDate = SiteModel().apply { timezone = "12" }.let {
            whenever(mockOrderStatsRestClient.fetchRevenueStats(any(), any(), any(), any(), any(), any(), any(), any())
            ).thenReturn(FetchRevenueStatsResponsePayload(it, StatsGranularity.DAYS, WCRevenueStatsModel()))
            val startDate = DateUtils.getStartDateForSite(it, DateUtils.formatDate("yyyy-MM-dd'T'00:00:00", Date()))
            val endDate = DateUtils.getEndDateForSite(it)
            val payload = FetchRevenueStatsPayload(it, StatsGranularity.DAYS, startDate, endDate)
            wcStatsStore.fetchRevenueStats(payload)

            val timeOnSite = getCurrentDateTimeForSite(it, "yyyy-MM-dd'T'00:00:00")

            // The date value passed to the network client should match the current date on the site
            val dateArgument = argumentCaptor<String>()
            verify(mockOrderStatsRestClient).fetchRevenueStats(any(), any(),
                    dateArgument.capture(), any(), any(), any(), any(), any())
            val siteDate = dateArgument.firstValue
            assertEquals(timeOnSite, siteDate)
            return@let siteDate
        }

        reset(mockOrderStatsRestClient)

        val minus12SiteDate = SiteModel().apply { timezone = "-12" }.let {
            whenever(mockOrderStatsRestClient.fetchRevenueStats(any(), any(), any(), any(), any(), any(), any(), any())
            ).thenReturn(FetchRevenueStatsResponsePayload(it, StatsGranularity.DAYS, WCRevenueStatsModel()))
            val startDate = DateUtils.getStartDateForSite(it, DateUtils.formatDate("yyyy-MM-dd'T'00:00:00", Date()))
            val endDate = DateUtils.getEndDateForSite(it)
            val payload = FetchRevenueStatsPayload(it, StatsGranularity.DAYS, startDate, endDate)
            wcStatsStore.fetchRevenueStats(payload)

            val timeOnSite = getCurrentDateTimeForSite(it, "yyyy-MM-dd'T'00:00:00")

            // The date value passed to the network client should match the current date on the site
            val dateArgument = argumentCaptor<String>()
            verify(mockOrderStatsRestClient).fetchRevenueStats(any(), any(), dateArgument.capture(), any(),
                    any(), any(), any(), any())
            val siteDate = dateArgument.firstValue
            assertEquals(timeOnSite, siteDate)
            return@let siteDate
        }

        // The two test sites are 24 hours apart, so we are guaranteed to have one site date match the local date,
        // and the other not match it
        val localDate = SimpleDateFormat("yyyy-MM-dd'T'00:00:00").format(Date())
        assertThat(localDate, anyOf(isEqual(plus12SiteDate), isEqual(minus12SiteDate)))
        assertThat(localDate, anyOf(not(plus12SiteDate), not(minus12SiteDate)))
    }

    @Test
    fun testFetchCurrentDayRevenueStatsDateSpecificEndDate() = runBlocking {
        val plus12SiteDate = SiteModel().apply { timezone = "12" }.let {
            whenever(mockOrderStatsRestClient.fetchRevenueStats(any(), any(), any(), any(), any(), any(), any(), any())
            ).thenReturn(FetchRevenueStatsResponsePayload(it, StatsGranularity.DAYS, WCRevenueStatsModel()))
            val startDate = DateUtils.getStartDateForSite(it, DateUtils.formatDate("yyyy-MM-dd'T'00:00:00", Date()))
            val endDate = DateUtils.formatDate("yyyy-MM-dd", Date())

            val payload = FetchRevenueStatsPayload(it, StatsGranularity.DAYS, startDate, endDate)
            wcStatsStore.fetchRevenueStats(payload)

            val timeOnSite = getCurrentDateTimeForSite(it, "yyyy-MM-dd'T'00:00:00")

            // The date value passed to the network client should match the current date on the site
            val dateArgument = argumentCaptor<String>()
            verify(mockOrderStatsRestClient).fetchRevenueStats(any(), any(),
                    dateArgument.capture(), any(), any(), any(), any(), any())
            val siteDate = dateArgument.firstValue
            assertEquals(timeOnSite, siteDate)
            return@let siteDate
        }

        reset(mockOrderStatsRestClient)

        val minus12SiteDate = SiteModel().apply { timezone = "-12" }.let {
            whenever(mockOrderStatsRestClient.fetchRevenueStats(any(), any(), any(), any(), any(), any(), any(), any())
            ).thenReturn(FetchRevenueStatsResponsePayload(it, StatsGranularity.DAYS, WCRevenueStatsModel()))
            val startDate = DateUtils.getStartDateForSite(it, DateUtils.formatDate("yyyy-MM-dd'T'00:00:00", Date()))
            val endDate = DateUtils.getEndDateForSite(it)
            val payload = FetchRevenueStatsPayload(it, StatsGranularity.DAYS, startDate, endDate)
            wcStatsStore.fetchRevenueStats(payload)

            val timeOnSite = getCurrentDateTimeForSite(it, "yyyy-MM-dd'T'00:00:00")

            // The date value passed to the network client should match the current date on the site
            val dateArgument = argumentCaptor<String>()
            verify(mockOrderStatsRestClient).fetchRevenueStats(any(), any(), dateArgument.capture(), any(),
                    any(), any(), any(), any())
            val siteDate = dateArgument.firstValue
            assertEquals(timeOnSite, siteDate)
            return@let siteDate
        }

        // The two test sites are 24 hours apart, so we are guaranteed to have one site date match the local date,
        // and the other not match it
        val localDate = SimpleDateFormat("yyyy-MM-dd'T'00:00:00").format(Date())
        assertThat(localDate, anyOf(isEqual(plus12SiteDate), isEqual(minus12SiteDate)))
        assertThat(localDate, anyOf(not(plus12SiteDate), not(minus12SiteDate)))
    }

    @Test
    @Suppress("LongMethod")
    fun testGetRevenueAndOrderStatsForSite() = runBlocking {
        // revenue stats model for current day
        val currentDayStatsModel = WCStatsTestUtils.generateSampleRevenueStatsModel()
        val site = SiteModel().apply { id = currentDayStatsModel.localSiteId }
        val currentDayGranularity = StatsGranularity.valueOf(
            currentDayStatsModel.interval.uppercase(
                Locale.getDefault()
            )
        )
        whenever(mockOrderStatsRestClient.fetchRevenueStats(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(
                        FetchRevenueStatsResponsePayload(
                                site,
                                currentDayGranularity,
                                currentDayStatsModel
                        )
                )
        wcStatsStore.fetchRevenueStats(
                FetchRevenueStatsPayload(
                        site,
                        currentDayGranularity,
                        currentDayStatsModel.startDate,
                        currentDayStatsModel.endDate
                )
        )

        // verify that the revenue stats & order count is not empty
        val currentDayRevenueStats = wcStatsStore.getGrossRevenueStats(
                site, StatsGranularity.valueOf(currentDayStatsModel.interval.uppercase(Locale.getDefault())),
                currentDayStatsModel.startDate, currentDayStatsModel.endDate
        )
        val currentDayOrderStats = wcStatsStore.getOrderCountStats(
                site, StatsGranularity.valueOf(currentDayStatsModel.interval.uppercase(Locale.getDefault())),
                currentDayStatsModel.startDate, currentDayStatsModel.endDate
        )

        assertTrue(currentDayRevenueStats.isNotEmpty())
        assertTrue(currentDayOrderStats.isNotEmpty())

        // revenue stats model for this week
        val currentWeekStatsModel =
                WCStatsTestUtils.generateSampleRevenueStatsModel(
                        interval = StatsGranularity.WEEKS.toString(), startDate = "2019-07-07", endDate = "2019-07-09"
                )
        val curretnWeekGranularity = StatsGranularity.valueOf(
            currentWeekStatsModel.interval.uppercase(
                Locale.getDefault()
            )
        )
        val currentWeekPayload = FetchRevenueStatsResponsePayload(
                site, curretnWeekGranularity, currentWeekStatsModel
        )
        whenever(mockOrderStatsRestClient.fetchRevenueStats(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(currentWeekPayload)
        wcStatsStore.fetchRevenueStats(
                FetchRevenueStatsPayload(
                        site,
                        curretnWeekGranularity,
                        currentWeekStatsModel.startDate,
                        currentWeekStatsModel.endDate
                )
        )

        // verify that the revenue stats & order count is not empty
        val currentWeekRevenueStats = wcStatsStore.getGrossRevenueStats(
                site, StatsGranularity.valueOf(currentWeekStatsModel.interval.uppercase(Locale.getDefault())),
                currentWeekStatsModel.startDate, currentWeekStatsModel.endDate
        )
        val currentWeekOrderStats = wcStatsStore.getOrderCountStats(
                site, StatsGranularity.valueOf(currentWeekStatsModel.interval.uppercase(Locale.getDefault())),
                currentWeekStatsModel.startDate, currentWeekStatsModel.endDate
        )

        assertTrue(currentWeekRevenueStats.isNotEmpty())
        assertTrue(currentWeekOrderStats.isNotEmpty())

        // revenue stats model for this month
        val currentMonthStatsModel =
                WCStatsTestUtils.generateSampleRevenueStatsModel(
                        interval = StatsGranularity.MONTHS.toString(), startDate = "2019-07-01", endDate = "2019-07-09"
                )
        val currentMonthGranularity = StatsGranularity.valueOf(
            currentMonthStatsModel.interval.uppercase(
                Locale.getDefault()
            )
        )
        val currentMonthPayload = FetchRevenueStatsResponsePayload(
                site, currentMonthGranularity, currentMonthStatsModel
        )
        whenever(mockOrderStatsRestClient.fetchRevenueStats(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(currentMonthPayload)
        wcStatsStore.fetchRevenueStats(
                FetchRevenueStatsPayload(
                        site,
                        currentMonthGranularity,
                        currentMonthStatsModel.startDate,
                        currentMonthStatsModel.endDate
                )
        )

        // verify that the revenue stats & order count is not empty
        val currentMonthRevenueStats = wcStatsStore.getGrossRevenueStats(
                site, StatsGranularity.valueOf(currentMonthStatsModel.interval.uppercase(Locale.getDefault())),
                currentMonthStatsModel.startDate, currentMonthStatsModel.endDate
        )
        val currentMonthOrderStats = wcStatsStore.getOrderCountStats(
                site, StatsGranularity.valueOf(currentMonthStatsModel.interval.uppercase(Locale.getDefault())),
                currentMonthStatsModel.startDate, currentMonthStatsModel.endDate
        )

        assertTrue(currentMonthRevenueStats.isNotEmpty())
        assertTrue(currentMonthOrderStats.isNotEmpty())

        // current day stats for alternate site
        val site2 = SiteModel().apply { id = 8 }
        val altSiteOrderStatsModel = WCStatsTestUtils.generateSampleRevenueStatsModel(
                localSiteId = site2.id, interval = StatsGranularity.DAYS.toString()
        )
        val allSiteCurrentDayGranularity = StatsGranularity.valueOf(
            altSiteOrderStatsModel.interval.uppercase(
                Locale.getDefault()
            )
        )
        val allSiteCurrentDayPayload = FetchRevenueStatsResponsePayload(
                site, allSiteCurrentDayGranularity, altSiteOrderStatsModel
        )
        whenever(mockOrderStatsRestClient.fetchRevenueStats(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(allSiteCurrentDayPayload)
        wcStatsStore.fetchRevenueStats(
                FetchRevenueStatsPayload(
                        site,
                        allSiteCurrentDayGranularity,
                        altSiteOrderStatsModel.startDate,
                        altSiteOrderStatsModel.endDate
                )
        )

        // verify that the revenue stats & order count is not empty
        val altSiteCurrentDayRevenueStats = wcStatsStore.getGrossRevenueStats(
                site, StatsGranularity.valueOf(altSiteOrderStatsModel.interval.uppercase(Locale.getDefault())),
                altSiteOrderStatsModel.startDate, altSiteOrderStatsModel.endDate
        )
        val altSiteCurrentDayOrderStats = wcStatsStore.getOrderCountStats(
                site, StatsGranularity.valueOf(altSiteOrderStatsModel.interval.uppercase(Locale.getDefault())),
                altSiteOrderStatsModel.startDate, altSiteOrderStatsModel.endDate
        )

        assertTrue(altSiteCurrentDayRevenueStats.isNotEmpty())
        assertTrue(altSiteCurrentDayOrderStats.isNotEmpty())

        // non existentSite
        val nonExistentSite = SiteModel().apply { id = 88 }
        val nonExistentSiteGranularity = StatsGranularity.valueOf(altSiteOrderStatsModel.interval)
        val nonExistentPayload = FetchRevenueStatsResponsePayload(
                nonExistentSite, nonExistentSiteGranularity, altSiteOrderStatsModel
        )
        whenever(mockOrderStatsRestClient.fetchRevenueStats(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(nonExistentPayload)
        wcStatsStore.fetchRevenueStats(
                FetchRevenueStatsPayload(
                        site,
                        nonExistentSiteGranularity,
                        altSiteOrderStatsModel.startDate,
                        altSiteOrderStatsModel.endDate
                )
        )

        // verify that the revenue stats & order count is empty
        val nonExistentRevenueStats = wcStatsStore.getGrossRevenueStats(
                nonExistentSite, StatsGranularity.valueOf(
                altSiteOrderStatsModel.interval.uppercase(Locale.getDefault())
            ),
                altSiteOrderStatsModel.startDate, altSiteOrderStatsModel.endDate
        )
        val nonExistentOrderStats = wcStatsStore.getOrderCountStats(
                nonExistentSite, StatsGranularity.valueOf(
                altSiteOrderStatsModel.interval.uppercase(Locale.getDefault())
            ),
                altSiteOrderStatsModel.startDate, altSiteOrderStatsModel.endDate
        )

        assertTrue(nonExistentRevenueStats.isEmpty())
        assertTrue(nonExistentOrderStats.isEmpty())

        // missing data
        whenever(mockOrderStatsRestClient.fetchRevenueStats(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(nonExistentPayload)
        wcStatsStore.fetchRevenueStats(
                FetchRevenueStatsPayload(
                    site = site,
                    granularity = StatsGranularity.YEARS,
                    startDate = "2019-01-01",
                    endDate = "2019-01-07"
                )
        )

        // verify that the revenue stats & order count is empty
        val missingRevenueStats = wcStatsStore.getGrossRevenueStats(
                site, StatsGranularity.YEARS, "2019-01-01", "2019-01-07"
        )
        val missingOrderStats = wcStatsStore.getOrderCountStats(
                site, StatsGranularity.YEARS, "2019-01-01", "2019-01-07"
        )
        assertTrue(missingRevenueStats.isEmpty())
        assertTrue(missingOrderStats.isEmpty())
    }

    @Test
    @Suppress("LongMethod")
    fun testGetVisitorStatsForCurrentDayGranularity() {
        // Test Scenario - 1: Generate default visitor stats i.e. isCustomField - false
        // Get visitor Stats of the same site and granularity and assert not null
        val defaultDayVisitorStatsModel = WCStatsTestUtils.generateSampleNewVisitorStatsModel()
        val site = SiteModel().apply { id = defaultDayVisitorStatsModel.localSiteId }
        WCVisitorStatsSqlUtils.insertOrUpdateNewVisitorStats(defaultDayVisitorStatsModel)

        val defaultDayVisitorStats = wcStatsStore.getNewVisitorStats(site, StatsGranularity.DAYS)
        assertTrue(defaultDayVisitorStats.isNotEmpty())

        // Test Scenario - 2: Generate default visitor stats with a different date
        // Get visitor of the same site and granularity and assert not null
        val defaultDayVisitorStatsModel2 = WCStatsTestUtils.generateSampleNewVisitorStatsModel(
                endDate = "2019-08-02"
        )
        WCVisitorStatsSqlUtils.insertOrUpdateNewVisitorStats(defaultDayVisitorStatsModel2)
        val defaultDayVisitorStats2 = wcStatsStore.getNewVisitorStats(site, StatsGranularity.DAYS)
        assertTrue(defaultDayVisitorStats2.isNotEmpty())

        // Test Scenario - 3: Generate custom stats for same site i.e. isCustomField - true
        // Get visitor Stats of the same site and granularity and assert not null
        val customDayVisitorStatsModel = WCStatsTestUtils.generateSampleNewVisitorStatsModel(
                quantity = "1", endDate = "2019-08-06", startDate = "2019-08-06"
        )
        WCVisitorStatsSqlUtils.insertOrUpdateNewVisitorStats(customDayVisitorStatsModel)

        val customDayVisitorStats = wcStatsStore.getNewVisitorStats(
                site, StatsGranularity.DAYS, customDayVisitorStatsModel.quantity,
                customDayVisitorStatsModel.endDate, customDayVisitorStatsModel.isCustomField
        )
        assertTrue(customDayVisitorStats.isNotEmpty())

        // Test Scenario - 4: Query for custom visitor stats that is not present in local cache:
        // for same site, same quantity, different date
        // Get visitor Stats of the same site and granularity and assert null
        val customDayVisitorStatsModel2 = WCStatsTestUtils.generateSampleNewVisitorStatsModel(
                quantity = "1", endDate = "2019-01-01", startDate = "2019-01-01"
        )
        val customDayVisitorStats2 = wcStatsStore.getNewVisitorStats(
                site, StatsGranularity.DAYS, customDayVisitorStatsModel2.quantity,
                customDayVisitorStatsModel2.endDate, customDayVisitorStatsModel2.isCustomField
        )
        assertTrue(customDayVisitorStats2.isEmpty())

        // Test Scenario - 5: Query for custom visitor stats that is not present in local cache:
        // for same site, different quantity, different date
        // Get visitor Stats of the same site and granularity and assert null
        val customDayVisitorStatsModel3 = WCStatsTestUtils.generateSampleNewVisitorStatsModel(
                quantity = "1", endDate = "2019-01-01", startDate = "2019-01-01"
        )

        val customDayVisitorStats3 = wcStatsStore.getNewVisitorStats(
                site, StatsGranularity.DAYS, customDayVisitorStatsModel3.quantity,
                customDayVisitorStatsModel3.endDate, customDayVisitorStatsModel3.isCustomField
        )
        assertTrue(customDayVisitorStats3.isEmpty())

        // Test Scenario - 6: Generate custom visitor stats for same site with different granularity (WEEKS),
        // same date(2019-01-01), same quantity (1) i.e. isCustomField - true
        // Get visitor Stats and assert Not Null
        // Now if another query ran for granularity - DAYS, with same date and same quantity: assert null
        val customWeekVisitorStatsModel = WCStatsTestUtils.generateSampleNewVisitorStatsModel(
                quantity = "1",
                endDate = "2019-02-01", startDate = "2019-02-01",
                granularity = StatsGranularity.WEEKS.toString()
        )

        WCVisitorStatsSqlUtils.insertOrUpdateNewVisitorStats(customWeekVisitorStatsModel)

        val customWeekVisitorStats = wcStatsStore.getNewVisitorStats(
                site, StatsGranularity.WEEKS, customWeekVisitorStatsModel.quantity,
                customWeekVisitorStatsModel.endDate, customWeekVisitorStatsModel.isCustomField
        )
        assertTrue(customWeekVisitorStats.isNotEmpty())

        val customDayVisitorStats4 = wcStatsStore.getNewVisitorStats(
                site, StatsGranularity.DAYS, customDayVisitorStatsModel.quantity,
                customDayVisitorStatsModel.endDate, customDayVisitorStatsModel.isCustomField
        )
        assertTrue(customDayVisitorStats4.isEmpty())

        // Test Scenario - 7: Generate custom stats for different site(8) with same granularity(WEEKS),
        // same date(2019-01-01), same quantity(1) i.e. isCustomField - true
        // Get visitor Stats and assert Not Null
        // Now if scenario 4 is run again it should assert NOT NULL, since the stats is for different sites
        val customWeekVisitorStatsModel2 = WCStatsTestUtils.generateSampleNewVisitorStatsModel(
                localSiteId = 8, granularity = StatsGranularity.WEEKS.toString(),
                quantity = "1", endDate = "2019-02-01", startDate = "2019-02-01"
        )

        WCVisitorStatsSqlUtils.insertOrUpdateNewVisitorStats(customWeekVisitorStatsModel2)

        val customWeekVisitorStats2 = wcStatsStore.getNewVisitorStats(
                site, StatsGranularity.WEEKS, customWeekVisitorStatsModel2.quantity,
                customWeekVisitorStatsModel2.endDate, customWeekVisitorStatsModel2.isCustomField
        )
        assertTrue(customWeekVisitorStats2.isNotEmpty())
        assertTrue(customWeekVisitorStats.isNotEmpty())
    }

    @Test
    @Suppress("LongMethod")
    fun testGetVisitorStatsForThisWeekGranularity() {
        // Test Scenario - 1: Generate default visitor stats i.e. isCustomField - false
        // Get visitor Stats of the same site and granularity and assert not null
        val defaultWeekVisitorStatsModel = WCStatsTestUtils.generateSampleNewVisitorStatsModel(
                granularity = StatsGranularity.WEEKS.toString()
        )
        val site = SiteModel().apply { id = defaultWeekVisitorStatsModel.localSiteId }
        WCVisitorStatsSqlUtils.insertOrUpdateNewVisitorStats(defaultWeekVisitorStatsModel)

        val defaultWeekVisitorStats = wcStatsStore.getNewVisitorStats(site, StatsGranularity.WEEKS)
        assertTrue(defaultWeekVisitorStats.isNotEmpty())

        // query for days granularity. the visitor stats should be empty
        val defaultDayVisitorStats = wcStatsStore.getNewVisitorStats(site, StatsGranularity.DAYS)
        assertTrue(defaultDayVisitorStats.isEmpty())

        // Test Scenario - 2: Generate default visitor stats with a different date
        // Get visitor of the same site and granularity and assert not null
        val defaultWeekVisitorStatsModel2 = WCStatsTestUtils.generateSampleNewVisitorStatsModel(
                granularity = StatsGranularity.WEEKS.toString(), endDate = "2019-03-20"
        )
        WCVisitorStatsSqlUtils.insertOrUpdateNewVisitorStats(defaultWeekVisitorStatsModel2)
        val defaultWeekVisitorStats2 = wcStatsStore.getNewVisitorStats(site, StatsGranularity.WEEKS)
        assertTrue(defaultWeekVisitorStats2.isNotEmpty())

        // Test Scenario - 3: Generate custom stats for same site i.e. isCustomField - true
        // Get visitor Stats of the same site and granularity and assert not null
        val customWeekVisitorStatsModel = WCStatsTestUtils.generateSampleNewVisitorStatsModel(
                granularity = StatsGranularity.WEEKS.toString(), quantity = "1",
                endDate = "2019-08-01", startDate = "2019-08-01"
        )
        WCVisitorStatsSqlUtils.insertOrUpdateNewVisitorStats(customWeekVisitorStatsModel)

        val customWeekVisitorStats = wcStatsStore.getNewVisitorStats(
                site, StatsGranularity.WEEKS, customWeekVisitorStatsModel.quantity,
                customWeekVisitorStatsModel.endDate, customWeekVisitorStatsModel.isCustomField
        )
        assertTrue(customWeekVisitorStats.isNotEmpty())

        // Test Scenario - 4: Query for custom visitor stats that is not present in local cache:
        // for same site, same quantity, different date
        // Get visitor Stats of the same site and granularity and assert null
        val customWeekVisitorStatsModel2 = WCStatsTestUtils.generateSampleNewVisitorStatsModel(
                granularity = StatsGranularity.WEEKS.toString(), quantity = "1",
                endDate = "2019-07-01", startDate = "2019-07-01"
        )
        val customWeekVisitorStats2 = wcStatsStore.getNewVisitorStats(
                site, StatsGranularity.WEEKS, customWeekVisitorStatsModel2.quantity,
                customWeekVisitorStatsModel2.endDate, customWeekVisitorStatsModel2.isCustomField
        )
        assertTrue(customWeekVisitorStats2.isEmpty())

        // Test Scenario - 5: Query for custom visitor stats that is not present in local cache:
        // for same site, different quantity, different date
        // Get visitor Stats of the same site and granularity and assert null
        val customWeekVisitorStatsModel3 = WCStatsTestUtils.generateSampleNewVisitorStatsModel(
                granularity = StatsGranularity.WEEKS.toString(), quantity = "1",
                endDate = "2019-07-01", startDate = "2019-07-01"
        )

        val customWeekVisitorStats3 = wcStatsStore.getNewVisitorStats(
                site, StatsGranularity.WEEKS, customWeekVisitorStatsModel3.quantity,
                customWeekVisitorStatsModel3.endDate, customWeekVisitorStatsModel3.isCustomField
        )
        assertTrue(customWeekVisitorStats3.isEmpty())

        // Test Scenario - 6: Generate custom visitor stats for same site with different granularity (MONTHS),
        // same date(2019-01-01), same quantity (1) i.e. isCustomField - true
        // Get visitor Stats and assert Not Null
        // Now if another query ran for granularity - WEEKS, with same date and same quantity: assert null
        val customMonthVisitorStatsModel = WCStatsTestUtils.generateSampleNewVisitorStatsModel(
                quantity = "1", endDate = "2019-08-01", startDate = "2019-08-01",
                granularity = StatsGranularity.MONTHS.toString())

        WCVisitorStatsSqlUtils.insertOrUpdateNewVisitorStats(customMonthVisitorStatsModel)

        val customMonthVisitorStats = wcStatsStore.getNewVisitorStats(
                site, StatsGranularity.MONTHS, customMonthVisitorStatsModel.quantity,
                customMonthVisitorStatsModel.endDate, customMonthVisitorStatsModel.isCustomField
        )
        assertTrue(customMonthVisitorStats.isNotEmpty())

        val customWeekVisitorStats4 = wcStatsStore.getNewVisitorStats(
                site, StatsGranularity.WEEKS, customWeekVisitorStatsModel.quantity,
                customWeekVisitorStatsModel.endDate, customWeekVisitorStatsModel.isCustomField
        )
        assertTrue(customWeekVisitorStats4.isEmpty())

        // Test Scenario - 7: Generate custom stats for different site(8) with same granularity(MONTHS),
        // same date(2019-01-01), same quantity(1) i.e. isCustomField - true
        // Get visitor Stats and assert Not Null
        // Now if scenario 4 is run again it should assert NOT NULL, since the stats is for different sites
        val customMonthVisitorStatsModel2 = WCStatsTestUtils.generateSampleNewVisitorStatsModel(
                localSiteId = 8, granularity = StatsGranularity.MONTHS.toString(),
                quantity = "1", endDate = "2019-08-01", startDate = "2019-08-01"
        )

        WCVisitorStatsSqlUtils.insertOrUpdateNewVisitorStats(customMonthVisitorStatsModel2)

        val customMonthVisitorStats2 = wcStatsStore.getNewVisitorStats(
                site, StatsGranularity.MONTHS, customMonthVisitorStatsModel2.quantity,
                customMonthVisitorStatsModel2.endDate, customMonthVisitorStatsModel2.isCustomField
        )
        assertTrue(customMonthVisitorStats2.isNotEmpty())
        assertTrue(customMonthVisitorStats.isNotEmpty())
    }

    @Test
    fun testGetNewVisitorStatsWithInvalidData() {
        // wrong-visitor-stats-data.json includes different wrong formatted data to ensure
        // that getNewVisitorStats is resilient and can recover from unexpected data
        //
        val defaultWeekVisitorStatsModel = WCStatsTestUtils.generateSampleNewVisitorStatsModel(
            granularity = StatsGranularity.WEEKS.toString(),
            data = UnitTestUtils.getStringFromResourceFile(this.javaClass, "wc/wrong-visitor-stats-data.json")
        )
        val site = SiteModel().apply { id = defaultWeekVisitorStatsModel.localSiteId }
        WCVisitorStatsSqlUtils.insertOrUpdateNewVisitorStats(defaultWeekVisitorStatsModel)

        val defaultWeekVisitorStats = wcStatsStore.getNewVisitorStats(site, StatsGranularity.WEEKS)
        assertTrue(defaultWeekVisitorStats.isNotEmpty())
        assertEquals(defaultWeekVisitorStats["2019-06-23"],10)
        assertEquals(defaultWeekVisitorStats["2019-06-22"],20)
        assertEquals(defaultWeekVisitorStats["2019-07-16"],0)
        assertEquals(defaultWeekVisitorStats["2019-07-17"],0)
        assertEquals(defaultWeekVisitorStats["2019-07-18"],0)
    }

    @Test
    fun testFetchBundlesErrorResponse() = runBlocking {
        val error = WooError(
            type = WooErrorType.INVALID_RESPONSE,
            original = GenericErrorType.INVALID_RESPONSE,
            message = "Invalid Response"
        )
        val response: WooPayload<BundleStatsApiResponse> = WooPayload(error)

        whenever(mockBundleStatsRestClient.fetchBundleStats(any(), any(), any(), any()))
            .thenReturn(response)

        val result = wcStatsStore.fetchProductBundlesStats(
            SiteModel(),
            "2024-03-01",
            endDate = "2024-04-01",
            interval = "day"
        )

        assertTrue(result.isError)
        assertTrue(result.model == null)
        assertThat(result.error, isEqual(error))
    }

    @Test
    fun testFetchBundlesNullResponse() = runBlocking {
        val response: WooPayload<BundleStatsApiResponse> = WooPayload(null)

        whenever(mockBundleStatsRestClient.fetchBundleStats(any(), any(), any(), any()))
            .thenReturn(response)

        val result = wcStatsStore.fetchProductBundlesStats(
            SiteModel(),
            "2024-03-01",
            endDate = "2024-04-01",
            interval = "day"
        )

        assertTrue(result.isError)
        assertTrue(result.model == null)
        assertThat(result.error.type, isEqual(WooErrorType.GENERIC_ERROR))
    }

    @Test
    fun testFetchBundlesSuccessResponse() = runBlocking {
        val totals = BundleStatsTotals(
            itemsSold = 5,
            netRevenue = 1000.00
        )
        val statsResponse = BundleStatsApiResponse(totals = totals)
        val response: WooPayload<BundleStatsApiResponse> = WooPayload(statsResponse)

        whenever(mockBundleStatsRestClient.fetchBundleStats(any(), any(), any(), any()))
            .thenReturn(response)

        val result = wcStatsStore.fetchProductBundlesStats(
            SiteModel(),
            "2024-03-01",
            endDate = "2024-04-01",
            interval = "day"
        )

        assertFalse(result.isError)
        assertTrue(result.model != null)
        assertEquals(result.model!!.itemsSold, totals.itemsSold)
        assertEquals(result.model!!.netRevenue, totals.netRevenue)
    }

    @Test
    fun testSuccessfulFetchingVisitorSummaryStats() = runBlocking {
        val site = SiteModel().apply { id = 0 }
        val apiResponse = VisitorStatsSummaryApiResponse(
            date = "2024-03-01",
            period = "day",
            views = 3,
            visitors = 2
        )
        whenever(
            mockOrderStatsRestClient.fetchVisitorStatsSummary(
                site = site,
                granularity = StatsGranularity.DAYS,
                date = "2024-03-01",
                force = false
            )
        ).thenReturn(WooPayload(apiResponse))

        val result = wcStatsStore.fetchVisitorStatsSummary(
            site = site,
            granularity = StatsGranularity.DAYS,
            date = "2024-03-01"
        )

        assertEquals(false, result.isError)
        assertEquals(WCVisitorStatsSummary(StatsGranularity.DAYS, "2024-03-01", 3, 2), result.model)
        assertEquals(
            WCVisitorStatsSummary(StatsGranularity.DAYS, "2024-03-01", 3, 2),
            wcStatsStore.getVisitorStatsSummary(site, StatsGranularity.DAYS, "2024-03-01")
        )
    }

    @Test
    fun testFailedFetchingVisitorSummaryStats() = runBlocking {
        val site = SiteModel().apply { id = 0 }
        whenever(
            mockOrderStatsRestClient.fetchVisitorStatsSummary(
                site = site,
                granularity = StatsGranularity.DAYS,
                date = "2024-03-01",
                force = false
            )
        ).thenReturn(WooPayload(WooError(GENERIC_ERROR, GenericErrorType.UNKNOWN)))

        val result = wcStatsStore.fetchVisitorStatsSummary(
            site = site,
            granularity = StatsGranularity.DAYS,
            date = "2024-03-01"
        )

        assertEquals(true, result.isError)
    }
}
