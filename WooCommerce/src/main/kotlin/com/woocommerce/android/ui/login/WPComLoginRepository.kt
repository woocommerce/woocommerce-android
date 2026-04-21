package com.woocommerce.android.ui.login

import com.woocommerce.android.OnChangedException
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.awaitEvent
import com.woocommerce.android.util.dispatchAndAwait
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.selects.select
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.AccountActionBuilder
import org.wordpress.android.fluxc.generated.AuthenticationActionBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.store.AccountStore.AuthEmailPayload
import org.wordpress.android.fluxc.store.AccountStore.AuthEmailPayloadScheme
import org.wordpress.android.fluxc.store.AccountStore.AuthenticatePayload
import org.wordpress.android.fluxc.store.AccountStore.AuthenticateTwoFactorPayload
import org.wordpress.android.fluxc.store.AccountStore.AuthenticationError
import org.wordpress.android.fluxc.store.AccountStore.AuthenticationErrorType.NEEDS_2FA
import org.wordpress.android.fluxc.store.AccountStore.FetchAuthOptionsPayload
import org.wordpress.android.fluxc.store.AccountStore.FinishWebauthnChallengePayload
import org.wordpress.android.fluxc.store.AccountStore.OnAuthEmailSent
import org.wordpress.android.fluxc.store.AccountStore.OnAuthOptionsFetched
import org.wordpress.android.fluxc.store.AccountStore.OnAuthenticationChanged
import org.wordpress.android.fluxc.store.AccountStore.OnTwoFactorAuthStarted
import org.wordpress.android.fluxc.store.AccountStore.StartWebauthnChallengePayload
import org.wordpress.android.fluxc.store.AccountStore.WebauthnChallengeReceived
import org.wordpress.android.fluxc.store.AccountStore.WebauthnPasskeyAuthenticated
import org.wordpress.android.login.AuthOptions
import javax.inject.Inject

