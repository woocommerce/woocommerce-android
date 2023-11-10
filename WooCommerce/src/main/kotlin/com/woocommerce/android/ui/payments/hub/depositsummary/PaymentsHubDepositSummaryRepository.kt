package com.woocommerce.android.ui.payments.hub.depositsummary

import kotlinx.coroutines.flow.flow
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.payments.woo.WooPaymentsDepositsOverview
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.store.WCWooPaymentsStore
import javax.inject.Inject

class PaymentsHubDepositSummaryRepository @Inject constructor(
    private val store: WCWooPaymentsStore,
    private val site: SiteModel,
) {
    suspend fun retrieveDepositOverview() =
        flow {
            val cachedData = store.getDepositsOverviewAll(site)
            if (cachedData != null) {
                emit(RetrieveDepositOverviewResult.Cache(cachedData))
            }

            val fetchedData = store.fetchDepositsOverview(site)
            val data = fetchedData.result
            if (fetchedData.isError || data == null) {
                store.deleteDepositsOverview(site)
                emit(RetrieveDepositOverviewResult.Error(fetchedData.error))
            } else {
                store.insertDepositsOverview(site, data)
                emit(RetrieveDepositOverviewResult.Remote(data))
            }
        }
}

sealed class RetrieveDepositOverviewResult {
    data class Error(val error: WooError) : RetrieveDepositOverviewResult()
    data class Cache(val overview: WooPaymentsDepositsOverview) :
        RetrieveDepositOverviewResult()

    data class Remote(val overview: WooPaymentsDepositsOverview) :
        RetrieveDepositOverviewResult()
}
