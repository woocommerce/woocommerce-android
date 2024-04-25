package com.woocommerce.android.wear

import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.gson.Gson
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.commons.wear.DataParameters.SITE_ID
import com.woocommerce.commons.wear.DataParameters.SITE_JSON
import com.woocommerce.commons.wear.DataParameters.TIMESTAMP
import com.woocommerce.commons.wear.DataParameters.TOKEN
import com.woocommerce.commons.wear.DataPath
import com.woocommerce.commons.wear.DataPath.SITE_DATA
import com.woocommerce.commons.wear.DataPath.TOKEN_DATA
import org.wordpress.android.fluxc.store.AccountStore
import java.time.Instant
import javax.inject.Inject

class WearableConnectionRepository @Inject constructor(
    private val dataClient: DataClient,
    private val accountStore: AccountStore,
    private val selectedSite: SelectedSite
) {
    private val gson by lazy { Gson() }

    fun sendTokenData() {
        sendData(
            TOKEN_DATA,
            DataMap().apply {
                val siteJSON = gson.toJson(selectedSite.get())
                putString(SITE_JSON.value, siteJSON)
                putString(SITE_ID.value, selectedSite.get().id.toString())
                putString(TOKEN.value, accountStore.accessToken.orEmpty())
                putLong(TIMESTAMP.value, Instant.now().epochSecond)
            }
        )
    }

    fun sendSiteData() {
        sendData(
            SITE_DATA,
            DataMap().apply {
                val siteJSON = gson.toJson(selectedSite.get())
                putString(SITE_JSON.value, siteJSON)
                putLong(TIMESTAMP.value, Instant.now().epochSecond)
            }
        )
    }

    private fun sendData(
        dataPath: DataPath,
        data: DataMap
    ) {
        PutDataMapRequest
            .create(dataPath.value)
            .apply { dataMap.putAll(data) }
            .asPutDataRequest().setUrgent()
            .let { dataClient.putDataItem(it) }
    }
}
