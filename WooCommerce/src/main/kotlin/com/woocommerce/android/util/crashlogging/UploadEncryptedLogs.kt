package com.woocommerce.android.util.crashlogging

import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.store.EncryptedLogStore
import javax.inject.Inject

class UploadEncryptedLogs @Inject constructor(
    dispatchers: CoroutineDispatchers,
    private val encryptedLogStore: EncryptedLogStore
) {
    private val coroutineScope = CoroutineScope(dispatchers.io)

    operator fun invoke() {
        coroutineScope.launch {
            encryptedLogStore.uploadQueuedEncryptedLogs()
        }
    }
}
