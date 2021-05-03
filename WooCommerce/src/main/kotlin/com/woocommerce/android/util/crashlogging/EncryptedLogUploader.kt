package com.woocommerce.android.util.crashlogging

import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.EncryptedLogActionBuilder
import org.wordpress.android.fluxc.store.EncryptedLogStore
import org.wordpress.android.fluxc.store.EncryptedLogStore.UploadEncryptedLogPayload
import org.wordpress.android.util.helpers.logfile.LogFileProviderInterface
import javax.inject.Inject

class EncryptedLogUploader @Inject constructor(
    dispatchers: CoroutineDispatchers,
    private val eventBusDispatcher: Dispatcher,
    private val encryptedLogStore: EncryptedLogStore,
    private val logFileProvider: LogFileProviderInterface,
    private val networkStatus: NetworkStatus
) {

    private val coroutineScope = CoroutineScope(dispatchers.io)

    init {
        eventBusDispatcher.register(this)
        eventBusDispatcher.dispatch(EncryptedLogActionBuilder.newResetUploadStatesAction())

        coroutineScope.launch {
            encryptedLogStore.uploadQueuedEncryptedLogs()
        }
    }

    fun upload(
        uuid: String,
        shouldStartUploadImmediately: Boolean
    ) {
        logFileProvider.getLogFiles().lastOrNull()?.let { logFile ->
            val payload = UploadEncryptedLogPayload(
                uuid = uuid,
                file = logFile,
                shouldStartUploadImmediately = shouldStartUploadImmediately && networkStatus.isConnected()
            )
            eventBusDispatcher.dispatch(EncryptedLogActionBuilder.newUploadLogAction(payload))
        }
    }
}
