package com.woocommerce.android.ui.woopos.roles.pin

import com.woocommerce.android.ui.woopos.roles.WooPosLocalRole
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WooPosLocalPINStore @Inject constructor(
    private val storage: WooPosPINStorage,
    private val dispatchers: CoroutineDispatchers,
) {
    private val mutex = Mutex()
    private val codec = WooPosPINCodec()

    suspend fun getPINs(localSiteId: Int): Result<Map<WooPosLocalRole, WooPosPINHash>> = operation {
        load(localSiteId)
    }

    suspend fun validatePINUniqueness(localSiteId: Int, role: WooPosLocalRole, pin: String): Result<Unit> = operation {
        if (!WooPosPINHash.isValidPIN(pin)) throw WooPosPINError.InvalidFormat
        load(localSiteId).requireUniquePIN(role, pin)
    }

    suspend fun createPIN(
        localSiteId: Int,
        role: WooPosLocalRole,
        pin: String,
        canCommit: () -> Boolean = { true },
    ): Result<Unit> = savePIN(localSiteId, role, pin, createOnly = true, canCommit)

    suspend fun setPIN(
        localSiteId: Int,
        role: WooPosLocalRole,
        pin: String,
        canCommit: () -> Boolean = { true },
    ): Result<Unit> = savePIN(localSiteId, role, pin, createOnly = false, canCommit)

    private suspend fun savePIN(
        localSiteId: Int,
        role: WooPosLocalRole,
        pin: String,
        createOnly: Boolean,
        canCommit: () -> Boolean,
    ): Result<Unit> = operation {
        if (!WooPosPINHash.isValidPIN(pin)) throw WooPosPINError.InvalidFormat
        val pins = load(localSiteId).toMutableMap()
        if (createOnly && role in pins) throw WooPosPINError.AlreadyExists
        if (role != WooPosLocalRole.ADMIN && WooPosLocalRole.ADMIN !in pins) throw WooPosPINError.AdminRequired
        pins.requireUniquePIN(role, pin)
        pins[role] = WooPosPINHash.create(pin)
        if (!canCommit()) throw WooPosPINError.StaleRequest
        storage.write(localSiteId, codec.encode(pins))
    }

    suspend fun removePIN(
        localSiteId: Int,
        role: WooPosLocalRole,
        canCommit: () -> Boolean = { true },
    ): Result<Unit> = operation {
        require(localSiteId > 0)
        if (role == WooPosLocalRole.ADMIN) {
            if (!canCommit()) throw WooPosPINError.StaleRequest
            storage.write(localSiteId, null)
        } else {
            val pins = load(localSiteId) - role
            if (!canCommit()) throw WooPosPINError.StaleRequest
            storage.write(localSiteId, pins.takeIf { it.isNotEmpty() }?.let(codec::encode))
        }
    }

    suspend fun clearAll(): Result<Unit> = operation { storage.clearAll() }

    private fun load(localSiteId: Int): Map<WooPosLocalRole, WooPosPINHash> {
        require(localSiteId > 0)
        return storage.read(localSiteId)?.let(codec::decode).orEmpty()
    }

    private fun Map<WooPosLocalRole, WooPosPINHash>.requireUniquePIN(role: WooPosLocalRole, pin: String) {
        if (any { (storedRole, hash) -> storedRole != role && hash.matches(pin) }) {
            throw WooPosPINError.MatchesOtherRole
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun <T> operation(block: () -> T): Result<T> = withContext(dispatchers.io) {
        mutex.withLock {
            try {
                Result.success(block())
            } catch (e: CancellationException) {
                throw e
            } catch (e: WooPosPINError) {
                Result.failure(e)
            } catch (e: Exception) {
                WooLog.e(WooLog.T.POS, "Local PIN storage operation failed: ${e.javaClass.simpleName}")
                Result.failure(WooPosPINError.StorageFailure)
            }
        }
    }
}
