package com.woocommerce.android.phone

import android.util.Log
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.DataItem
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageClient
import com.woocommerce.android.ui.login.LoginRepository
import com.woocommerce.commons.wear.DataPath
import com.woocommerce.commons.wear.MessagePath
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class PhoneConnectionRepository @Inject constructor(
    private val loginRepository: LoginRepository,
    private val capabilityClient: CapabilityClient,
    private val messageClient: MessageClient,
    private val coroutineScope: CoroutineScope
) {
    fun handleReceivedData(dataItem: DataItem) {
        when (dataItem.uri.path) {
            DataPath.SITE_DATA.value -> handleAuthenticationData(dataItem)
            else -> Log.d(TAG, "Unknown path data received")
        }
    }

    suspend fun sendMessage(
        path: MessagePath,
        data: ByteArray = byteArrayOf()
    ): Result<Unit> = fetchReachableNodes()
        .takeIf { it.isNotEmpty() }
        ?.map { coroutineScope.async { messageClient.sendMessage(it.id, path.value, data) } }
        ?.awaitAll()
        ?.let { Result.success(Unit) }
        ?: Result.failure(Exception("No reachable nodes found"))

    private suspend fun fetchReachableNodes() = capabilityClient
        .getAllCapabilities(CapabilityClient.FILTER_REACHABLE)
        .await()
        .flatMap { (capability, capabilityInfo) ->
            capabilityInfo.nodes.map { it to capability }
        }.filter { (_, capabilityInfo) ->
            WOO_MOBILE_CAPABILITY in capabilityInfo
        }.map { (node, _) -> node }

    private fun handleAuthenticationData(dataItem: DataItem) {
        val dataMap = DataMapItem.fromDataItem(dataItem).dataMap
        coroutineScope.launch { loginRepository.receiveStoreData(dataMap) }
    }

    companion object {
        const val TAG = "PhoneConnectionRepository"
        const val WOO_MOBILE_CAPABILITY = "woo_mobile"
    }
}
