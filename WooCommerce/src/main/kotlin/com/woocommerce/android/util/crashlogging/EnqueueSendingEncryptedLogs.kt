package com.woocommerce.android.util.crashlogging

import com.automattic.android.tracks.crashlogging.EventLevel
import com.automattic.android.tracks.crashlogging.EventLevel.FATAL
import com.woocommerce.android.tools.NetworkStatus
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.EncryptedLogActionBuilder
import org.wordpress.android.fluxc.store.EncryptedLogStore.UploadEncryptedLogPayload
import javax.inject.Inject

class EnqueueSendingEncryptedLogs @Inject constructor(
    private val eventBusDispatcher: Dispatcher,
    private val wooLogFileProvider: WooLogFileProvider,
    private val networkStatus: NetworkStatus
) {
    operator fun invoke(
        uuid: String,
        eventLevel: EventLevel
    ) {
        val payload = UploadEncryptedLogPayload(
            uuid = uuid,
            file = wooLogFileProvider.provide(),
            shouldStartUploadImmediately = eventLevel != FATAL && networkStatus.isConnected()
        )
        eventBusDispatcher.dispatch(EncryptedLogActionBuilder.newUploadLogAction(payload))
    }
}
