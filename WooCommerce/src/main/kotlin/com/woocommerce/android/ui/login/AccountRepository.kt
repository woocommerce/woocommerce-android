package com.woocommerce.android.ui.login

import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T.SITE_PICKER
import kotlinx.coroutines.suspendCancellableCoroutine
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.AccountAction.SIGN_OUT
import org.wordpress.android.fluxc.generated.AccountActionBuilder
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged
import javax.inject.Inject
import kotlin.coroutines.resume

class AccountRepository @Inject constructor(
    private val accountStore: AccountStore,
    private val dispatcher: Dispatcher
) {
    fun getUserAccount(): AccountModel? = accountStore.account.takeIf { it.userId != 0L }

    fun isUserLoggedIn() = accountStore.hasAccessToken()

    suspend fun logout(): Boolean = suspendCancellableCoroutine { continuation ->
        val listener = object : Any() {
            @Suppress("unused")
            @Subscribe(threadMode = MAIN)
            fun onAccountChanged(event: OnAccountChanged) {
                if (event.causeOfChange == SIGN_OUT) {
                    dispatcher.unregister(this)
                    if (!continuation.isActive) return

                    if (event.isError) {
                        WooLog.e(
                            SITE_PICKER,
                            "Account error [type = ${event.causeOfChange}] : " +
                                "${event.error.type} > ${event.error.message}"
                        )
                        continuation.resume(false)
                    } else if (!isUserLoggedIn()) {
                        continuation.resume(true)
                    }
                }
            }
        }
        dispatcher.register(listener)
        dispatcher.dispatch(AccountActionBuilder.newSignOutAction())
        dispatcher.dispatch(SiteActionBuilder.newRemoveWpcomAndJetpackSitesAction())

        continuation.invokeOnCancellation {
            dispatcher.unregister(listener)
        }
    }
}
