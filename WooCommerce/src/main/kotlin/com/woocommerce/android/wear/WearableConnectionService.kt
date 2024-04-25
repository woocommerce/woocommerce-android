package com.woocommerce.android.wear

import android.util.Log
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.commons.wear.MessagePath.REQUEST_SITE
import com.woocommerce.commons.wear.MessagePath.REQUEST_TOKEN
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@AndroidEntryPoint
class WearableConnectionService : WearableListenerService() {

    @Inject
    lateinit var connRepository: WearableConnectionRepository

    @Inject
    lateinit var coroutineScope: CoroutineScope

    @Inject
    lateinit var selectedSite: SelectedSite

    override fun onCreate() {
        super.onCreate()
        selectedSite.observe()
            .filterNotNull()
            .distinctUntilChanged()
            .onEach { connRepository.sendSiteData() }
            .launchIn(coroutineScope)
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        super.onDataChanged(dataEvents)
        Log.d(TAG, "onDataChanged")
    }

    override fun onMessageReceived(message: MessageEvent) {
        super.onMessageReceived(message)
        when (message.path) {
            REQUEST_TOKEN.value -> connRepository.sendTokenData()
            REQUEST_SITE.value -> connRepository.sendSiteData()
        }
    }

    companion object {
        private const val TAG = "WearableConnectionService"
    }
}
