package com.woocommerce.android.util.crashlogging

import com.automattic.android.tracks.crashlogging.EventLevel
import com.automattic.android.tracks.crashlogging.EventLevel.FATAL
import com.automattic.encryptedlogging.EncryptedLogging
import com.woocommerce.android.tools.NetworkStatus
import javax.inject.Inject

class EnqueueSendingEncryptedLogs @Inject constructor(
    private val encryptedLogging: EncryptedLogging,
    private val encryptedLogsFileProvider: EncryptedLogsFileProvider,
    private val networkStatus: NetworkStatus
) {
    operator fun invoke(
        uuid: String,
        eventLevel: EventLevel
    ) {
        encryptedLogging.enqueueSendingEncryptedLogs(
            uuid = uuid,
            file = encryptedLogsFileProvider.provide(),
            shouldUploadImmediately = eventLevel != FATAL && networkStatus.isConnected()
        )
    }
}
