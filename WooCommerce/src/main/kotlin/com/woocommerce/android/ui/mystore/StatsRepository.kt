package com.woocommerce.android.ui.mystore

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.ContinuationWrapper
import com.woocommerce.android.util.ContinuationWrapper.ContinuationResult.Cancellation
import com.woocommerce.android.util.ContinuationWrapper.ContinuationResult.Success
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T.DASHBOARD
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.WCStatsAction.FETCH_REVENUE_STATS
import org.wordpress.android.fluxc.generated.WCStatsActionBuilder
import org.wordpress.android.fluxc.model.WCRevenueStatsModel
import org.wordpress.android.fluxc.store.WCStatsStore
import org.wordpress.android.fluxc.store.WCStatsStore.*
import javax.inject.Inject

class StatsRepository @Inject constructor(
    private val selectedSite: SelectedSite,
    private val dispatcher: Dispatcher,
    private val wcStatsStore: WCStatsStore
) {
    companion object {
        private val TAG = MyStorePresenter::class.java
    }

    private var continuationRevenueStats = ContinuationWrapper<Result<WCRevenueStatsModel?>>(DASHBOARD)

    fun init() {
        dispatcher.register(this)
    }

    fun onCleanup() {
        dispatcher.unregister(this)
    }

    suspend fun fetchRevenueStats(
        granularity: StatsGranularity,
        forced: Boolean
    ): Result<WCRevenueStatsModel?> {
        val result =  continuationRevenueStats.callAndWait {
            val statsPayload = FetchRevenueStatsPayload(selectedSite.get(), granularity, forced = forced)
            dispatcher.dispatch(WCStatsActionBuilder.newFetchRevenueStatsAction(statsPayload))
        }

        return when(result) {
            is Cancellation -> Result.failure(result.exception)
            is Success -> result.value
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onWCRevenueStatsChanged(event: OnWCRevenueStatsChanged) {
        if (event.causeOfChange == FETCH_REVENUE_STATS) {
            if (event.isError) {
                WooLog.e(DASHBOARD, "$TAG - Error fetching stats: ${event.error.message}")
                // display a different error snackbar if the error type is not "plugin not active", since
                // this error is already being handled by the activity class
                val exception = StatsException(
                    error = event.error
                )
                continuationRevenueStats.continueWith(Result.failure(exception))
            } else {
                val revenueStatsModel = wcStatsStore.getRawRevenueStats(
                    selectedSite.get(), event.granularity, event.startDate!!, event.endDate!!
                )
                continuationRevenueStats.continueWith(Result.success(revenueStatsModel))
            }
        }
    }

    data class StatsException(val error: OrderStatsError): Exception()
}
