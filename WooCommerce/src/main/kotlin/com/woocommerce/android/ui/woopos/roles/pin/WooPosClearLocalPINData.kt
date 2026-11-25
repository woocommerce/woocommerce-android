package com.woocommerce.android.ui.woopos.roles.pin

import com.woocommerce.android.ui.woopos.roles.lock.WooPosLocalRateLimiter
import com.woocommerce.android.ui.woopos.roles.lock.WooPosLockPreferences
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import javax.inject.Inject

class WooPosClearLocalPINData @Inject constructor(
    private val localPINStore: WooPosLocalPINStore,
    private val rateLimiter: WooPosLocalRateLimiter,
    private val lockPreferences: WooPosLockPreferences,
) {
    suspend operator fun invoke() = withContext(NonCancellable) {
        rateLimiter.onLogout()
        lockPreferences.clearAll().onFailure {
            WooLog.e(WooLog.T.POS, "Unable to clear POS restoration state")
        }
        localPINStore.clearAll().onFailure {
            WooLog.e(WooLog.T.POS, "Unable to clear local POS PINs")
        }
        rateLimiter.clearAllAttempts().onFailure {
            WooLog.e(WooLog.T.POS, "Unable to clear local POS PIN attempts")
        }
        Unit
    }
}
