package com.woocommerce.android.ui.simplifiedlogin.data

import com.woocommerce.android.OnChangedException
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
import org.wordpress.android.fluxc.store.AccountStore.AuthenticationError
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

    suspend fun login(emailOrUsername: String, password: String): WPComLoginResult {
        WooLog.i(
            T.LOGIN,
            "Signing in using WPCom email or username: $emailOrUsername"
        )
        return submitAuthRequest(emailOrUsername, password, null, false).fold(
            onSuccess = {
                WPComLoginResult.Success(it)
            },
            onFailure = {
                val authError = (it as? OnChangedException)?.error as? AuthenticationError
                    ?: return@fold WPComLoginResult.GenericError(it.message.orEmpty())

                when (authError.type) {
                    INCORRECT_USERNAME_OR_PASSWORD, NOT_AUTHENTICATED -> WPComLoginResult.AuthenticationError
                    NEEDS_2FA -> WPComLoginResult.Requires2FA
                    EMAIL_LOGIN_NOT_ALLOWED -> WPComLoginResult.EmailLoginNotAllowed
                    else -> WPComLoginResult.GenericError(authError.message.orEmpty())
                }
            }
        )
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

    private suspend fun submitAuthRequest(
        emailOrUsername: String,
        password: String,
        twoStepCode: String?,
        shouldRequestTwoStepCode: Boolean
    ) = suspendCancellableCoroutine<Result<String>> { cont ->
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
                    cont.resume(Result.failure(OnChangedException(event.error)))
                } else {
                    WooLog.i(T.LOGIN, "Authentication Succeeded for user ${event.userName}")
                    cont.resume(Result.success(event.userName!!))
                }
            }
        }

        val payload = AuthenticatePayload(emailOrUsername, password).apply {
            this.twoStepCode = twoStepCode
            this.shouldSendTwoStepSms = shouldRequestTwoStepCode
        }
        dispatcher.dispatch(AuthenticationActionBuilder.newAuthenticateAction(payload))

        cont.invokeOnCancellation {
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

sealed interface WPCom2FAResult {
    data class Success(val username: String) : WPCom2FAResult
    object OTPInvalid : WPCom2FAResult
    data class GenericError(val errorMessage: String) : WPCom2FAResult
}

sealed interface NewTwoStepSMSResult {
    object Success : NewTwoStepSMSResult
    data class UserSignedIn(val username: String) : NewTwoStepSMSResult
    data class GenericError(val errorMessage: String) : NewTwoStepSMSResult
}

