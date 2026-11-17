package com.woocommerce.android.ui.woopos.roles.lock

import com.woocommerce.android.ui.woopos.roles.WooPosLocalRole
import com.woocommerce.android.ui.woopos.roles.pin.WooPosLocalPINStore
import com.woocommerce.android.ui.woopos.roles.pin.WooPosPINAuthenticator
import com.woocommerce.android.ui.woopos.roles.pin.WooPosPINHash
import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.mockito.kotlin.any
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

internal class WooPosLocalRateLimiterTestFixture(private val dispatchers: CoroutineDispatchers) {
    val admin = WooPosLocalRole.ADMIN
    val cashier = WooPosLocalRole.CASHIER
    var pins = Result.success(
        mapOf(
            admin to WooPosPINHash.create("1234"),
            cashier to WooPosPINHash.create("5678"),
        )
    )
    var pinReadGate: CompletableDeferred<Unit>? = null
    var pinReads = 0
    val store: WooPosLocalPINStore = mock()

    init {
        runBlocking {
            whenever(store.getPINs(any())).doSuspendableAnswer {
                pinReads++
                pinReadGate?.await()
                pins
            }
        }
    }
    var boot = 1
    var elapsed = 1000L
    val clock = object : WooPosPINClock {
        override fun now() = WooPosPINTime(boot, elapsed)
    }
    val records = mutableMapOf<Int, WooPosPINAttemptState>()
    var attemptReads = 0
    var readError = false
    var writeError = false
    var onAttemptWrite: ((WooPosPINAttemptState) -> Unit)? = null
    val storage = object : WooPosPINAttemptStorage {
        override fun read(localSiteId: Int): WooPosPINAttemptState {
            attemptReads++
            check(!readError)
            return records[localSiteId] ?: WooPosPINAttemptState.NO_FAILURES
        }
        override fun write(localSiteId: Int, state: WooPosPINAttemptState) {
            check(!writeError)
            onAttemptWrite?.invoke(state)
            records[localSiteId] = state
        }
        override fun clearAll() { records.clear() }
    }
    var recoveryResult: Result<WooPosLocalRole> = Result.failure(IllegalStateException())
    var recoveryAction: suspend () -> Result<WooPosLocalRole> = { recoveryResult }
    val recovery = object : WooPosPINRecovery {
        override suspend fun authenticate(localSiteId: Int) = recoveryAction()
    }
    fun access() = WooPosLocalRateLimiter(
        store,
        WooPosPINAuthenticator(dispatchers),
        storage,
        clock,
        recovery,
        dispatchers,
    )
}
