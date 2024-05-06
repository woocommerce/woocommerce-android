package com.woocommerce.android.wear

import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.gson.Gson
import com.woocommerce.android.extensions.convertedFrom
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.commons.wear.DataParameters.CONVERSION_RATE
import com.woocommerce.commons.wear.DataParameters.ORDERS_COUNT
import com.woocommerce.commons.wear.DataParameters.SITE_JSON
import com.woocommerce.commons.wear.DataParameters.TIMESTAMP
import com.woocommerce.commons.wear.DataParameters.TOKEN
import com.woocommerce.commons.wear.DataParameters.TOTAL_REVENUE
import com.woocommerce.commons.wear.DataParameters.VISITORS_TOTAL
import com.woocommerce.commons.wear.DataPath
import com.woocommerce.commons.wear.DataPath.SITE_DATA
import com.woocommerce.commons.wear.DataPath.STATS_DATA
import org.wordpress.android.fluxc.store.AccountStore
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class WearableConnectionRepository @Inject constructor(
    private val dataClient: DataClient,
    private val selectedSite: SelectedSite,
    private val accountStore: AccountStore,
    private val getStats: GetWearableMyStoreStats,
    private val coroutineScope: CoroutineScope
) {
    private val gson by lazy { Gson() }

    fun sendSiteData() {
        sendData(
            SITE_DATA,
            DataMap().apply {
                val siteJSON = gson.toJson(selectedSite.get())
                putString(SITE_JSON.value, siteJSON)
                putString(TOKEN.value, accountStore.accessToken.orEmpty())
            }
        )
    }

    fun sendStatsData() = coroutineScope.launch {
        val stats = getStats(selectedSite.get())
        val revenueTotals = stats?.revenue?.parseTotal()
        val totalSales = revenueTotals?.totalSales ?: 0.0
        val ordersCount = revenueTotals?.ordersCount ?: 0
        val visitorsCount = stats?.visitors?.values?.sum() ?: 0
        val conversionRate = ordersCount convertedFrom visitorsCount

        sendData(
            STATS_DATA,
            DataMap().apply {
                putDouble(TOTAL_REVENUE.value, totalSales)
                putInt(ORDERS_COUNT.value, ordersCount)
                putInt(VISITORS_TOTAL.value, visitorsCount)
                putString(CONVERSION_RATE.value, conversionRate)
            }
        )
    }

    private fun sendData(
        dataPath: DataPath,
        data: DataMap
    ) {
        PutDataMapRequest
            .create(dataPath.value)
            .apply {
                dataMap.putAll(data)
                dataMap.putLong(TIMESTAMP.value, Instant.now().epochSecond)
            }.asPutDataRequest().setUrgent()
            .let { dataClient.putDataItem(it) }
    }
}
