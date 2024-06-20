package com.woocommerce.android.wear

import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.analytics.toAnalyticsEvent
import com.woocommerce.android.extensions.convertedFrom
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.commons.DataParameters.ANALYTICS_PARAMETERS
import com.woocommerce.commons.DataParameters.ANALYTICS_TRACK
import com.woocommerce.commons.DataParameters.CONVERSION_RATE
import com.woocommerce.commons.DataParameters.ORDERS_COUNT
import com.woocommerce.commons.DataParameters.ORDERS_JSON
import com.woocommerce.commons.DataParameters.ORDER_ID
import com.woocommerce.commons.DataParameters.ORDER_PRODUCTS_JSON
import com.woocommerce.commons.DataParameters.SITE_ID
import com.woocommerce.commons.DataParameters.SITE_JSON
import com.woocommerce.commons.DataParameters.TIMESTAMP
import com.woocommerce.commons.DataParameters.TOKEN
import com.woocommerce.commons.DataParameters.TOTAL_REVENUE
import com.woocommerce.commons.DataParameters.VISITORS_TOTAL
import com.woocommerce.commons.DataPath
import com.woocommerce.commons.DataPath.ORDERS_DATA
import com.woocommerce.commons.DataPath.ORDER_PRODUCTS_DATA
import com.woocommerce.commons.DataPath.SITE_DATA
import com.woocommerce.commons.DataPath.STATS_DATA
import com.woocommerce.commons.WearAnalyticsEvent
import com.woocommerce.commons.WearOrder
import com.woocommerce.commons.WearOrderAddress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.WCWearableStore
import org.wordpress.android.fluxc.store.WCWearableStore.OrdersForWearablesResult.Success
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.time.Instant
import javax.inject.Inject

class WearableConnectionRepository @Inject constructor(
    private val dataClient: DataClient,
    private val selectedSite: SelectedSite,
    private val accountStore: AccountStore,
    private val wooCommerceStore: WooCommerceStore,
    private val wearableStore: WCWearableStore,
    private val getStats: GetWearableMyStoreStats,
    private val getOrderProducts: GetWearableOrderProducts,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
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
        val fetchedOrders = wearableStore.fetchOrders(
            site = selectedSite.get(),
            shouldStoreData = false
        ).run { this as? Success }?.orders ?: emptyList()

        val orders = fetchedOrders.map {
            val orderAddress = it.getBillingAddress().let { address ->
                WearOrderAddress(
                    email = address.email,
                    firstName = address.firstName,
                    lastName = address.lastName,
                    company = address.company,
                    address1 = address.address1,
                    address2 = address.address2,
                    city = address.city,
                    state = address.state,
                    postcode = address.postcode,
                    country = address.country,
                    phone = address.phone
                )
            }

            WearOrder(
                localSiteId = it.localSiteId.value,
                id = it.orderId,
                number = it.number,
                date = it.dateCreated,
                status = it.status,
                total = it.total,
                billingFirstName = it.billingFirstName,
                billingLastName = it.billingLastName,
                address = orderAddress,
                lineItemsJson = it.lineItems
            )
        }

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

    fun receiveAnalyticsFromWear(dataMap: DataMap) {
        val typeToken = object : TypeToken<Map<String, String>>() {}.type
        val parameters: Map<String, String> = dataMap.getString(ANALYTICS_PARAMETERS.value)
            ?.takeIf { it.isNotEmpty() }
            ?.let { gson.fromJson(it, typeToken) }
            ?: emptyMap()

        dataMap.getString(ANALYTICS_TRACK.value)
            ?.takeIf { it.isNotEmpty() }
            ?.let { runCatching { WearAnalyticsEvent.valueOf(it) }.getOrNull() }
            ?.toAnalyticsEvent()
            ?.let { analyticsTrackerWrapper.track(it, parameters) }
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
                dataMap.putLong(SITE_ID.value, selectedSite.getOrNull()?.siteId ?: 0L)
            }.asPutDataRequest().setUrgent()
            .let { dataClient.putDataItem(it) }
    }
}
