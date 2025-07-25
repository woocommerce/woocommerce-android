package com.woocommerce.android.util.crashlogging

import com.automattic.encryptedlogging.EncryptedLogging
import javax.inject.Inject

class UploadEncryptedLogs @Inject constructor(
    private val encryptedLogging: EncryptedLogging
) {
    operator fun invoke() {
        encryptedLogging.resetUploadStates()
        encryptedLogging.uploadEncryptedLogs()
    }
}
