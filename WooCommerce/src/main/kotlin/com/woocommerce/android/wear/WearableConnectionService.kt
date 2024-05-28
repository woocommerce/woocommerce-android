package com.woocommerce.android.wear

import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.woocommerce.commons.DataPath.ANALYTICS_DATA
import com.woocommerce.commons.MessagePath.REQUEST_ORDERS
import com.woocommerce.commons.MessagePath.REQUEST_ORDER_PRODUCTS
import com.woocommerce.commons.MessagePath.REQUEST_SITE
import com.woocommerce.commons.MessagePath.REQUEST_STATS
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class WearableConnectionService : WearableListenerService() {

    @Inject
    lateinit var wearableConnectionRepository: WearableConnectionRepository

    override fun onMessageReceived(message: MessageEvent) {
        super.onMessageReceived(message)
        when (message.path) {
            REQUEST_SITE.value -> wearableConnectionRepository.sendSiteData()
            REQUEST_STATS.value -> wearableConnectionRepository.sendStatsData()
            REQUEST_ORDERS.value -> wearableConnectionRepository.sendOrdersData()
            REQUEST_ORDER_PRODUCTS.value -> wearableConnectionRepository.sendOrderProductsData(message)
        }
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        super.onDataChanged(dataEvents)
        dataEvents
            .filter { it.type == DataEvent.TYPE_CHANGED }
            .forEach {
                val dataItem = it.dataItem
                val dataPath = it.dataItem.uri.path
                when (dataPath) {
                    ANALYTICS_DATA.value -> {
                        val dataMap = DataMapItem.fromDataItem(dataItem).dataMap
                        wearableConnectionRepository.receiveAnalyticsFromWear(dataMap)
                    }
                }
            }
    }
}
