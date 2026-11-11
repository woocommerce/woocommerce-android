package com.woocommerce.android.ui.woopos.lock

import com.woocommerce.android.ui.woopos.roles.WooPosOperator
import com.woocommerce.android.ui.woopos.roles.WooPosPINAuthenticator
import com.woocommerce.android.ui.woopos.roles.WooPosPINCredential
import com.woocommerce.android.ui.woopos.roles.WooPosPINCredentialSource
import com.woocommerce.android.ui.woopos.roles.WooPosPINHash
import com.woocommerce.android.ui.woopos.roles.WooPosRole
import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.CompletableDeferred

internal class WooPosPINAccessTestFixture(private val dispatchers: CoroutineDispatchers) {
    val admin = WooPosOperator(WooPosRole.ADMIN.permissions)
    val cashier = WooPosOperator(WooPosRole.CASHIER.permissions)
    var credentials = Result.success(
        listOf(
            WooPosPINCredential(admin, WooPosPINHash.create("1234")),
            WooPosPINCredential(cashier, WooPosPINHash.create("5678")),
        )
    )
    var credentialReadGate: CompletableDeferred<Unit>? = null
    var credentialReads = 0
    val source = object : WooPosPINCredentialSource {
        override suspend fun credentials(localSiteId: Int): Result<List<WooPosPINCredential>> {
            credentialReads++
            credentialReadGate?.await()
            return credentials
        }
    }
    var boot = 1
    var elapsed = 1000L
    val clock = object : WooPosPINClock {
        override fun now() = WooPosPINTime(boot, elapsed)
    }
    val records = mutableMapOf<Int, WooPosPINAttemptState>()
    var readError = false
    var writeError = false
    val storage = object : WooPosPINAttemptStorage {
        override fun read(localSiteId: Int): WooPosPINAttemptState {
            check(!readError)
            return records[localSiteId] ?: WooPosPINAttemptState()
        }
        override fun write(localSiteId: Int, state: WooPosPINAttemptState) {
            check(!writeError)
            records[localSiteId] = state
        }
        override fun clearAll() { records.clear() }
    }
    var recoveryResult: Result<WooPosOperator> = Result.failure(IllegalStateException())
    val recovery = object : WooPosPINRecovery {
        override suspend fun authenticate(localSiteId: Int) = recoveryResult
    }
    fun access() = WooPosPINAccess(
        source,
        WooPosPINAuthenticator(source, dispatchers),
        storage,
        clock,
        recovery,
        dispatchers,
    )
}
