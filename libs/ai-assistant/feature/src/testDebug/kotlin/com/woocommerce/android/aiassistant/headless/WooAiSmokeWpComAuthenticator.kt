package com.woocommerce.android.aiassistant.headless

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.AccountAction
import org.wordpress.android.fluxc.generated.AccountActionBuilder
import org.wordpress.android.fluxc.generated.AuthenticationActionBuilder
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.AccountStore.AuthenticatePayload
import org.wordpress.android.fluxc.store.AccountStore.AuthenticationErrorType.NEEDS_2FA
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged
import org.wordpress.android.fluxc.store.AccountStore.OnAuthenticationChanged
import org.wordpress.android.fluxc.store.AccountStore.OnTwoFactorAuthStarted
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal class WooAiSmokeWpComAuthenticator internal constructor(
    private val dispatcher: Dispatcher,
    private val accountStore: AccountStore,
    private val authTimeout: Duration,
) {
    @Inject
    constructor(
        dispatcher: Dispatcher,
        accountStore: AccountStore,
    ) : this(dispatcher, accountStore, AUTH_TIMEOUT)

    suspend fun authenticate(credentials: WooAiSmokeCredentialConfig) {
        val listener = AuthenticationListener()
        dispatcher.register(listener)
        val authResult = try {
            dispatcher.dispatch(
                AuthenticationActionBuilder.newAuthenticateAction(
                    AuthenticatePayload(credentials.wpComUsername, credentials.wpComPassword)
                )
            )
            listener.awaitResult(authTimeout)
        } catch (_: TimeoutCancellationException) {
            error("WPCOM_AUTH_TIMEOUT")
        } finally {
            dispatcher.unregister(listener)
        }

        when (authResult) {
            is AuthenticationResult.Success -> {
                require(accountStore.hasAccessToken()) { "WPCOM_OAUTH_TOKEN_MISSING" }
                fetchAccountProfile()
            }
            is AuthenticationResult.Failure -> error(authResult.message)
        }
    }

    private suspend fun fetchAccountProfile() {
        val listener = AccountFetchListener()
        dispatcher.register(listener)
        val result = try {
            dispatcher.dispatch(AccountActionBuilder.newFetchAccountAction())
            listener.awaitResult(authTimeout)
        } catch (_: TimeoutCancellationException) {
            error("WPCOM_ACCOUNT_FETCH_TIMEOUT")
        } finally {
            dispatcher.unregister(listener)
        }

        when (result) {
            is AccountFetchResult.Success -> require(accountStore.account.userId > 0L) {
                "WPCOM_ACCOUNT_MISSING: /me did not persist a default WordPress.com account."
            }
            is AccountFetchResult.Failure -> error(result.message)
        }
    }

    class AuthenticationListener {
        private val result = CompletableDeferred<AuthenticationResult>()

        @Subscribe(threadMode = ThreadMode.POSTING)
        fun onAuthenticationChanged(event: OnAuthenticationChanged) {
            if (!event.isError) {
                result.complete(AuthenticationResult.Success)
                return
            }

            val error = event.error
            if (error?.type == NEEDS_2FA) {
                result.complete(AuthenticationResult.Failure(TWO_FACTOR_APPLICATION_PASSWORDS_MESSAGE))
            } else {
                result.complete(
                    AuthenticationResult.Failure(
                        "WPCOM_AUTH_FAILED: ${error?.type ?: "UNKNOWN"} ${error?.message.orEmpty()}".trim()
                    )
                )
            }
        }

        @Subscribe(threadMode = ThreadMode.POSTING)
        fun onTwoFactorAuthStarted(@Suppress("UnusedParameter") event: OnTwoFactorAuthStarted) {
            result.complete(AuthenticationResult.Failure(TWO_FACTOR_APPLICATION_PASSWORDS_MESSAGE))
        }

        suspend fun awaitResult(timeout: Duration): AuthenticationResult =
            withTimeout(timeout) { result.await() }
    }

    class AccountFetchListener {
        private val result = CompletableDeferred<AccountFetchResult>()

        @Subscribe(threadMode = ThreadMode.POSTING)
        fun onAccountChanged(event: OnAccountChanged) {
            if (event.causeOfChange != AccountAction.FETCH_ACCOUNT) return

            if (!event.isError) {
                result.complete(AccountFetchResult.Success)
                return
            }

            val error = event.error
            result.complete(
                AccountFetchResult.Failure(
                    "WPCOM_ACCOUNT_FETCH_FAILED: ${error?.type ?: "UNKNOWN"} ${error?.message.orEmpty()}".trim()
                )
            )
        }

        suspend fun awaitResult(timeout: Duration): AccountFetchResult =
            withTimeout(timeout) { result.await() }
    }

    sealed interface AuthenticationResult {
        data object Success : AuthenticationResult
        data class Failure(val message: String) : AuthenticationResult
    }

    sealed interface AccountFetchResult {
        data object Success : AccountFetchResult
        data class Failure(val message: String) : AccountFetchResult
    }

    companion object {
        const val TWO_FACTOR_APPLICATION_PASSWORDS_MESSAGE =
            "WPCOM_AUTH_REQUIRES_2FA: use a WordPress.com Application Password for WOO_WPCOM_PASSWORD."

        private val AUTH_TIMEOUT = 30.seconds
    }
}
