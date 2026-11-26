package com.woocommerce.android.ui.woopos.roles.pin

import com.woocommerce.android.ui.woopos.roles.lock.WooPosLocalRateLimiter
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import javax.inject.Inject

class WooPosClearLocalPINData @Inject constructor(
    private val localPINStore: WooPosLocalPINStore,
    private val rateLimiter: WooPosLocalRateLimiter,
) {
    suspend operator fun invoke() = withContext(NonCancellable) {
        rateLimiter.onLogout()
        localPINStore.clearAll().onFailure {
            WooLog.e(WooLog.T.POS, "Unable to clear local POS PINs")
        }
        rateLimiter.clearAllAttempts().onFailure {
            WooLog.e(WooLog.T.POS, "Unable to clear local POS PIN attempts")
        }
        Unit
    }
}
