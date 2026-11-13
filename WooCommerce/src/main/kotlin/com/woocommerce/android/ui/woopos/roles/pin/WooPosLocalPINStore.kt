package com.woocommerce.android.ui.woopos.roles.pin

import com.woocommerce.android.ui.woopos.roles.WooPosLocalRole
import com.woocommerce.android.ui.woopos.roles.WooPosStaff
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
) : WooPosPINCredentialSource {
    private val mutex = Mutex()
    private val codec = WooPosPINCodec()

    override suspend fun credentials(localSiteId: Int): Result<List<WooPosPINCredential>> = operation {
        load(localSiteId).map { (role, hash) -> WooPosPINCredential(WooPosStaff(role.capabilities), hash) }
    }

    suspend fun setPIN(localSiteId: Int, role: WooPosLocalRole, pin: String): Result<Unit> = operation {
        if (!WooPosPINHash.isValidPIN(pin)) throw WooPosPINError.InvalidFormat
        val pins = load(localSiteId).toMutableMap()
        if (role != WooPosLocalRole.ADMIN && WooPosLocalRole.ADMIN !in pins) throw WooPosPINError.AdminRequired
        if (pins.any { (storedRole, hash) -> storedRole != role && hash.matches(pin) }) {
            throw WooPosPINError.MatchesOtherRole
        }
        pins[role] = WooPosPINHash.create(pin)
        storage.write(localSiteId, codec.encode(pins))
    }

    suspend fun removePIN(localSiteId: Int, role: WooPosLocalRole): Result<Unit> = operation {
        require(localSiteId > 0)
        if (role == WooPosLocalRole.ADMIN) {
            storage.write(localSiteId, null)
        } else {
            val pins = load(localSiteId) - role
            storage.write(localSiteId, pins.takeIf { it.isNotEmpty() }?.let(codec::encode))
        }
    }

    suspend fun clearAll(): Result<Unit> = operation { storage.clearAll() }

    private fun load(localSiteId: Int): Map<WooPosLocalRole, WooPosPINHash> {
        require(localSiteId > 0)
        return storage.read(localSiteId)?.let(codec::decode).orEmpty()
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
