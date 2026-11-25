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
    ): Result<Unit> = savePIN(localSiteId, role, pin, WriteMode.CREATE, canCommit)

    suspend fun replacePIN(
        localSiteId: Int,
        role: WooPosLocalRole,
        pin: String,
        canCommit: () -> Boolean = { true },
    ): Result<Unit> = savePIN(localSiteId, role, pin, WriteMode.REPLACE, canCommit)

    suspend fun setPIN(
        localSiteId: Int,
        role: WooPosLocalRole,
        pin: String,
    ): Result<Unit> = savePIN(localSiteId, role, pin, WriteMode.UPSERT) { true }

    private suspend fun savePIN(
        localSiteId: Int,
        role: WooPosLocalRole,
        pin: String,
        mode: WriteMode,
        canCommit: () -> Boolean,
    ): Result<Unit> = operation {
        if (!WooPosPINHash.isValidPIN(pin)) throw WooPosPINError.InvalidFormat
        val pins = load(localSiteId).toMutableMap()
        if (mode == WriteMode.CREATE && role in pins) throw WooPosPINError.AlreadyExists
        if (mode == WriteMode.REPLACE && role !in pins) throw WooPosPINError.StaleRequest
        if (role != WooPosLocalRole.ADMIN && WooPosLocalRole.ADMIN !in pins) throw WooPosPINError.AdminRequired
        pins.requireUniquePIN(role, pin)
        pins[role] = WooPosPINHash.create(pin)
        if (!storage.write(localSiteId, codec.encode(pins), canCommit)) throw WooPosPINError.StaleRequest
    }

    suspend fun removePIN(
        localSiteId: Int,
        role: WooPosLocalRole,
        canCommit: () -> Boolean = { true },
    ): Result<Unit> = operation {
        require(localSiteId > 0)
        val updated = if (role == WooPosLocalRole.ADMIN) {
            null
        } else {
            val pins = load(localSiteId) - role
            pins.takeIf { it.isNotEmpty() }?.let(codec::encode)
        }
        if (!storage.write(localSiteId, updated, canCommit)) throw WooPosPINError.StaleRequest
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

    private enum class WriteMode { CREATE, REPLACE, UPSERT }
}
