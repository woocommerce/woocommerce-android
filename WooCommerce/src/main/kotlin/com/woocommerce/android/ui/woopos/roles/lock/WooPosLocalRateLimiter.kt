package com.woocommerce.android.ui.woopos.roles.lock

import com.woocommerce.android.ui.woopos.roles.WooPosCapability
import com.woocommerce.android.ui.woopos.roles.WooPosLocalRole
import com.woocommerce.android.ui.woopos.roles.pin.WooPosLocalPINStore
import com.woocommerce.android.ui.woopos.roles.pin.WooPosPINAuthenticator
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Unlock and action approval must both use this service, rather than authenticating PINs directly. */
@Singleton
class WooPosLocalRateLimiter @Inject internal constructor(
    private val store: WooPosLocalPINStore,
    private val authenticator: WooPosPINAuthenticator,
    private val storage: WooPosPINAttemptStorage,
    private val clock: WooPosPINClock,
    private val recovery: WooPosPINRecovery,
    private val dispatchers: CoroutineDispatchers,
) {
    private val mutex = Mutex()

    suspend fun protection(localSiteId: Int): Result<Boolean> = store.getPINs(localSiteId).map { it.isNotEmpty() }

    suspend fun remainingSeconds(localSiteId: Int): Result<Long> = operation {
        val (attempts, time) = load(localSiteId)
        seconds(attempts.remainingMillis(time))
    }

    suspend fun authenticate(
        localSiteId: Int,
        pin: String,
        requiredCapability: WooPosCapability? = null,
    ): WooPosLocalRateLimiterResult = operation {
        val pins = store.getPINs(localSiteId).getOrThrow()
        if (pins.isEmpty()) return@operation WooPosLocalRateLimiterResult.ProtectionOff
        val (attempts, time) = load(localSiteId)
        val wait = seconds(attempts.remainingMillis(time))
        if (wait > 0) return@operation WooPosLocalRateLimiterResult.Waiting(wait)
        // Persist before comparing: a process kill must not create an unrecorded PIN oracle.
        val chargedAttempt = attempts.failed(time)
        storage.write(localSiteId, chargedAttempt)
        val authenticated = authenticator.authenticate(pins, pin)
        if (authenticated.isFailure) {
            storage.write(localSiteId, attempts)
            return@operation WooPosLocalRateLimiterResult.Unavailable
        }
        val role = authenticated.getOrThrow()
        when {
            role == null || (requiredCapability != null && !role.hasCapability(requiredCapability)) -> {
                WooPosLocalRateLimiterResult.Incorrect(seconds(chargedAttempt.waitMillis))
            }
            else -> {
                val next = if (role.hasCapability(WooPosCapability.MANAGE_STORE_SETTINGS)) {
                    WooPosPINAttemptState()
                } else {
                    attempts
                }
                storage.write(localSiteId, next)
                WooPosLocalRateLimiterResult.Authenticated(role)
            }
        }
    }.getOrDefault(WooPosLocalRateLimiterResult.Unavailable)

    suspend fun recover(localSiteId: Int): WooPosLocalRateLimiterResult = operation {
        val role = recovery.authenticate(localSiteId).getOrThrow()
        check(role.hasCapability(WooPosCapability.MANAGE_STORE_SETTINGS))
        storage.write(localSiteId, WooPosPINAttemptState())
        WooPosLocalRateLimiterResult.Authenticated(role)
    }.getOrDefault(WooPosLocalRateLimiterResult.RecoveryUnavailable)

    suspend fun clearAllAttempts(): Result<Unit> = operation { storage.clearAll() }

    private fun load(localSiteId: Int): Pair<WooPosPINAttemptState, WooPosPINTime> {
        val time = clock.now()
        val stored = storage.read(localSiteId)
        val current = stored.at(time)
        if (current != stored) storage.write(localSiteId, current)
        return current to time
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun <T> operation(block: suspend () -> T): Result<T> =
        withContext(dispatchers.io + NonCancellable) {
            mutex.withLock {
                try {
                    Result.success(block())
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    WooLog.e(WooLog.T.POS, "Local PIN access operation failed: ${e.javaClass.simpleName}")
                    Result.failure(e)
                }
            }
        }

    private fun seconds(millis: Long): Long = (millis + MILLIS_PER_SECOND - 1) / MILLIS_PER_SECOND

    companion object {
        private const val MILLIS_PER_SECOND = 1000L
    }
}

sealed interface WooPosLocalRateLimiterResult {
    data class Authenticated(val role: WooPosLocalRole) : WooPosLocalRateLimiterResult
    data class Incorrect(val remainingSeconds: Long) : WooPosLocalRateLimiterResult
    data class Waiting(val remainingSeconds: Long) : WooPosLocalRateLimiterResult
    data object ProtectionOff : WooPosLocalRateLimiterResult
    data object Unavailable : WooPosLocalRateLimiterResult
    data object RecoveryUnavailable : WooPosLocalRateLimiterResult
}
