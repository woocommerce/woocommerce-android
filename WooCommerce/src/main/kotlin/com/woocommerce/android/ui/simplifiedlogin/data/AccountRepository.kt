package com.woocommerce.android.ui.simplifiedlogin.data

import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import com.woocommerce.android.util.WooLog.T.SITE_PICKER
import kotlinx.coroutines.suspendCancellableCoroutine
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.AccountAction.SIGN_OUT
import org.wordpress.android.fluxc.generated.AccountActionBuilder
import org.wordpress.android.fluxc.generated.AuthenticationActionBuilder
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.AccountStore.AuthenticatePayload
import org.wordpress.android.fluxc.store.AccountStore.AuthenticationErrorType.EMAIL_LOGIN_NOT_ALLOWED
import org.wordpress.android.fluxc.store.AccountStore.AuthenticationErrorType.INCORRECT_USERNAME_OR_PASSWORD
import org.wordpress.android.fluxc.store.AccountStore.AuthenticationErrorType.NEEDS_2FA
import org.wordpress.android.fluxc.store.AccountStore.AuthenticationErrorType.NOT_AUTHENTICATED
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged
import org.wordpress.android.fluxc.store.AccountStore.OnAuthenticationChanged
import javax.inject.Inject
import kotlin.coroutines.resume

class AccountRepository @Inject constructor(
    private val accountStore: AccountStore,
    private val dispatcher: Dispatcher
) {
    fun getUserAccount(): AccountModel? = accountStore.account.takeIf { it.userId != 0L }

    fun isUserLoggedIn() = accountStore.hasAccessToken()

    suspend fun login(emailOrUsername: String, password: String) =
        suspendCancellableCoroutine<WPComLoginResult> { cont ->
            val listener = object : Any() {
                // OnChanged events
                @Subscribe(threadMode = MAIN)
                @Suppress("unused")
                fun onAuthenticationChanged(event: OnAuthenticationChanged) {
                    dispatcher.unregister(this)
                    if (event.isError) {
                        WooLog.w(
                            T.LOGIN,
                            "onAuthenticationChanged has error: " + event.error.type + " - " + event.error.message
                        )
                        when (event.error.type) {
                            INCORRECT_USERNAME_OR_PASSWORD, NOT_AUTHENTICATED -> cont.resume(WPComLoginResult.AuthenticationError)
                            NEEDS_2FA -> cont.resume(WPComLoginResult.Requires2FA)
                            EMAIL_LOGIN_NOT_ALLOWED -> cont.resume(WPComLoginResult.EmailLoginNotAllowed)
                            else -> cont.resume(WPComLoginResult.GenericError(event.error.message.orEmpty()))
                        }
                    } else {
                        WooLog.i(T.LOGIN, "Authentication Succeeded for user ${event.userName}")
                        cont.resume(WPComLoginResult.Success(event.userName!!))
                    }
                }
            }

            val payload = AuthenticatePayload(emailOrUsername, password)
            dispatcher.dispatch(AuthenticationActionBuilder.newAuthenticateAction(payload))
            WooLog.i(
                T.LOGIN,
                "User tries to log in wpcom. email or username: $emailOrUsername"
            )

            cont.invokeOnCancellation {
                dispatcher.unregister(listener)
            }
        }

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

sealed interface WPComLoginResult {
    data class Success(val username: String) : WPComLoginResult
    object Requires2FA : WPComLoginResult
    object EmailLoginNotAllowed : WPComLoginResult
    object AuthenticationError : WPComLoginResult
    data class GenericError(val errorMessage: String) : WPComLoginResult
}
