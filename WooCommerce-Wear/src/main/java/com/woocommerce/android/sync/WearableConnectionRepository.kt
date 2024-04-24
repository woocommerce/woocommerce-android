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
    ) {
        fetchReachableNodes()
            .map { (node, _) ->
                coroutineScope.async {
                    messageClient.sendMessage(node.id, path.value, data)
                }
            }.awaitAll()
    }

    private suspend fun fetchReachableNodes() = capabilityClient
        .getAllCapabilities(CapabilityClient.FILTER_REACHABLE)
        .await()
        .flatMap { (capability, capabilityInfo) ->
            capabilityInfo.nodes.map { it to capability }
        }
        .groupBy(
            keySelector = { it.first },
            valueTransform = { it.second }
        )
        .mapValues { it.value.toSet() }

    enum class MessagePath(val value: String) {
        START_AUTH("/start-auth")
    }
}
