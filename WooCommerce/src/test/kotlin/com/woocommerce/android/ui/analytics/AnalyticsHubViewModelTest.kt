package com.woocommerce.android.ui.analytics

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.AnalyticCardConfiguration
import com.woocommerce.android.model.AnalyticsCards
import com.woocommerce.android.model.DeltaPercentage
import com.woocommerce.android.model.DeltaPercentage.NotExist
import com.woocommerce.android.model.FeatureFeedbackSettings
import com.woocommerce.android.model.OrdersStat
import com.woocommerce.android.model.ProductItem
import com.woocommerce.android.model.ProductsStat
import com.woocommerce.android.model.RevenueStat
import com.woocommerce.android.model.SessionStat
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.analytics.hub.AnalyticsHubCardViewState
import com.woocommerce.android.ui.analytics.hub.AnalyticsHubFragmentArgs
import com.woocommerce.android.ui.analytics.hub.AnalyticsHubInformationViewState
import com.woocommerce.android.ui.analytics.hub.AnalyticsHubListViewState
import com.woocommerce.android.ui.analytics.hub.AnalyticsHubTransactionLauncher
import com.woocommerce.android.ui.analytics.hub.AnalyticsHubViewModel
import com.woocommerce.android.ui.analytics.hub.AnalyticsViewEvent
import com.woocommerce.android.ui.analytics.hub.AnalyticsViewState
import com.woocommerce.android.ui.analytics.hub.GetReportUrl
import com.woocommerce.android.ui.analytics.hub.ObserveAnalyticsCardsConfiguration
import com.woocommerce.android.ui.analytics.hub.RefreshIndicator
import com.woocommerce.android.ui.analytics.hub.RefreshIndicator.NotShowIndicator
import com.woocommerce.android.ui.analytics.hub.ReportCard
import com.woocommerce.android.ui.analytics.hub.informationcard.AnalyticsHubInformationSectionViewState
import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsHubUpdateState
import com.woocommerce.android.ui.analytics.hub.sync.OrdersState
import com.woocommerce.android.ui.analytics.hub.sync.ProductsState
import com.woocommerce.android.ui.analytics.hub.sync.RevenueState
import com.woocommerce.android.ui.analytics.hub.sync.SessionState
import com.woocommerce.android.ui.analytics.hub.sync.UpdateAnalyticsHubStats
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType.CUSTOM
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType.LAST_YEAR
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType.TODAY
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType.WEEK_TO_DATE
import com.woocommerce.android.ui.dashboard.DashboardStatsUsageTracksEventEmitter
import com.woocommerce.android.ui.dashboard.domain.ObserveLastUpdate
import com.woocommerce.android.ui.feedback.FeedbackRepository
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.util.locale.LocaleProvider
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doReturnConsecutively
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class AnalyticsHubViewModelTest : BaseUnitTest() {
    private val currencyFormatter: CurrencyFormatter = mock {
        on { formatCurrency(TOTAL_VALUE.toString(), CURRENCY_CODE) } doReturn TOTAL_CURRENCY_VALUE
        on { formatCurrency(NET_VALUE.toString(), CURRENCY_CODE) } doReturn NET_CURRENCY_VALUE
        on { formatCurrency(OTHER_TOTAL_VALUE.toString(), OTHER_CURRENCY_CODE) } doReturn OTHER_TOTAL_CURRENCY_VALUE
        on { formatCurrency(OTHER_NET_VALUE.toString(), OTHER_CURRENCY_CODE) } doReturn OTHER_NET_CURRENCY_VALUE
        on { formatCurrency(AVG_ORDER_VALUE.toString(), CURRENCY_CODE) } doReturn AVG_CURRENCY_VALUE
        on { formatCurrency(OTHER_AVG_ORDER_VALUE.toString(), OTHER_CURRENCY_CODE) } doReturn OTHER_AVG_CURRENCY_VALUE
        on { formatCurrency(PRODUCT_NET_SALES.toString(), CURRENCY_CODE) } doReturn PRODUCT_CURRENCY_VALUE
        on { formatCurrency(OTHER_PRODUCT_NET_SALES.toString(), OTHER_CURRENCY_CODE) } doReturn
            OTHER_PRODUCT_CURRENCY_VALUE
    }

    private val updateStats: UpdateAnalyticsHubStats = mock()
    private val observeLastUpdate: ObserveLastUpdate = mock()
    private lateinit var savedState: SavedStateHandle
    private val transactionLauncher = mock<AnalyticsHubTransactionLauncher>()
    private val feedbackRepository: FeedbackRepository = mock()
    private val tracker: AnalyticsTrackerWrapper = mock()
    private val dateUtils: DateUtils = mock()
    private val trackerEventEmitter: DashboardStatsUsageTracksEventEmitter = mock()
    private val observeAnalyticsCardsConfiguration: ObserveAnalyticsCardsConfiguration = mock()

    private lateinit var localeProvider: LocaleProvider
    private lateinit var testLocale: Locale
    private lateinit var testCalendar: Calendar

    private val selectedSite: SelectedSite = mock()
    private val getReportUrl: GetReportUrl = GetReportUrl(selectedSite)

    private val defaultCardsConfiguration = AnalyticsCards.entries.map { card ->
        AnalyticCardConfiguration(
            card = card,
            title = card.name,
            isVisible = true
        )
    }

    private lateinit var sut: AnalyticsHubViewModel

    @Before
    fun setUp() {
        testLocale = Locale.UK
        val testTimeZone = TimeZone.getDefault()
        testCalendar = Calendar.getInstance(testLocale)
        testCalendar.timeZone = testTimeZone
        testCalendar.firstDayOfWeek = Calendar.MONDAY
        localeProvider = mock {
            on { provideLocale() } doReturn testLocale
        }
        savedState = AnalyticsHubFragmentArgs(
            TODAY.generateSelectionData(
                calendar = testCalendar,
                locale = testLocale,
                referenceStartDate = Date(),
                referenceEndDate = Date()
            )
        ).toSavedStateHandle()
    }

    @Test
    fun `given an init viewState, when view model is created, then has the expected values`() =
        testBlocking {
            sut = givenAViewModel()

            val expectedSelection = TODAY.generateSelectionData(
                calendar = testCalendar,
                locale = testLocale,
                referenceStartDate = Date(),
                referenceEndDate = Date()
            )

            with(sut.viewState.value.analyticsDateRangeSelectorState) {
                assertEquals(expectedSelection.selectionType, selectionType)
                assertEquals(expectedSelection.currentRangeDescription, currentRange)
                assertEquals(expectedSelection.previousRangeDescription, previousRange)
            }

            with(sut.viewState.value.cards) {
                assertTrue(this is AnalyticsHubCardViewState.LoadingCardsConfiguration)
            }

            with(sut.viewState.value.refreshIndicator) {
                assertTrue(this is NotShowIndicator)
            }

            with(sut.viewState.value.showFeedBackBanner) {
                assertFalse(this)
            }
        }

    @Test
    fun `when ViewModel is with savedState is created, then has the expected values`() =
        testBlocking {
            configureVisibleCards()
            configureSuccessfulStatsResponse()

            val resourceProvider: ResourceProvider = mock {
                on { getString(any()) } doReturn TODAY.name
            }

            sut = givenAViewModel(resourceProvider)

            val expectedSelection = TODAY.generateSelectionData(
                calendar = testCalendar,
                locale = testLocale,
                referenceStartDate = Date(),
                referenceEndDate = Date()
            )

            with(sut.viewState.value.analyticsDateRangeSelectorState) {
                assertEquals(expectedSelection.selectionType, selectionType)
                assertEquals(expectedSelection.currentRangeDescription, currentRange)
                assertEquals(expectedSelection.previousRangeDescription, previousRange)
            }
        }

    @Test
    fun `given a view model, when selected date range changes, then has the expected date range selector values`() =
        testBlocking {
            configureVisibleCards()
            configureSuccessfulStatsResponse()

            val resourceProvider: ResourceProvider = mock {
                on { getString(any()) } doReturnConsecutively
                    listOf(ANY_VALUE, LAST_YEAR.name)
            }

            sut = givenAViewModel(resourceProvider)
            sut.onNewRangeSelection(LAST_YEAR)

            val expectedSelection = LAST_YEAR.generateSelectionData(
                calendar = testCalendar,
                locale = testLocale,
                referenceStartDate = Date(),
                referenceEndDate = Date()
            )

            with(sut.viewState.value.analyticsDateRangeSelectorState) {
                assertEquals(expectedSelection.selectionType, selectionType)
                assertEquals(expectedSelection.currentRangeDescription, currentRange)
                assertEquals(expectedSelection.previousRangeDescription, previousRange)
            }
        }

    @Test
    fun `given a view model, when selected date range changes, then has expected revenue values`() =
        testBlocking {
            configureSuccessfulStatsResponse()
            configureVisibleCards()

            sut = givenAViewModel()
            sut.onNewRangeSelection(LAST_YEAR)

            val resourceProvider = givenAResourceProvider()
            val state = sut.viewState.value.cards
            assertTrue(state is AnalyticsHubCardViewState.CardsState)
            val revenueCardState = state.cardsState.find { it.card == AnalyticsCards.Revenue }
            with(revenueCardState) {
                assertTrue(this is AnalyticsHubInformationViewState.DataViewState)
                assertEquals(resourceProvider.getString(R.string.analytics_revenue_card_title), title)
                assertEquals(resourceProvider.getString(R.string.analytics_total_sales_title), leftSection.title)
                assertEquals(TOTAL_CURRENCY_VALUE, leftSection.value)
                assertEquals(resourceProvider.getString(R.string.analytics_net_sales_title), rightSection.title)
                assertEquals(TOTAL_DELTA.toInt(), leftSection.delta)
                assertEquals(NET_CURRENCY_VALUE, rightSection.value)
                assertEquals(NET_DELTA.toInt(), rightSection.delta)
            }
        }

    @Test
    fun `given a view model with on existent delta then delta is not shown`() =
        testBlocking {
            configureVisibleCards()
            updateStats.stub {
                onBlocking { revenueState } doReturn flow {
                    emit(RevenueState.Available(getRevenueStats(netDelta = NotExist, totalDelta = NotExist)))
                }
            }

            sut = givenAViewModel()
            sut.onNewRangeSelection(LAST_YEAR)

            val state = sut.viewState.value.cards
            assertTrue(state is AnalyticsHubCardViewState.CardsState)
            val revenueCardState = state.cardsState.find { it.card == AnalyticsCards.Revenue }

            with(revenueCardState) {
                assertTrue(this is AnalyticsHubInformationViewState.DataViewState)
                assertTrue(leftSection.delta == null)
                assertTrue(rightSection.delta == null)
            }
        }

    @Test
    fun `given a view model, when selected date range changes, then has expected refresh indicator value`() =
        testBlocking {
            configureSuccessfulStatsResponse()

            sut = givenAViewModel()

            sut.onNewRangeSelection(LAST_YEAR)

            with(sut.viewState.value.refreshIndicator) {
                assertTrue(this is NotShowIndicator)
            }
        }

    @Test
    fun `given a week to date selected, when refresh is requested, then has expected revenue values`() = testBlocking {
        val weekRevenueStats = getRevenueStats(
            OTHER_TOTAL_VALUE,
            OTHER_NET_VALUE,
            OTHER_CURRENCY_CODE,
            DeltaPercentage.Value(OTHER_TOTAL_DELTA),
            DeltaPercentage.Value(OTHER_NET_DELTA),
        )

        updateStats.stub {
            onBlocking { revenueState } doReturn flow { emit(RevenueState.Available(weekRevenueStats)) }
        }

        configureVisibleCards()
        sut = givenAViewModel()
        sut.onNewRangeSelection(WEEK_TO_DATE)
        sut.onRefreshRequested()

        val resourceProvider = givenAResourceProvider()

        val state = sut.viewState.value.cards
        assertTrue(state is AnalyticsHubCardViewState.CardsState)
        val revenueCardState = state.cardsState.find { it.card == AnalyticsCards.Revenue }

        with(revenueCardState) {
            assertTrue(this is AnalyticsHubInformationViewState.DataViewState)
            assertEquals(resourceProvider.getString(R.string.analytics_revenue_card_title), title)
            assertEquals(resourceProvider.getString(R.string.analytics_total_sales_title), leftSection.title)
            assertEquals(OTHER_TOTAL_CURRENCY_VALUE, leftSection.value)
            assertEquals(OTHER_TOTAL_DELTA, leftSection.delta)
            assertEquals(resourceProvider.getString(R.string.analytics_net_sales_title), rightSection.title)
            assertEquals(OTHER_NET_CURRENCY_VALUE, rightSection.value)
            assertEquals(OTHER_NET_DELTA, rightSection.delta)
        }
    }

    @Test
    fun `given a view model, when selected date range changes, then has expected orders values`() =
        testBlocking {
            configureSuccessfulStatsResponse()
            configureVisibleCards()
            sut = givenAViewModel()
            sut.onNewRangeSelection(LAST_YEAR)

            val resourceProvider = givenAResourceProvider()

            val state = sut.viewState.value.cards
            assertTrue(state is AnalyticsHubCardViewState.CardsState)
            val ordersCardState = state.cardsState.find { it.card == AnalyticsCards.Orders }

            with(ordersCardState) {
                assertTrue(this is AnalyticsHubInformationViewState.DataViewState)
                assertEquals(resourceProvider.getString(R.string.analytics_orders_card_title), title)
                assertEquals(resourceProvider.getString(R.string.analytics_total_orders_title), leftSection.title)
                assertEquals(ORDERS_COUNT.toString(), leftSection.value)
                assertEquals(resourceProvider.getString(R.string.analytics_avg_orders_title), rightSection.title)
                assertEquals(ORDERS_COUNT_DELTA, leftSection.delta)
                assertEquals(AVG_CURRENCY_VALUE, rightSection.value)
                assertEquals(AVG_ORDER_VALUE_DELTA, rightSection.delta)
            }
        }

    @Test
    fun `given a view model, when selected date range changes, then product has values`() =
        testBlocking {
            configureSuccessfulStatsResponse()
            configureVisibleCards()
            sut = givenAViewModel()
            sut.onNewRangeSelection(LAST_YEAR)

            val resourceProvider = givenAResourceProvider()

            val state = sut.viewState.value.cards
            assertTrue(state is AnalyticsHubCardViewState.CardsState)
            val productsCardState = state.cardsState.find { it.card == AnalyticsCards.Products }

            with(productsCardState) {
                assertTrue(this is AnalyticsHubListViewState.DataViewState)
                assertEquals(resourceProvider.getString(R.string.analytics_products_card_title), title)
                assertEquals(PRODUCT_ITEMS_SOLD_DELTA, delta)
                assertEquals(resourceProvider.getString(R.string.analytics_products_list_items_sold), subTitle)
                assertEquals(PRODUCT_ITEMS_SOLD.toString(), subTitleValue)
                assertEquals(resourceProvider.getString(R.string.analytics_products_list_header_title), listLeftHeader)
                assertEquals(
                    resourceProvider.getString(R.string.analytics_products_list_header_subtitle),
                    listRightHeader
                )
                assertEquals(PRODUCT_LIST.size, items.size)
            }
        }

    @Test
    fun `given a week to date selected, when refresh is requested, then has expected orders values`() = testBlocking {
        configureVisibleCards()
        val weekOrdersData = getOrdersStats(
            OTHER_ORDERS_COUNT,
            OTHER_ORDERS_COUNT_DELTA,
            OTHER_AVG_ORDER_VALUE,
            OTHER_AVG_ORDER_VALUE_DELTA,
            OTHER_CURRENCY_CODE
        )

        updateStats.stub {
            onBlocking { ordersState } doReturn flow { emit(OrdersState.Available(weekOrdersData)) }
        }

        sut = givenAViewModel()
        sut.onNewRangeSelection(WEEK_TO_DATE)
        sut.onRefreshRequested()

        val resourceProvider = givenAResourceProvider()

        val state = sut.viewState.value.cards
        assertTrue(state is AnalyticsHubCardViewState.CardsState)
        val ordersCardState = state.cardsState.find { it.card == AnalyticsCards.Orders }

        with(ordersCardState) {
            assertTrue(this is AnalyticsHubInformationViewState.DataViewState)
            assertEquals(resourceProvider.getString(R.string.analytics_orders_card_title), title)
            assertEquals(resourceProvider.getString(R.string.analytics_total_orders_title), leftSection.title)
            assertEquals(OTHER_ORDERS_COUNT.toString(), leftSection.value)
            assertEquals(OTHER_ORDERS_COUNT_DELTA, leftSection.delta)
            assertEquals(resourceProvider.getString(R.string.analytics_avg_orders_title), rightSection.title)
            assertEquals(OTHER_AVG_CURRENCY_VALUE, rightSection.value)
            assertEquals(OTHER_AVG_ORDER_VALUE_DELTA, rightSection.delta)
        }
    }

    @Test
    fun `given a week to date selected, when refresh is requested, then revenue is the expected`() = testBlocking {
        configureVisibleCards()
        val weekRevenueStats = getRevenueStats(
            OTHER_TOTAL_VALUE,
            OTHER_NET_VALUE,
            OTHER_CURRENCY_CODE,
            DeltaPercentage.Value(OTHER_TOTAL_DELTA),
            DeltaPercentage.Value(OTHER_NET_DELTA)
        )

        updateStats.stub {
            onBlocking { revenueState } doReturn flow { emit(RevenueState.Available(weekRevenueStats)) }
        }

        sut = givenAViewModel()
        sut.onNewRangeSelection(WEEK_TO_DATE)
        sut.onRefreshRequested()

        val state = sut.viewState.value.cards
        assertTrue(state is AnalyticsHubCardViewState.CardsState)
        val revenueCardState = state.cardsState.find { it.card == AnalyticsCards.Revenue }

        with(revenueCardState) {
            assertTrue(this is AnalyticsHubInformationViewState.DataViewState)
            assertEquals(OTHER_TOTAL_CURRENCY_VALUE, leftSection.value)
            assertEquals(OTHER_TOTAL_DELTA, leftSection.delta)
            assertEquals(OTHER_NET_CURRENCY_VALUE, rightSection.value)
            assertEquals(OTHER_NET_DELTA, rightSection.delta)
        }
    }

    @Test
    fun `given a week to date selected, when refresh is requested, then has expected product values`() = testBlocking {
        configureVisibleCards()
        val weekOrdersData = getProductsStats(
            OTHER_PRODUCT_ITEMS_SOLD,
            OTHER_PRODUCT_ITEMS_SOLD_DELTA,
            OTHER_PRODUCT_LIST
        )

        updateStats.stub {
            onBlocking { productsState } doReturn flow { emit(ProductsState.Available(weekOrdersData)) }
        }

        sut = givenAViewModel()
        sut.onNewRangeSelection(WEEK_TO_DATE)
        sut.onRefreshRequested()

        val resourceProvider = givenAResourceProvider()

        val state = sut.viewState.value.cards
        assertTrue(state is AnalyticsHubCardViewState.CardsState)
        val productsCardState = state.cardsState.find { it.card == AnalyticsCards.Products }

        with(productsCardState) {
            assertTrue(this is AnalyticsHubListViewState.DataViewState)
            assertEquals(resourceProvider.getString(R.string.analytics_products_card_title), title)
            assertEquals(OTHER_PRODUCT_ITEMS_SOLD_DELTA, delta)
            assertEquals(resourceProvider.getString(R.string.analytics_products_list_items_sold), subTitle)
            assertEquals(OTHER_PRODUCT_ITEMS_SOLD.toString(), subTitleValue)
            assertEquals(resourceProvider.getString(R.string.analytics_products_list_header_title), listLeftHeader)
            assertEquals(
                resourceProvider.getString(R.string.analytics_products_list_header_subtitle),
                listRightHeader
            )
            assertEquals(OTHER_PRODUCT_LIST.size, items.size)
        }
    }

    @Test
    fun `given a view, when refresh is requested, then show indicator is the expected`() = testBlocking {
        configureSuccessfulStatsResponse()
        updateStats.stub {
            onBlocking { invoke(any(), any(), any(), any()) } doReturn flow {
                emit(AnalyticsHubUpdateState.Finished)
                emit(AnalyticsHubUpdateState.Loading)
            }
        }

        sut = givenAViewModel()
        val states = mutableListOf<AnalyticsViewState>()
        val getShowIndicatorStatesJob = launch { sut.viewState.toList(states) }
        sut.onRefreshRequested()

        assertThat(states.map(AnalyticsViewState::refreshIndicator)).containsExactly(
            NotShowIndicator,
            RefreshIndicator.ShowIndicator
        )

        getShowIndicatorStatesJob.cancel()
    }

    @Test
    fun `given a view, when custom date range is clicked, then OpenDatePicker event is triggered`() {
        sut = givenAViewModel()
        sut.onCustomDateRangeClicked()

        assertThat(sut.event.value).isInstanceOf(AnalyticsViewEvent.OpenDatePicker::class.java)
    }

    @Test
    fun `when all data is fetched successfully then all transaction conditions are satisfied`() = testBlocking {
        configureVisibleCards()
        configureSuccessfulStatsResponse()

        sut = givenAViewModel()

        verify(transactionLauncher).onRevenueFetched()
        verify(transactionLauncher).onOrdersFetched()
        verify(transactionLauncher).onProductsFetched()
        verify(transactionLauncher).onSessionFetched()
    }

    @Test
    fun `when fetch revenue fails then performance transaction revenue condition is not satisfied`() = testBlocking {
        configureVisibleCards()
        configureSuccessfulStatsResponse()
        updateStats.stub {
            onBlocking { revenueState } doReturn flow { RevenueState.Error }
        }

        sut = givenAViewModel()

        verify(transactionLauncher, never()).onRevenueFetched()
        verify(transactionLauncher).onOrdersFetched()
        verify(transactionLauncher).onProductsFetched()
        verify(transactionLauncher).onSessionFetched()
    }

    @Test
    fun `when fetch orders fails then performance transaction order condition is not satisfied`() = testBlocking {
        configureVisibleCards()
        configureSuccessfulStatsResponse()
        updateStats.stub {
            onBlocking { ordersState } doReturn flow { OrdersState.Error }
        }

        sut = givenAViewModel()

        verify(transactionLauncher).onRevenueFetched()
        verify(transactionLauncher, never()).onOrdersFetched()
        verify(transactionLauncher).onProductsFetched()
        verify(transactionLauncher).onSessionFetched()
    }

    @Test
    fun `when fetch products fails then performance transaction products condition is not satisfied`() = testBlocking {
        configureVisibleCards()
        configureSuccessfulStatsResponse()
        updateStats.stub {
            onBlocking { productsState } doReturn flow { ProductsState.Error }
        }

        sut = givenAViewModel()

        verify(transactionLauncher).onRevenueFetched()
        verify(transactionLauncher).onOrdersFetched()
        verify(transactionLauncher, never()).onProductsFetched()
        verify(transactionLauncher).onSessionFetched()
    }

    @Test
    fun `when fetch visitors fails then performance transaction visitors condition is not satisfied`() = testBlocking {
        configureVisibleCards()
        configureSuccessfulStatsResponse()
        updateStats.stub {
            onBlocking { sessionState } doReturn flow { SessionState.Error }
        }

        sut = givenAViewModel()

        verify(transactionLauncher).onRevenueFetched()
        verify(transactionLauncher).onOrdersFetched()
        verify(transactionLauncher).onProductsFetched()
        verify(transactionLauncher, never()).onSessionFetched()
    }

    @Test
    fun `given a date range selected, then has expected visitors values`() = testBlocking {
        configureVisibleCards()
        configureSuccessfulStatsResponse()
        updateStats.stub {
            onBlocking { sessionState } doReturn flow { emit(SessionState.Available(defaultSessionStat)) }
        }

        sut = givenAViewModel()
        sut.onNewRangeSelection(WEEK_TO_DATE)

        val state = sut.viewState.value.cards
        assertTrue(state is AnalyticsHubCardViewState.CardsState)
        val sessionCardState = state.cardsState.find { it.card == AnalyticsCards.Session }
        assertTrue(sessionCardState is AnalyticsHubInformationViewState)
        assert(sessionCardState)
    }

    @Test
    fun `when the analytics feedback is on state UNANSWERED then show the feedback banner`() = testBlocking {
        whenever(feedbackRepository.getFeatureFeedbackState(FeatureFeedbackSettings.Feature.ANALYTICS_HUB))
            .thenReturn(FeatureFeedbackSettings.FeedbackState.UNANSWERED)

        sut = givenAViewModel()

        assertTrue { sut.viewState.value.showFeedBackBanner }
    }

    @Test
    fun `when the analytics feedback is on state GIVEN then hide the feedback banner`() = testBlocking {
        whenever(feedbackRepository.getFeatureFeedbackState(FeatureFeedbackSettings.Feature.ANALYTICS_HUB))
            .thenReturn(FeatureFeedbackSettings.FeedbackState.GIVEN)

        sut = givenAViewModel()

        assertFalse { sut.viewState.value.showFeedBackBanner }
    }

    @Test
    fun `when the analytics feedback is on state DISMISSED then hide the feedback banner`() = testBlocking {
        whenever(feedbackRepository.getFeatureFeedbackState(FeatureFeedbackSettings.Feature.ANALYTICS_HUB))
            .thenReturn(FeatureFeedbackSettings.FeedbackState.DISMISSED)

        sut = givenAViewModel()

        assertFalse { sut.viewState.value.showFeedBackBanner }
    }

    @Test
    fun `when send feedback is pressed then feedback status is saved as GIVEN`() = testBlocking {
        sut = givenAViewModel()

        sut.onSendFeedbackClicked()

        verify(feedbackRepository).saveFeatureFeedback(
            FeatureFeedbackSettings.Feature.ANALYTICS_HUB,
            FeatureFeedbackSettings.FeedbackState.GIVEN
        )
        assertThat(sut.event.value).isInstanceOf(AnalyticsViewEvent.SendFeedback::class.java)
    }

    @Test
    fun `when dismiss feedback is pressed then feedback status is saved as DISMISSED`() = testBlocking {
        sut = givenAViewModel()

        sut.onDismissBannerClicked()

        verify(feedbackRepository).saveFeatureFeedback(
            FeatureFeedbackSettings.Feature.ANALYTICS_HUB,
            FeatureFeedbackSettings.FeedbackState.DISMISSED
        )
    }

    @Test
    fun `when send feedback is pressed then send feedback event is tracked`() = testBlocking {
        sut = givenAViewModel()

        sut.onSendFeedbackClicked()

        verify(tracker).track(
            AnalyticsEvent.FEATURE_FEEDBACK_BANNER,
            mapOf(
                AnalyticsTracker.KEY_FEEDBACK_CONTEXT to AnalyticsTracker.VALUE_ANALYTICS_HUB_FEEDBACK,
                AnalyticsTracker.KEY_FEEDBACK_ACTION to AnalyticsTracker.VALUE_FEEDBACK_GIVEN
            )
        )
    }

    @Test
    fun `when dismiss feedback is pressed then dismiss feedback event is tracked`() = testBlocking {
        sut = givenAViewModel()

        sut.onDismissBannerClicked()

        verify(tracker).track(
            AnalyticsEvent.FEATURE_FEEDBACK_BANNER,
            mapOf(
                AnalyticsTracker.KEY_FEEDBACK_CONTEXT to AnalyticsTracker.VALUE_ANALYTICS_HUB_FEEDBACK,
                AnalyticsTracker.KEY_FEEDBACK_ACTION to AnalyticsTracker.VALUE_FEEDBACK_DISMISSED
            )
        )
    }

    @Test
    fun `when a new range selection is selected, then the new selection is tracked`() = testBlocking {
        configureSuccessfulStatsResponse()
        sut = givenAViewModel()

        sut.onNewRangeSelection(WEEK_TO_DATE)

        verify(tracker).track(
            AnalyticsEvent.ANALYTICS_HUB_DATE_RANGE_SELECTED,
            mapOf(AnalyticsTracker.KEY_OPTION to WEEK_TO_DATE.identifier)
        )
    }

    @Test
    fun `when a custom range selection is selected, then the custom range selection is tracked`() = testBlocking {
        configureSuccessfulStatsResponse()
        sut = givenAViewModel()
        val startDate = Date()
        val endDate = Date()

        sut.onCustomRangeSelected(startDate, endDate)

        verify(tracker).track(
            AnalyticsEvent.ANALYTICS_HUB_DATE_RANGE_SELECTED,
            mapOf(AnalyticsTracker.KEY_OPTION to CUSTOM.identifier)
        )
    }

    @Test
    fun `when the range selection control is pressed, then register a track event`() = testBlocking {
        configureSuccessfulStatsResponse()
        sut = givenAViewModel()

        sut.onDateRangeSelectorClick()

        verify(tracker).track(AnalyticsEvent.ANALYTICS_HUB_DATE_RANGE_BUTTON_TAPPED)
    }

    @Test
    fun `when onRefreshRequested is called, then trigger expected analytics event`() = testBlocking {
        configureSuccessfulStatsResponse()
        sut = givenAViewModel()

        sut.onRefreshRequested()

        verify(tracker).track(AnalyticsEvent.ANALYTICS_HUB_PULL_TO_REFRESH_TRIGGERED)
    }

    @Test
    fun `when last update information changes, then update view state as expected`() = testBlocking {
        val lastUpdateTimestamp = 123456789L
        whenever(
            observeLastUpdate.invoke(selectedRange = any(), analyticDataList = any())
        ).thenReturn(flowOf(lastUpdateTimestamp))
        whenever(dateUtils.getDateOrTimeFromMillis(lastUpdateTimestamp)).thenReturn("9:35 AM")
        configureVisibleCards()
        sut = givenAViewModel()

        assertThat(sut.viewState.value.lastUpdateTimestamp).isEqualTo("9:35 AM")
    }

    @Test
    fun `when last update information is not initialized, then update view state field is empty`() = testBlocking {
        sut = givenAViewModel()

        assertThat(sut.viewState.value.lastUpdateTimestamp).isEmpty()
    }

    @Test
    fun `when last update information is null, then update view state field is empty`() = testBlocking {
        sut = givenAViewModel()

        assertThat(sut.viewState.value.lastUpdateTimestamp).isEmpty()
    }

    @Test
    fun `when site is a wpcom and see report is pressed then open a wpcom webview`() = testBlocking {
        whenever(selectedSite.getOrNull()).thenReturn(SiteModel().apply { setIsWpComStore(true) })
        sut = givenAViewModel()
        sut.onSeeReport("https://report-url", ReportCard.Revenue)
        assertThat(sut.event.value).isInstanceOf(AnalyticsViewEvent.OpenWPComWebView::class.java)
    }

    @Test
    fun `when site is not a wpcom and see report is pressed then open the default webview`() = testBlocking {
        whenever(selectedSite.getOrNull()).thenReturn(SiteModel().apply { setIsWpComStore(false) })
        sut = givenAViewModel()
        sut.onSeeReport("https://report-url", ReportCard.Revenue)
        assertThat(sut.event.value).isInstanceOf(AnalyticsViewEvent.OpenUrl::class.java)
    }

    @Test
    fun `when see report is pressed then track see report event`() = testBlocking {
        whenever(selectedSite.getOrNull()).thenReturn(SiteModel().apply { setIsWpComStore(true) })
        sut = givenAViewModel()
        sut.onNewRangeSelection(WEEK_TO_DATE)

        sut.onSeeReport("https://report-url", ReportCard.Revenue)
        verify(tracker).track(
            AnalyticsEvent.ANALYTICS_HUB_VIEW_FULL_REPORT_TAPPED,
            mapOf(
                AnalyticsTracker.KEY_PERIOD to WEEK_TO_DATE.identifier,
                AnalyticsTracker.KEY_REPORT to ReportCard.Revenue.name.lowercase(),
                AnalyticsTracker.KEY_COMPARE to AnalyticsTracker.VALUE_PREVIOUS_PERIOD
            )
        )
    }

    @Test
    fun `when see report is pressed then interaction tracked`() = testBlocking {
        whenever(selectedSite.getOrNull()).thenReturn(
            SiteModel().apply {
                setIsWpComStore(true)
                adminUrl = "https://report-url/wc-admin"
            }
        )
        configureSuccessfulStatsResponse()
        sut = givenAViewModel()
        // When the view is initialized we track some interactions
        clearInvocations(trackerEventEmitter)

        sut.onSeeReport("https://report-url", ReportCard.Revenue)
        verify(trackerEventEmitter).interacted(any())
    }

    @Test
    fun `when a card configuration is received, then visible cards follows the configuration `() = testBlocking {
        val configuration = listOf(
            AnalyticCardConfiguration(
                card = AnalyticsCards.Revenue,
                title = AnalyticsCards.Revenue.name,
                isVisible = false
            ),
            AnalyticCardConfiguration(
                card = AnalyticsCards.Orders,
                title = AnalyticsCards.Orders.name,
                isVisible = true
            ),
            AnalyticCardConfiguration(
                card = AnalyticsCards.Products,
                title = AnalyticsCards.Products.name,
                isVisible = false
            ),
            AnalyticCardConfiguration(
                card = AnalyticsCards.Session,
                title = AnalyticsCards.Session.name,
                isVisible = true
            )
        )
        whenever(observeLastUpdate.invoke(selectedRange = any(), analyticDataList = any())).thenReturn(flowOf(null))
        configureVisibleCards(configuration)
        configureSuccessfulStatsResponse()

        sut = givenAViewModel()
        sut.onNewRangeSelection(LAST_YEAR)

        with(sut.viewState.value.cards) {
            assertTrue(this is AnalyticsHubCardViewState.CardsState)
            val revenueState = this.cardsState.find { it.card == AnalyticsCards.Revenue }
            val ordersState = this.cardsState.find { it.card == AnalyticsCards.Orders }
            val productsState = this.cardsState.find { it.card == AnalyticsCards.Products }
            val sessionState = this.cardsState.find { it.card == AnalyticsCards.Session }

            assertNull(revenueState)
            assertTrue(ordersState is AnalyticsHubInformationViewState.DataViewState)
            assertNull(productsState)
            assertTrue(sessionState is AnalyticsHubInformationViewState.DataViewState)
        }
    }

    @Test
    fun `when some cards are hidden, then observation job for those cards is null`() = testBlocking {
        val configuration = listOf(
            AnalyticCardConfiguration(
                card = AnalyticsCards.Revenue,
                title = AnalyticsCards.Revenue.name,
                isVisible = false
            ),
            AnalyticCardConfiguration(
                card = AnalyticsCards.Orders,
                title = AnalyticsCards.Orders.name,
                isVisible = true
            ),
            AnalyticCardConfiguration(
                card = AnalyticsCards.Products,
                title = AnalyticsCards.Products.name,
                isVisible = false
            ),
            AnalyticCardConfiguration(
                card = AnalyticsCards.Session,
                title = AnalyticsCards.Session.name,
                isVisible = true
            )
        )
        whenever(observeLastUpdate.invoke(selectedRange = any(), analyticDataList = any())).thenReturn(flowOf(null))
        configureVisibleCards(configuration)
        configureSuccessfulStatsResponse()

        sut = givenAViewModel()
        sut.onNewRangeSelection(LAST_YEAR)

        assertThat(sut.sessionObservationJob).isNotNull
        assertThat(sut.ordersObservationJob).isNotNull
        assertThat(sut.revenueObservationJob).isNull()
        assertThat(sut.productObservationJob).isNull()
    }

    @Test
    fun `when a new range is selected, the update stats its call with the right visible cards`() = testBlocking {
        val configuration = listOf(
            AnalyticCardConfiguration(
                card = AnalyticsCards.Revenue,
                title = AnalyticsCards.Revenue.name,
                isVisible = false
            ),
            AnalyticCardConfiguration(
                card = AnalyticsCards.Orders,
                title = AnalyticsCards.Orders.name,
                isVisible = true
            ),
            AnalyticCardConfiguration(
                card = AnalyticsCards.Products,
                title = AnalyticsCards.Products.name,
                isVisible = false
            ),
            AnalyticCardConfiguration(
                card = AnalyticsCards.Session,
                title = AnalyticsCards.Session.name,
                isVisible = true
            )
        )

        val expectedVisibleCards = configuration.filter { it.isVisible }.map { it.card }

        whenever(observeLastUpdate.invoke(selectedRange = any(), analyticDataList = any())).thenReturn(flowOf(null))
        configureVisibleCards(configuration)
        configureSuccessfulStatsResponse()

        sut = givenAViewModel()

        verify(updateStats).invoke(
            rangeSelection = any(),
            scope = any(),
            forceUpdate = any(),
            visibleCards = eq(expectedVisibleCards)
        )
    }

    @Test
    fun `when a a refresh event is triggered, then the update stats its call with the right visible cards`() =
        testBlocking {
            val configuration = listOf(
                AnalyticCardConfiguration(
                    card = AnalyticsCards.Revenue,
                    title = AnalyticsCards.Revenue.name,
                    isVisible = false
                ),
                AnalyticCardConfiguration(
                    card = AnalyticsCards.Orders,
                    title = AnalyticsCards.Orders.name,
                    isVisible = true
                ),
                AnalyticCardConfiguration(
                    card = AnalyticsCards.Products,
                    title = AnalyticsCards.Products.name,
                    isVisible = false
                ),
                AnalyticCardConfiguration(
                    card = AnalyticsCards.Session,
                    title = AnalyticsCards.Session.name,
                    isVisible = true
                )
            )

            val expectedVisibleCards = configuration.filter { it.isVisible }.map { it.card }

            whenever(observeLastUpdate.invoke(selectedRange = any(), analyticDataList = any())).thenReturn(flowOf(null))
            configureVisibleCards(configuration)
            configureSuccessfulStatsResponse()

            sut = givenAViewModel()
            clearInvocations(updateStats)

            sut.onRefreshRequested()

            verify(updateStats).invoke(
                rangeSelection = any(),
                scope = any(),
                forceUpdate = any(),
                visibleCards = eq(expectedVisibleCards)
            )
        }

    @Test
    fun `when update analytics settings is pressed then the event is tracked`() = testBlocking {
        configureSuccessfulStatsResponse()
        sut = givenAViewModel()

        sut.onOpenSettings()
        verify(tracker).track(AnalyticsEvent.ANALYTICS_HUB_SETTINGS_OPENED)
    }

    private fun givenAResourceProvider(): ResourceProvider = mock {
        on { getString(any()) } doAnswer { invocationOnMock -> invocationOnMock.arguments[0].toString() }
        on { getString(any(), any()) } doAnswer { invMock -> invMock.arguments[0].toString() }
    }

    private fun givenAViewModel(resourceProvider: ResourceProvider = givenAResourceProvider()): AnalyticsHubViewModel {
        return AnalyticsHubViewModel(
            resourceProvider,
            currencyFormatter,
            transactionLauncher,
            trackerEventEmitter,
            updateStats,
            observeLastUpdate,
            localeProvider,
            feedbackRepository,
            tracker,
            dateUtils,
            selectedSite,
            getReportUrl,
            observeAnalyticsCardsConfiguration,
            savedState
        )
    }

    private fun getRevenueStats(
        totalValue: Double = TOTAL_VALUE,
        netValue: Double = NET_VALUE,
        currencyCode: String = CURRENCY_CODE,
        totalDelta: DeltaPercentage = DeltaPercentage.Value(TOTAL_DELTA.toInt()),
        netDelta: DeltaPercentage = DeltaPercentage.Value(NET_DELTA.toInt()),
    ) = RevenueStat(
        totalValue,
        totalDelta,
        netValue,
        netDelta,
        currencyCode,
        listOf(TOTAL_VALUE),
        listOf(NET_VALUE)
    )

    private fun getOrdersStats(
        ordersCount: Int = ORDERS_COUNT,
        ordersCountDelta: Int = ORDERS_COUNT_DELTA,
        avgOrderValue: Double = AVG_ORDER_VALUE,
        avgOrderValueDelta: Int = AVG_ORDER_VALUE_DELTA,
        currencyCode: String = CURRENCY_CODE
    ) = OrdersStat(
        ordersCount,
        DeltaPercentage.Value(ordersCountDelta),
        avgOrderValue,
        DeltaPercentage.Value(avgOrderValueDelta),
        currencyCode,
        listOf(ORDERS_COUNT.toLong()),
        listOf(AVG_ORDER_VALUE)
    )

    private fun getProductsStats(
        itemsSold: Int = PRODUCT_ITEMS_SOLD,
        itemsSoldDelta: Int = PRODUCT_ITEMS_SOLD_DELTA,
        productList: List<ProductItem> = PRODUCT_LIST
    ) = ProductsStat(itemsSold, DeltaPercentage.Value(itemsSoldDelta), productList)

    private fun assert(visitorState: AnalyticsHubInformationViewState) {
        val resourceProvider = givenAResourceProvider()
        assertThat(visitorState).isEqualTo(
            AnalyticsHubInformationViewState.DataViewState(
                card = AnalyticsCards.Session,
                title = resourceProvider.getString(R.string.analytics_session_card_title),
                leftSection = AnalyticsHubInformationSectionViewState(
                    title = resourceProvider.getString(R.string.analytics_visitors_subtitle),
                    value = DEFAULT_VISITORS_COUNT.toString(),
                    delta = null,
                    chartInfo = emptyList()
                ),
                rightSection = AnalyticsHubInformationSectionViewState(
                    title = resourceProvider.getString(R.string.analytics_conversion_subtitle),
                    value = defaultSessionStat.conversionRate,
                    delta = null,
                    chartInfo = emptyList()
                ),
                reportUrl = null
            )
        )
    }

    private fun configureSuccessfulStatsResponse() {
        updateStats.stub {
            onBlocking { revenueState } doReturn flow {
                emit(RevenueState.Available(RevenueStat.EMPTY))
                emit(RevenueState.Loading)
                emit(RevenueState.Available(getRevenueStats()))
            }
            onBlocking { ordersState } doReturn flow {
                emit(OrdersState.Available(OrdersStat.EMPTY))
                emit(OrdersState.Loading)
                emit(OrdersState.Available(getOrdersStats()))
            }
            onBlocking { productsState } doReturn flow {
                emit(ProductsState.Available(ProductsStat.EMPTY))
                emit(ProductsState.Loading)
                emit(ProductsState.Available(getProductsStats()))
            }
            onBlocking { sessionState } doReturn flow {
                emit(SessionState.Available(SessionStat.EMPTY))
                emit(SessionState.Loading)
                emit(SessionState.Available(testSessionStat))
            }
            onBlocking { invoke(any(), any(), any(), any()) } doReturn flow {
                emit(AnalyticsHubUpdateState.Finished)
            }
        }
    }

    private fun configureVisibleCards(configuration: List<AnalyticCardConfiguration> = defaultCardsConfiguration) {
        observeAnalyticsCardsConfiguration.stub {
            onBlocking { observeAnalyticsCardsConfiguration.invoke() } doReturn flowOf(configuration)
        }
    }

    companion object {
        private const val ANY_VALUE = "Today"

        const val TOTAL_VALUE = 10.0
        const val TOTAL_DELTA = 5.0
        const val NET_VALUE = 20.0
        const val NET_DELTA = 10.0
        const val CURRENCY_CODE = "EUR"
        const val TOTAL_CURRENCY_VALUE = "10 E"
        const val NET_CURRENCY_VALUE = "10 E"

        const val OTHER_TOTAL_VALUE = 20.0
        const val OTHER_TOTAL_DELTA = 15
        const val OTHER_NET_VALUE = 10.0
        const val OTHER_NET_DELTA = 20
        const val OTHER_CURRENCY_CODE = "DOL"
        const val OTHER_TOTAL_CURRENCY_VALUE = "20 USD"
        const val OTHER_NET_CURRENCY_VALUE = "10 USD"

        const val PRODUCT_ITEMS_SOLD = 1
        const val PRODUCT_ITEMS_SOLD_DELTA = 50
        const val PRODUCT_CURRENCY_VALUE = "50 E"
        const val OTHER_PRODUCT_ITEMS_SOLD = 3
        const val OTHER_PRODUCT_ITEMS_SOLD_DELTA = 10
        const val OTHER_PRODUCT_CURRENCY_VALUE = "55 E"

        private const val PRODUCT_ONE_QUANTITY = 1
        private const val PRODUCT_MORE_THAN_ONE_QUANTITY = 10
        private const val PRODUCT_NET_SALES = 1.toDouble()
        private const val OTHER_PRODUCT_NET_SALES = 2.toDouble()
        private const val PRODUCT_ITEM_IMAGE = "image"
        private const val PRODUCT_ITEM_NAME = "product"
        private const val PRODUCT_CURRENCY_CODE = "EUR"

        val PRODUCT_LIST = listOf(
            ProductItem(
                PRODUCT_ITEM_NAME,
                PRODUCT_NET_SALES,
                PRODUCT_ITEM_IMAGE,
                PRODUCT_ONE_QUANTITY,
                PRODUCT_CURRENCY_CODE
            ),
            ProductItem(
                PRODUCT_ITEM_NAME,
                PRODUCT_NET_SALES,
                PRODUCT_ITEM_IMAGE,
                PRODUCT_MORE_THAN_ONE_QUANTITY,
                PRODUCT_CURRENCY_CODE
            )
        ).sortedByDescending { it.quantity }

        val OTHER_PRODUCT_LIST = listOf(
            ProductItem(
                PRODUCT_ITEM_NAME,
                PRODUCT_NET_SALES,
                PRODUCT_ITEM_IMAGE,
                PRODUCT_ONE_QUANTITY,
                PRODUCT_CURRENCY_CODE
            )
        ).sortedByDescending { it.quantity }

        private const val DEFAULT_VISITORS_COUNT = 321

        const val ORDERS_COUNT = 5
        const val OTHER_ORDERS_COUNT = 50
        const val ORDERS_COUNT_DELTA = 20
        const val OTHER_ORDERS_COUNT_DELTA = 1
        const val AVG_ORDER_VALUE = 11.2
        const val OTHER_AVG_ORDER_VALUE = 44.21
        const val AVG_ORDER_VALUE_DELTA = 50
        const val OTHER_AVG_ORDER_VALUE_DELTA = 1
        const val AVG_CURRENCY_VALUE = "11.20 E"
        const val OTHER_AVG_CURRENCY_VALUE = "44.21 E"

        private val defaultSessionStat = SessionStat(ORDERS_COUNT, DEFAULT_VISITORS_COUNT)
    }
}
