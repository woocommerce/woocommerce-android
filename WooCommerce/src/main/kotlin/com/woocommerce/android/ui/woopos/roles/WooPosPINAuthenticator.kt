package com.woocommerce.android.ui.woopos.roles

import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class WooPosPINAuthenticator @Inject constructor(
    private val source: WooPosPINCredentialSource,
    private val dispatchers: CoroutineDispatchers,
) {
    // Check credentials() separately for protection status; null here means no unique matching operator.
    suspend fun authenticate(localSiteId: Int, pin: String): Result<WooPosOperator?> =
        withContext(dispatchers.computation) {
            if (!WooPosPINHash.isValidPIN(pin)) return@withContext Result.success(null)
            source.credentials(localSiteId).mapCatching { credentials ->
                val matches = credentials.filter { it.pinHash.matches(pin) }
                matches.singleOrNull()?.operator
            }
        }
}
