package com.woocommerce.android.util.crashlogging

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
        shouldStartUploadImmediately: Boolean
    ) {
        val payload = UploadEncryptedLogPayload(
            uuid = uuid,
            file = wooLogFileProvider.provide(),
            shouldStartUploadImmediately = shouldStartUploadImmediately && networkStatus.isConnected()
        )
        eventBusDispatcher.dispatch(EncryptedLogActionBuilder.newUploadLogAction(payload))
    }
}
