@file:Suppress("FunctionNaming")

package com.woocommerce.android.aiassistant.headless

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.AccountAction
import org.wordpress.android.fluxc.action.AuthenticationAction
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.network.rest.wpcom.auth.Authenticator.TwoFactorResponse
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.AccountStore.AuthenticationErrorType.NEEDS_2FA
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged
import org.wordpress.android.fluxc.store.AccountStore.OnAuthenticationChanged
import org.wordpress.android.fluxc.store.AccountStore.OnTwoFactorAuthStarted
import kotlin.time.Duration.Companion.milliseconds

@RunWith(RobolectricTestRunner::class)
class WooAiSmokeWpComAuthenticatorTest {
    @Test
    fun `given auth succeeds, when authenticating, then fetches account before site lookup`() =
        runTest {
            val dispatcher = TrackingDispatcher()
            val authenticator = WooAiSmokeWpComAuthenticator(
                dispatcher = dispatcher,
                accountStore = mock {
                    on { hasAccessToken() } doReturn true
                    on { account } doReturn AccountModel().apply { userId = ACCOUNT_ID }
                },
                authTimeout = 500.milliseconds,
            )

            dispatcher.eventsToEmitOnDispatch += OnAuthenticationChanged()
            dispatcher.eventsToEmitOnDispatch += fetchAccountChanged()

            authenticator.authenticate(credentials())

            assertThat(dispatcher.dispatchedActions.map { it.type })
                .containsExactly(AuthenticationAction.AUTHENTICATE, AccountAction.FETCH_ACCOUNT)
        }

    @Test
    fun `given 2fa auth signal, when authenticating, then failure recommends application passwords`() =
        runTest {
            val twoFactorEvents = listOf(
                OnAuthenticationChanged().apply {
                    error = AccountStore.AuthenticationError(NEEDS_2FA, "2fa required")
                },
                OnTwoFactorAuthStarted(twoFactorResponse()),
            )

            twoFactorEvents.forEach { event ->
                val dispatcher = TrackingDispatcher()
                val authenticator = WooAiSmokeWpComAuthenticator(
                    dispatcher = dispatcher,
                    accountStore = mock(),
                    authTimeout = 500.milliseconds,
                )
                dispatcher.eventsToEmitOnDispatch += event

                val error = runCatching { authenticator.authenticate(credentials()) }.exceptionOrNull()

                assertThat(error)
                    .hasMessageContaining("WordPress.com Application Password")
                    .hasMessageContaining("WOO_WPCOM_PASSWORD")
            }
        }

    @Test
    fun `given account fetch returns no user id after auth, when authenticating, then failure explains missing account`() =
        runTest {
            val dispatcher = TrackingDispatcher()
            val authenticator = WooAiSmokeWpComAuthenticator(
                dispatcher = dispatcher,
                accountStore = mock {
                    on { hasAccessToken() } doReturn true
                    on { account } doReturn AccountModel()
                },
                authTimeout = 500.milliseconds,
            )
            dispatcher.eventsToEmitOnDispatch += OnAuthenticationChanged()
            dispatcher.eventsToEmitOnDispatch += fetchAccountChanged()

            val error = runCatching { authenticator.authenticate(credentials()) }.exceptionOrNull()

            assertThat(error).hasMessageContaining("WPCOM_ACCOUNT_MISSING")
        }

    private fun credentials() = WooAiSmokeCredentialConfig(
        siteUrl = "https://store.example",
        wpComUsername = "merchant@example.com",
        wpComPassword = "app password",
        storeLabel = "store",
        outputDirectory = java.io.File("build/woo-ai-smoke"),
        credentialSource = "test",
    )

    private fun twoFactorResponse() = TwoFactorResponse(
        JSONObject(
            """
            {
              "user_id": "123",
              "two_step_nonce_webauthn": "",
              "two_step_nonce_backup": "backup",
              "two_step_nonce_authenticator": "authenticator",
              "two_step_nonce_push": "",
              "two_step_supported_auth_types": ["authenticator"]
            }
            """.trimIndent()
        )
    )

    private fun fetchAccountChanged() = OnAccountChanged().apply {
        causeOfChange = AccountAction.FETCH_ACCOUNT
    }

    private class TrackingDispatcher : Dispatcher() {
        val dispatchedActions = mutableListOf<Action<*>>()
        val eventsToEmitOnDispatch = ArrayDeque<Any>()

        override fun dispatch(action: Action<*>) {
            dispatchedActions += action
            eventsToEmitOnDispatch.removeFirstOrNull()?.let(::emitChange)
        }
    }

    private companion object {
        const val ACCOUNT_ID = 123L
    }
}
