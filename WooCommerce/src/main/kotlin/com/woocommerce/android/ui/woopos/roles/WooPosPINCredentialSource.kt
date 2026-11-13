package com.woocommerce.android.ui.woopos.roles

import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

interface WooPosPINCredentialSource {
    // Use the database's local site ID, including for stores without a WordPress.com site ID.
    // Empty credentials mean protection is off; storage failure must keep the caller locked.
    suspend fun credentials(localSiteId: Int): Result<List<WooPosPINCredential>>
}

class WooPosPINCredential(val operator: WooPosOperator, val pinHash: WooPosPINHash)

class WooPosPINAuthenticator @Inject constructor(
    private val source: WooPosPINCredentialSource,
    private val dispatchers: CoroutineDispatchers,
) {
    // Check credentials() separately for protection status; null here means no unique matching operator.
    suspend fun authenticate(localSiteId: Int, pin: String): Result<WooPosOperator?> =
        source.credentials(localSiteId).fold(
            onSuccess = { authenticate(it, pin) },
            onFailure = { Result.failure(it) },
        )

    // Authenticate one source snapshot so protection status and matching cannot use different records.
    suspend fun authenticate(credentials: List<WooPosPINCredential>, pin: String): Result<WooPosOperator?> =
        withContext(dispatchers.computation) {
            runCatching {
                if (!WooPosPINHash.isValidPIN(pin)) {
                    null
                } else {
                    credentials.filter { it.pinHash.matches(pin) }.singleOrNull()?.operator
                }
            }
        }
}
