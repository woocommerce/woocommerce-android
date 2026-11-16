package com.woocommerce.android.ui.woopos.roles.lock

import com.woocommerce.android.ui.woopos.roles.WooPosLocalRole
import javax.inject.Inject

/** Returns an Admin operator only after fresh store-account authentication in WOOMOB-4606. */
interface WooPosPINRecovery {
    suspend fun authenticate(localSiteId: Int): Result<WooPosLocalRole>
}

class WooPosPendingPINRecovery @Inject constructor() : WooPosPINRecovery {
    override suspend fun authenticate(localSiteId: Int): Result<WooPosLocalRole> =
        Result.failure(UnsupportedOperationException("PIN recovery is not integrated"))
}
