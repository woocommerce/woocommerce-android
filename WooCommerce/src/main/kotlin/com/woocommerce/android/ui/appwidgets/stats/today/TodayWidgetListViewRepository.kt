package com.woocommerce.android.ui.appwidgets.stats.today

import com.woocommerce.android.model.RequestResult
import com.woocommerce.android.model.RequestResult.ERROR
import com.woocommerce.android.model.RequestResult.NO_ACTION_NEEDED
import com.woocommerce.android.model.RequestResult.RETRY
import com.woocommerce.android.model.RequestResult.SUCCESS
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T.DASHBOARD
import com.woocommerce.android.util.suspendCoroutineWithTimeout
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.WCStatsAction.FETCH_NEW_VISITOR_STATS
import org.wordpress.android.fluxc.generated.WCStatsActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCRevenueStatsModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.WCStatsStore
import org.wordpress.android.fluxc.store.WCStatsStore.FetchNewVisitorStatsPayload
import org.wordpress.android.fluxc.store.WCStatsStore.FetchRevenueStatsPayload
import org.wordpress.android.fluxc.store.WCStatsStore.OnWCRevenueStatsChanged
import org.wordpress.android.fluxc.store.WCStatsStore.OnWCStatsChanged
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.fluxc.utils.DateUtils
import javax.inject.Inject
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

class TodayWidgetListViewRepository @Inject constructor(
    private val dispatcher: Dispatcher,
    private val accountStore: AccountStore, // required to verify if user is logged in
    private val statsStore: WCStatsStore, // required to fetch stats data
    private val wooCommerceStore: WooCommerceStore // required to fetch site currency settings
) {
    companion object {
        private const val ACTION_TIMEOUT = 10L * 1000
    }

    private var isFetchingData: Boolean = false

    private var continuationFetchTodayRevenue: Continuation<Boolean>? = null
    private var continuationFetchTodayVisitors: Continuation<Boolean>? = null

    init {
        dispatcher.register(this)
    }

    fun onCleanup() {
        dispatcher.unregister(this)
    }

    fun userIsLoggedIn() = accountStore.hasAccessToken()

    /**
     * Fires 2 requests to fetch the current day revenue stats and current day visitor stats for a given site
     *
     * Wait for both requests to complete. If the fetch is already in progress
     * return [RequestResult.NO_ACTION_NEEDED].
     *
     * @return the result of the action as a [RequestResult]
     */
    suspend fun fetchTodayStats(site: SiteModel): RequestResult {
        return if (!isFetchingData) {
            isFetchingData = true

            return coroutineScope {
                //
                val isUserLoggedIn = userIsLoggedIn()
                if (isUserLoggedIn) {
                    var fetchedRevenueStats = false
                    var fetchedVisitorStats = false

                    val fetchRevenueStats = async {
                        fetchedRevenueStats = fetchRevenueStats(site)
                    }
                    val fetchVisitorStats = async {
                        fetchedVisitorStats = fetchVisitorStats(site)
                    }
                    fetchRevenueStats.await()
                    fetchVisitorStats.await()

                    if (fetchedRevenueStats && fetchedVisitorStats) SUCCESS else RETRY
                } else ERROR
            }
        } else NO_ACTION_NEEDED
    }

    /**
     * Fires the request to fetch the revenue stats info for a site
     *
     * @return the result of the action as a [Boolean]
     */
    private suspend fun fetchRevenueStats(site: SiteModel): Boolean {
        return try {
            suspendCoroutineWithTimeout<Boolean>(ACTION_TIMEOUT) {
                continuationFetchTodayRevenue = it

                val payload = FetchRevenueStatsPayload(site, StatsGranularity.DAYS, forced = true)
                // TODO dispatcher.dispatch(WCStatsActionBuilder.newFetchRevenueStatsPayload(payload))
            } ?: false // request timed out
        } catch (e: CancellationException) {
            WooLog.e(DASHBOARD, "Exception encountered while fetching visitor stats", e)
            false
        }
    }

    /**
     * Fires the request to fetch the visitor stats info for the current day, for a site
     *
     * @return the result of the action as a [Boolean]
     */
    private suspend fun fetchVisitorStats(site: SiteModel): Boolean {
        return try {
            suspendCoroutineWithTimeout<Boolean>(ACTION_TIMEOUT) {
                continuationFetchTodayVisitors = it

                val payload = FetchNewVisitorStatsPayload(site, StatsGranularity.DAYS, forced = true)
                dispatcher.dispatch(WCStatsActionBuilder.newFetchNewVisitorStatsAction(payload))
            } ?: false // request timed out
        } catch (e: CancellationException) {
            WooLog.e(DASHBOARD, "Exception encountered while fetching visitor stats", e)
            false
        }
    }

    fun getTodayRevenueStats(site: SiteModel): WCRevenueStatsModel? {
        // loads a formatted date that accounts for the site's timezone setting,
        // in the format yyy-MM-ddThh:mm:ss with the time always set to the START of the current day
        val startDate = DateUtils.getStartDateForSite(site, DateUtils.getStartOfCurrentDay())

        // Loads a formatted date that accounts for the site's timezone setting,
        // in the format yyy-MM-ddThh:mm:ss with the time always set to the END of the current day
        val endDate = DateUtils.getEndDateForSite(site)

        return statsStore.getRawRevenueStats(
            site, StatsGranularity.DAYS, startDate, endDate
        )
    }

    fun getTodayVisitorStats(site: SiteModel): String {
        val date = DateUtils.getDateTimeForSite(site, "yyyy-MM-dd", DateUtils.getStartOfCurrentDay())
        val visitorStats = statsStore.getNewVisitorStats(
            site, StatsGranularity.DAYS, "1", date, true
        )
        return visitorStats.values.sum().toString()
    }

    fun getStatsCurrency(site: SiteModel) = wooCommerceStore.getSiteSettings(site)?.currencyCode

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onWCRevenueStatsChanged(event: OnWCRevenueStatsChanged) {
        // TODO
        /*if (event.causeOfChange == FETCH_REVENUE_STATS) {
            if (event.isError) {
                if (event.error.type == PLUGIN_NOT_ACTIVE) {
                    AppPrefs.setV4StatsSupported(false)
                }
                continuationFetchTodayRevenue?.resume(false)
            } else {
                continuationFetchTodayRevenue?.resume(true)
            }
            continuationFetchTodayRevenue = null
        }*/
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onWCStatsChanged(event: OnWCStatsChanged) {
        if (event.causeOfChange == FETCH_NEW_VISITOR_STATS) {
            if (event.isError) {
                // Error fetching visitor stats
                continuationFetchTodayVisitors?.resume(false)
            } else {
                continuationFetchTodayVisitors?.resume(true)
            }
            continuationFetchTodayVisitors = null
        }
    }
}
