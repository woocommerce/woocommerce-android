package com.woocommerce.android.util.crashlogging

import com.automattic.android.tracks.crashlogging.EventLevel
import com.automattic.android.tracks.crashlogging.EventLevel.FATAL
import com.automattic.encryptedlogging.EncryptedLogging
import com.woocommerce.android.di.AppCoroutineScope
import com.woocommerce.android.tools.NetworkStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

class EnqueueSendingEncryptedLogs @Inject constructor(
    private val encryptedLogging: EncryptedLogging,
    private val encryptedLogsFileProvider: EncryptedLogsFileProvider,
    private val networkStatus: NetworkStatus,
    @AppCoroutineScope private val appCoroutineScope: CoroutineScope,
) {
    operator fun invoke(
        uuid: String,
        eventLevel: EventLevel
    ) {
        if (eventLevel == FATAL) {
            val logs = runBlocking { encryptedLogsFileProvider.provide() }
            encryptedLogging.enqueueSendingEncryptedLogs(
                uuid = uuid,
                file = logs,
                shouldUploadImmediately = false
            )
        } else {
            appCoroutineScope.launch {
                encryptedLogging.enqueueSendingEncryptedLogs(
                    uuid = uuid,
                    file = encryptedLogsFileProvider.provide(),
                    shouldUploadImmediately = networkStatus.isConnected()
                )
            }
        }
    }
}
