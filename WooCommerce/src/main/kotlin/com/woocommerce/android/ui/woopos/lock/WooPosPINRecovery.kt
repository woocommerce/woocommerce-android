package com.woocommerce.android.ui.woopos.lock

import com.woocommerce.android.ui.woopos.roles.WooPosOperator
import javax.inject.Inject

/** Returns an Admin operator only after fresh store-account authentication in WOOMOB-4606. */
interface WooPosPINRecovery {
    suspend fun authenticate(localSiteId: Int): Result<WooPosOperator>
}

class WooPosPendingPINRecovery @Inject constructor() : WooPosPINRecovery {
    override suspend fun authenticate(localSiteId: Int): Result<WooPosOperator> =
        Result.failure(UnsupportedOperationException("PIN recovery is not integrated"))
}
