package com.woocommerce.android.ui.woopos.roles.lock

import com.woocommerce.android.ui.woopos.roles.WooPosLocalRole
import javax.inject.Inject

/** Returns an Admin role only after fresh store-account authentication, not a cached session (WOOMOB-4606). */
interface WooPosPINRecovery {
    suspend fun authenticate(localSiteId: Int): Result<WooPosLocalRole>
}

class WooPosPendingPINRecovery @Inject constructor() : WooPosPINRecovery {
    override suspend fun authenticate(localSiteId: Int): Result<WooPosLocalRole> =
        Result.failure(UnsupportedOperationException("PIN recovery is not integrated"))
}
