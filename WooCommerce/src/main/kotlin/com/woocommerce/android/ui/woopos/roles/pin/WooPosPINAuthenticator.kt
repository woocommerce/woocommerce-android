package com.woocommerce.android.ui.woopos.roles.pin

import com.woocommerce.android.ui.woopos.roles.WooPosLocalRole
import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class WooPosPINAuthenticator @Inject constructor(
    private val store: WooPosLocalPINStore,
    private val dispatchers: CoroutineDispatchers,
) {
    // Check getPINs() separately for protection status; null here means no unique matching role.
    suspend fun authenticate(localSiteId: Int, pin: String): Result<WooPosLocalRole?> =
        withContext(dispatchers.computation) {
            if (!WooPosPINHash.isValidPIN(pin)) return@withContext Result.success(null)
            store.getPINs(localSiteId).mapCatching { pins ->
                pins.entries.filter { it.value.matches(pin) }.singleOrNull()?.key
            }
        }
}
