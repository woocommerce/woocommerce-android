package com.woocommerce.android.ui.payments.hub.depositsummary

import kotlinx.coroutines.flow.flow
import org.wordpress.android.fluxc.model.SiteModel
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
                emit(cachedData)
            }

            val fetchedData = store.fetchDepositsOverview(site)

        }
}

sealed class RetrieveDepositOverviewResult {
    data class Error(val error: WooError) : RetrieveDepositOverviewResult()

}
