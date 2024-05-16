package com.woocommerce.android.wear

import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.gson.Gson
import com.woocommerce.android.extensions.convertedFrom
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.commons.wear.DataParameters.CONVERSION_RATE
import com.woocommerce.commons.wear.DataParameters.ORDERS_COUNT
import com.woocommerce.commons.wear.DataParameters.ORDERS_JSON
import com.woocommerce.commons.wear.DataParameters.ORDER_ID
import com.woocommerce.commons.wear.DataParameters.ORDER_PRODUCTS_JSON
import com.woocommerce.commons.wear.DataParameters.SITE_JSON
import com.woocommerce.commons.wear.DataParameters.TIMESTAMP
import com.woocommerce.commons.wear.DataParameters.TOKEN
import com.woocommerce.commons.wear.DataParameters.TOTAL_REVENUE
import com.woocommerce.commons.wear.DataParameters.VISITORS_TOTAL
import com.woocommerce.commons.wear.DataPath
import com.woocommerce.commons.wear.DataPath.ORDERS_DATA
import com.woocommerce.commons.wear.DataPath.ORDER_PRODUCTS_DATA
import com.woocommerce.commons.wear.DataPath.SITE_DATA
import com.woocommerce.commons.wear.DataPath.STATS_DATA
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.OrdersForWearablesResult.Success
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.time.Instant
import javax.inject.Inject

class WearableConnectionRepository @Inject constructor(
    private val dataClient: DataClient,
    private val selectedSite: SelectedSite,
    private val accountStore: AccountStore,
    private val wooCommerceStore: WooCommerceStore,
    private val orderStore: WCOrderStore,
    private val getStats: GetWearableMyStoreStats,
    private val getOrderProducts: GetWearableOrderProducts,
    private val coroutineScope: CoroutineScope
) {
    private val gson by lazy { Gson() }

    fun sendSiteData(
        currentSite: SiteModel? = null
    ) {
        val site = currentSite ?: selectedSite.get()
        sendData(
            SITE_DATA,
            DataMap().apply {
                val siteJSON = gson.toJson(site)
                putString(SITE_JSON.value, siteJSON)
                putString(TOKEN.value, accountStore.accessToken.orEmpty())
            }
        )
    }

    fun sendStatsData() = coroutineScope.launch {
        val stats = getStats(selectedSite.get())
        val revenueTotals = stats?.revenue?.parseTotal()
        val ordersCount = revenueTotals?.ordersCount ?: 0
        val visitorsCount = stats?.visitors?.values?.sum() ?: 0
        val conversionRate = ordersCount convertedFrom visitorsCount
        val formattedTotalSales = wooCommerceStore.formatCurrencyForDisplay(
            amount = revenueTotals?.totalSales ?: 0.0,
            site = selectedSite.get(),
            currencyCode = null,
            applyDecimalFormatting = true
        )

        sendData(
            STATS_DATA,
            DataMap().apply {
                putString(TOTAL_REVENUE.value, formattedTotalSales)
                putInt(ORDERS_COUNT.value, ordersCount)
                putInt(VISITORS_TOTAL.value, visitorsCount)
                putString(CONVERSION_RATE.value, conversionRate)
            }
        )
    }

    fun sendOrdersData() = coroutineScope.launch {
        val orders = orderStore.fetchOrdersForWearables(
            site = selectedSite.get(),
            shouldStoreData = false
        ).run { this as? Success }?.orders ?: emptyList()

        sendData(
            ORDERS_DATA,
            DataMap().apply {
                putString(ORDERS_JSON.value, gson.toJson(orders))
            }
        )
    }

    fun sendOrderProductsData(message: MessageEvent) = coroutineScope.launch {
        val orderId = runCatching {
            message.data.toString(Charsets.UTF_8).toLong()
        }.getOrNull()

        val orderProductsJson = orderId
            ?.let { getOrderProducts(it) }
            ?.let { gson.toJson(it) }
            .orEmpty()

        sendData(
            ORDER_PRODUCTS_DATA,
            DataMap().apply {
                putLong(ORDER_ID.value, orderId ?: -1L)
                putString(ORDER_PRODUCTS_JSON.value, orderProductsJson)
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