class WPComLoginRepository @Inject constructor(
    private val dispatcher: Dispatcher,
    private val wpComAccessToken: AccessToken
) {
    suspend fun fetchAuthOptions(emailOrUsername: String): Result<AuthOptions> {
        val action = AccountActionBuilder.newFetchAuthOptionsAction(
            FetchAuthOptionsPayload(emailOrUsername)
        )
        val event: OnAuthOptionsFetched = dispatcher.dispatchAndAwait(action)

        return when {
            event.isError -> Result.failure(OnChangedException(event.error))
            else -> Result.success(
                AuthOptions(
                    isPasswordless = event.isPasswordless,
                    isEmailVerified = event.isEmailVerified
                )
            )
        }
    }

    fun clearAccessToken() {
        wpComAccessToken.set(null)
    }

    suspend fun login(emailOrUsername: String, password: String): LoginResult = coroutineScope {
        WooLog.i(
            WooLog.T.LOGIN,
            "Signing in using WPCom email or username: $emailOrUsername"
        )
        val authChangedDeferred = async { dispatcher.awaitEvent<OnAuthenticationChanged>() }
        val twoFactorDeferred = async { dispatcher.awaitEvent<OnTwoFactorAuthStarted>() }
        dispatcher.dispatch(
            AuthenticationActionBuilder.newAuthenticateAction(
                AuthenticatePayload(emailOrUsername, password)
            )
        )

        select {
            twoFactorDeferred.onAwait { event ->
                authChangedDeferred.cancel()
                LoginResult.TwoFactorRequired(
                    userId = event.userId,
                    webauthnNonce = event.webauthnNonce,
                    supportedAuthTypes = event.mSupportedAuthTypes
                )
            }
            authChangedDeferred.onAwait { event ->
                twoFactorDeferred.cancel()
                when {
                    !event.isError -> LoginResult.Success
                    event.error?.type == NEEDS_2FA -> LoginResult.TwoFactorRequired(
                        userId = "",
                        webauthnNonce = "",
                        supportedAuthTypes = emptyList()
                    )
                    else -> {
                        WooLog.w(
                            WooLog.T.LOGIN,
                            "Authentication request failed: ${event.error.type} - ${event.error.message}"
                        )
                        LoginResult.Error(event.error)
                    }
                }
            }
        }
    }

    suspend fun submitTwoStepCode(emailOrUsername: String, password: String, twoStepCode: String): Result<Unit> {
        WooLog.i(WooLog.T.LOGIN, "Submitting 2FA verification code")

        return submitAuthRequest(emailOrUsername, password, twoStepCode, false)
    }

    suspend fun requestTwoStepSMS(emailOrUsername: String, password: String): Result<SMSRequestResult> {
        WooLog.i(WooLog.T.LOGIN, "Submitting 2FA verification code")

        return submitAuthRequest(emailOrUsername, password, null, true)
            .map {
                // If we get a successful response, then this means the user was successfully signed in
                // This can happen only if 2FA was disabled in the account before sending the request
                SMSRequestResult.UserSignedIn
            }
            .recoverCatching {
                if (((it as? OnChangedException)?.error as? AuthenticationError)?.type == NEEDS_2FA) {
                    SMSRequestResult.SMSRequested
                } else {
                    throw it
                }
            }
    }

    suspend fun requestMagicLink(
        emailOrUsername: String,
        flow: MagicLinkFlow,
        isSignup: Boolean
    ): Result<Unit> {
        WooLog.i(WooLog.T.LOGIN, "Submitting a Magic Link request")

        val action = AuthenticationActionBuilder.newSendAuthEmailAction(
            AuthEmailPayload(
                emailOrUsername,
                isSignup,
                flow,
                null,
                AuthEmailPayloadScheme.WOOCOMMERCE
            )
        )
        val event: OnAuthEmailSent = dispatcher.dispatchAndAwait(action)

        return when {
            event.isError -> {
                WooLog.w(
                    WooLog.T.LOGIN,
                    "Magic Link request failed: " + event.error.type + " - " + event.error.message
                )
                Result.failure(OnChangedException(event.error))
            }

            else -> {
                WooLog.i(WooLog.T.LOGIN, "Magic Link request sent successfully")
                Result.success(Unit)
            }
        }
    }

    private suspend fun submitAuthRequest(
        emailOrUsername: String,
        password: String,
        twoStepCode: String?,
        shouldRequestTwoStepCode: Boolean
    ): Result<Unit> {
        val payload = AuthenticateTwoFactorPayload(
            emailOrUsername,
            password,
            twoStepCode.orEmpty(),
            shouldRequestTwoStepCode
        )
        val event: OnAuthenticationChanged =
            dispatcher.dispatchAndAwait(AuthenticationActionBuilder.newAuthenticateTwoFactorAction(payload))

        return if (event.isError) {
            WooLog.w(
                WooLog.T.LOGIN,
                "Authentication request failed: " + event.error.type + " - " + event.error.message
            )
            Result.failure(OnChangedException(event.error))
        } else {
            WooLog.i(WooLog.T.LOGIN, "Authentication Succeeded for user ${event.userName}")
            Result.success(Unit)
        }
    }

    suspend fun startSecurityKeyChallenge(
        userId: String,
        webauthnNonce: String
    ): Result<SecurityKeyChallengeData> {
        WooLog.i(WooLog.T.LOGIN, "Starting security key challenge")
        val event: WebauthnChallengeReceived = dispatcher.dispatchAndAwait(
            AuthenticationActionBuilder.newStartSecurityKeyChallengeAction(
                StartWebauthnChallengePayload(userId, webauthnNonce)
            )
        )

        return if (event.isError) {
            WooLog.w(WooLog.T.LOGIN, "Security key challenge failed: ${event.error.type}")
            Result.failure(OnChangedException(event.error))
        } else {
            Result.success(
                SecurityKeyChallengeData(
                    userId = event.mUserId,
                    twoStepNonce = event.webauthnNonce,
                    challengeJson = event.mJsonResponse.toString()
                )
            )
        }
    }

    suspend fun finishSecurityKeyChallenge(
        userId: String,
        twoStepNonce: String,
        clientData: String
    ): Result<Unit> {
        WooLog.i(WooLog.T.LOGIN, "Finishing security key challenge")
        val payload = FinishWebauthnChallengePayload().apply {
            mUserId = userId
            mTwoStepNonce = twoStepNonce
            mClientData = clientData
        }
        val event: WebauthnPasskeyAuthenticated = dispatcher.dispatchAndAwait(
            AuthenticationActionBuilder.newFinishSecurityKeyChallengeAction(payload)
        )

        return if (event.isError) {
            WooLog.w(WooLog.T.LOGIN, "Security key authentication failed: ${event.error.type}")
            Result.failure(OnChangedException(event.error))
        } else {
            WooLog.i(WooLog.T.LOGIN, "Security key authentication succeeded")
            Result.success(Unit)
        }
    }

    data class SecurityKeyChallengeData(
        val userId: String,
        val twoStepNonce: String,
        val challengeJson: String
    )

    sealed class LoginResult {
        data object Success : LoginResult()
        data class TwoFactorRequired(
            val userId: String,
            val webauthnNonce: String,
            val supportedAuthTypes: List<String>
        ) : LoginResult()
        data class Error(val error: AuthenticationError) : LoginResult()
    }

    enum class SMSRequestResult {
        UserSignedIn, SMSRequested
    }
}
