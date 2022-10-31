package com.woocommerce.android.ui.login

import com.woocommerce.android.OnChangedException
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T.SITE_PICKER
import com.woocommerce.android.util.dispatchAndAwait
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.AccountActionBuilder
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged
import javax.inject.Inject

class AccountRepository @Inject constructor(
    private val accountStore: AccountStore,
    private val dispatcher: Dispatcher
) {
    fun getUserAccount(): AccountModel? = accountStore.account.takeIf { it.userId != 0L }

    suspend fun fetchUserAccount(): Result<Unit> {
        val event: OnAccountChanged = dispatcher.dispatchAndAwait(AccountActionBuilder.newFetchAccountAction())

        return when {
            event.isError -> Result.failure(OnChangedException(event.error))
            else -> Result.success(Unit)
        }
    }

    fun isUserLoggedIn() = accountStore.hasAccessToken()

    suspend fun logout(): Boolean {
        val event: OnAccountChanged = dispatcher.dispatchAndAwait(AccountActionBuilder.newSignOutAction())
        return if (event.isError) {
            WooLog.e(
                SITE_PICKER,
                "Account error [type = ${event.causeOfChange}] : " +
                    "${event.error.type} > ${event.error.message}"
            )
            false
        } else {
            dispatcher.dispatch(SiteActionBuilder.newRemoveWpcomAndJetpackSitesAction())
            true
        }
    }
}
