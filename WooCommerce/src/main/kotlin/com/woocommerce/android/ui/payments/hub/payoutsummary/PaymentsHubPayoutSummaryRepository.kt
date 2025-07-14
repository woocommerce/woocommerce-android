package com.woocommerce.android.ui.payments.hub.payoutsummary

import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.flow.flow
import org.wordpress.android.fluxc.model.payments.woo.WooPaymentsDepositsOverview
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.store.WCWooPaymentsStore
import javax.inject.Inject

class PaymentsHubPayoutSummaryRepository @Inject constructor(
    private val store: WCWooPaymentsStore,
    private val site: SelectedSite,
) {
    suspend fun retrievePayoutOverview() =
        flow {
            val cachedData = store.getDepositsOverviewAll(site.get())
            if (cachedData != null) {
                emit(RetrievePayoutOverviewResult.Cache(cachedData))
            }

            val fetchedData = store.fetchDepositsOverview(site.get())
            val data = fetchedData.result
            if (fetchedData.isError || data == null) {
                store.deleteDepositsOverview(site.get())
                emit(RetrievePayoutOverviewResult.Error(fetchedData.error))
            } else {
                store.insertDepositsOverview(site.get(), data)
                emit(RetrievePayoutOverviewResult.Remote(data))
            }
        }
}

sealed class RetrievePayoutOverviewResult {
    data class Error(val error: WooError) : RetrievePayoutOverviewResult()
    data class Cache(val overview: WooPaymentsDepositsOverview) :
        RetrievePayoutOverviewResult()

    data class Remote(val overview: WooPaymentsDepositsOverview) :
        RetrievePayoutOverviewResult()
}
