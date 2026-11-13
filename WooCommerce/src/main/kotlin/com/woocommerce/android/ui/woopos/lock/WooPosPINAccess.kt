package com.woocommerce.android.ui.woopos.lock

import com.woocommerce.android.ui.woopos.roles.WooPosOperator
import com.woocommerce.android.ui.woopos.roles.WooPosPINAuthenticator
import com.woocommerce.android.ui.woopos.roles.WooPosPINCredentialSource
import com.woocommerce.android.ui.woopos.roles.WooPosPermission
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
class WooPosPINAccess @Inject internal constructor(
    private val source: WooPosPINCredentialSource,
    private val authenticator: WooPosPINAuthenticator,
    private val storage: WooPosPINAttemptStorage,
    private val clock: WooPosPINClock,
    private val recovery: WooPosPINRecovery,
    private val dispatchers: CoroutineDispatchers,
) {
    private val mutex = Mutex()

    suspend fun protection(localSiteId: Int): Result<Boolean> = source.credentials(localSiteId).map { it.isNotEmpty() }

    suspend fun remainingSeconds(localSiteId: Int): Result<Long> = operation {
        val (attempts, time) = load(localSiteId)
        seconds(attempts.remainingMillis(time))
    }

    suspend fun authenticate(
        localSiteId: Int,
        pin: String,
        permission: WooPosPermission? = null,
    ): WooPosPINAccessResult = operation {
        val credentials = source.credentials(localSiteId).getOrThrow()
        if (credentials.isEmpty()) return@operation WooPosPINAccessResult.ProtectionOff
        val (attempts, time) = load(localSiteId)
        val wait = seconds(attempts.remainingMillis(time))
        if (wait > 0) return@operation WooPosPINAccessResult.Waiting(wait)
        // Persist before comparing: a process kill must not create an unrecorded PIN oracle.
        val failed = attempts.failed(clock.now())
        storage.write(localSiteId, failed)
        val authenticated = authenticator.authenticate(credentials, pin)
        if (authenticated.isFailure) {
            storage.write(localSiteId, attempts)
            return@operation WooPosPINAccessResult.Unavailable
        }
        val operator = authenticated.getOrThrow()
        when {
            operator == null || (permission != null && !operator.allows(permission)) -> {
                WooPosPINAccessResult.Incorrect(seconds(failed.waitMillis))
            }
            else -> {
                val next = if (operator.allows(WooPosPermission.MANAGE_STORE_SETTINGS)) {
                    WooPosPINAttemptState()
                } else {
                    attempts
                }
                storage.write(localSiteId, next)
                WooPosPINAccessResult.Authenticated(operator)
            }
        }
    }.getOrDefault(WooPosPINAccessResult.Unavailable)

    suspend fun recover(localSiteId: Int): WooPosPINAccessResult = operation {
        // WOOMOB-4606 supplies fresh account authentication. A stored app session is not recovery proof.
        val operator = recovery.authenticate(localSiteId).getOrThrow()
        check(operator.allows(WooPosPermission.MANAGE_STORE_SETTINGS))
        storage.write(localSiteId, WooPosPINAttemptState())
        WooPosPINAccessResult.Authenticated(operator)
    }.getOrDefault(WooPosPINAccessResult.RecoveryUnavailable)

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
                    WooLog.e(WooLog.T.POS, "PIN access operation failed: ${e.javaClass.simpleName}")
                    Result.failure(e)
                }
            }
        }

    private fun seconds(millis: Long): Long = (millis + MILLIS_PER_SECOND - 1) / MILLIS_PER_SECOND

    companion object {
        private const val MILLIS_PER_SECOND = 1000L
    }
}

sealed interface WooPosPINAccessResult {
    data class Authenticated(val operator: WooPosOperator) : WooPosPINAccessResult
    data class Incorrect(val remainingSeconds: Long) : WooPosPINAccessResult
    data class Waiting(val remainingSeconds: Long) : WooPosPINAccessResult
    data object ProtectionOff : WooPosPINAccessResult
    data object Unavailable : WooPosPINAccessResult
    data object RecoveryUnavailable : WooPosPINAccessResult
}
