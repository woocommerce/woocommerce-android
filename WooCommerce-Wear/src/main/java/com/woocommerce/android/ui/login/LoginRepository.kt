package com.woocommerce.android.ui.login

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.MessageClient
import com.woocommerce.android.datastore.DataStoreQualifier
import com.woocommerce.android.datastore.DataStoreType
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class LoginRepository @Inject constructor(
    @DataStoreQualifier(DataStoreType.LOGIN) private val loginDataStore: DataStore<Preferences>,
    private val capabilityClient: CapabilityClient,
    private val messageClient: MessageClient,
    private val coroutineScope: CoroutineScope
) {
    fun isUserLoggedIn() = loginDataStore.data
        .map { it[stringPreferencesKey(generateStoreConfigKey())].isNullOrEmpty().not() }

    suspend fun sendMessageToAllNodes(
        path: String,
        data: ByteArray = byteArrayOf()
    ) {
        fetchReachableNodes()
            .map { (node, _) ->
                coroutineScope.async {
                    messageClient.sendMessage(node.id, path, data)
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

    private fun generateStoreConfigKey(): String {
        return "store_config_key"
    }
}
