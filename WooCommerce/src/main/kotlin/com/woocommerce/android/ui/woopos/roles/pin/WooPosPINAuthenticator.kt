package com.woocommerce.android.ui.woopos.roles.pin

import com.woocommerce.android.ui.woopos.roles.WooPosLocalRole
import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class WooPosPINAuthenticator @Inject constructor(
    private val dispatchers: CoroutineDispatchers,
) {
    // The access limiter must compare the same snapshot it checked before persisting the attempt.
    internal suspend fun authenticate(
        pins: Map<WooPosLocalRole, WooPosPINHash>,
        pin: String,
    ): Result<WooPosLocalRole?> = withContext(dispatchers.computation) {
        if (!WooPosPINHash.isValidPIN(pin)) return@withContext Result.success(null)
        runCatching { pins.entries.filter { it.value.matches(pin) }.singleOrNull()?.key }
    }
}
