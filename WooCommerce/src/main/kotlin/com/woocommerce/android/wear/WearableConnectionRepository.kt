package com.woocommerce.android.wear

import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.PutDataMapRequest
import com.woocommerce.commons.wear.DataParameters
import com.woocommerce.commons.wear.DataParameters.TIMESTAMP
import com.woocommerce.commons.wear.DataParameters.TOKEN
import com.woocommerce.commons.wear.DataPath
import com.woocommerce.commons.wear.DataPath.TOKEN_DATA
import java.time.Instant
import javax.inject.Inject
import org.wordpress.android.fluxc.store.AccountStore

class WearableConnectionRepository @Inject constructor(
    private val dataClient: DataClient,
    private val accountStore: AccountStore
) {
    fun sendTokenData() {
        PutDataMapRequest
            .create(TOKEN_DATA.value)
            .apply {
                dataMap.putString(TOKEN.value, accountStore.accessToken.orEmpty())
                dataMap.putLong(TIMESTAMP.value, Instant.now().epochSecond)
            }.asPutDataRequest().setUrgent()
            .let { dataClient.putDataItem(it) }
    }
}
