package com.woocommerce.android.sync

import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.MessageClient
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.tasks.await

class WearableConnectionRepository @Inject constructor(
    private val capabilityClient: CapabilityClient,
    private val messageClient: MessageClient,
    private val coroutineScope: CoroutineScope
) {
    suspend fun sendMessageToAllNodes(
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

    enum class MessagePath(val value: String) {
        START_AUTH("/start-auth")
    }

    companion object {
        const val WOO_MOBILE_CAPABILITY = "woo_mobile"
    }
}
