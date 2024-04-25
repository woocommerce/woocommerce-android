package com.woocommerce.android.wear

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.woocommerce.commons.wear.MessagePath.REQUEST_SITE
import com.woocommerce.commons.wear.MessagePath.REQUEST_TOKEN
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class WearableConnectionService : WearableListenerService() {

    @Inject
    lateinit var connRepository: WearableConnectionRepository

    override fun onMessageReceived(message: MessageEvent) {
        super.onMessageReceived(message)
        when (message.path) {
            REQUEST_TOKEN.value -> connRepository.sendTokenData()
            REQUEST_SITE.value -> connRepository.sendSiteData()
        }
    }
}
