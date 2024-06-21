package com.woocommerce.android.wear.phone

import android.util.Log
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataItem
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.PutDataMapRequest
import com.woocommerce.android.wear.ui.login.LoginRepository
import com.woocommerce.android.wear.ui.orders.OrdersRepository
import com.woocommerce.android.wear.ui.stats.datasource.StatsRepository
import com.woocommerce.commons.DataParameters
import com.woocommerce.commons.DataPath
import com.woocommerce.commons.DataPath.ORDERS_DATA
import com.woocommerce.commons.DataPath.ORDER_PRODUCTS_DATA
import com.woocommerce.commons.DataPath.SITE_DATA
import com.woocommerce.commons.DataPath.STATS_DATA
import com.woocommerce.commons.MessagePath
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.Instant
import javax.inject.Inject

class PhoneConnectionRepository @Inject constructor(
    private val loginRepository: LoginRepository,
    private val statsRepository: StatsRepository,
    private val ordersRepository: OrdersRepository,
    private val capabilityClient: CapabilityClient,
    private val dataClient: DataClient,
    private val messageClient: MessageClient,
    private val coroutineScope: CoroutineScope
) {
    suspend fun isPhoneConnectionAvailable() = fetchReachableNodes().isNotEmpty()

    fun handleReceivedData(dataItem: DataItem) {
        val dataPath = dataItem.uri.path
        val dataMap = DataMapItem.fromDataItem(dataItem).dataMap

        coroutineScope.launch {
            when (dataPath) {
                SITE_DATA.value -> loginRepository.receiveStoreDataFromPhone(dataMap)
                STATS_DATA.value -> statsRepository.receiveStatsDataFromPhone(dataMap)
                ORDERS_DATA.value -> ordersRepository.receiveOrdersDataFromPhone(dataMap)
                ORDER_PRODUCTS_DATA.value -> ordersRepository.receiveOrderProductsDataFromPhone(dataMap)
                else -> Log.d(TAG, "Unknown path data received")
            }
        }
    }

    suspend fun sendMessage(
        path: MessagePath,
        data: ByteArray = byteArrayOf()
    ) = fetchReachableNodes()
        .takeIf { it.isNotEmpty() }
        ?.map { coroutineScope.async { messageClient.sendMessage(it.id, path.value, data) } }
        ?.awaitAll()
        ?.let { Result.success(Unit) }
        ?: Result.failure(Exception(MESSAGE_FAILURE_EXCEPTION))

    fun sendData(
        dataPath: DataPath,
        data: DataMap
    ) {
        PutDataMapRequest
            .create(dataPath.value)
            .apply {
                dataMap.putAll(data)
                dataMap.putLong(DataParameters.TIMESTAMP.value, Instant.now().epochSecond)
            }.asPutDataRequest().setUrgent()
            .let { dataClient.putDataItem(it) }
    }

    private suspend fun fetchReachableNodes() = capabilityClient
        .getAllCapabilities(CapabilityClient.FILTER_REACHABLE)
        .await()
        .flatMap { (capability, capabilityInfo) ->
            capabilityInfo.nodes.map { it to capability }
        }.filter { (_, capabilityInfo) ->
            WOO_MOBILE_CAPABILITY in capabilityInfo
        }.map { (node, _) -> node }

    companion object {
        const val TAG = "PhoneConnectionRepository"
        const val WOO_MOBILE_CAPABILITY = "woo_mobile"

        const val MESSAGE_FAILURE_EXCEPTION = "No reachable nodes found"
    }
}
