package com.woocommerce.android.ui.login.sitecredentials.applicationpassword

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.util.getOrAwaitValue
import com.woocommerce.android.util.runAndCaptureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.wordpress.android.fluxc.network.UserAgent

@OptIn(ExperimentalCoroutinesApi::class)
class ApplicationPasswordTutorialViewModelTest : BaseUnitTest() {
    private val analyticsTracker: AnalyticsTrackerWrapper = mock()
    private val userAgent: UserAgent = mock()

    private lateinit var viewModel: ApplicationPasswordTutorialViewModel

    @Test
    fun `given verified login URL with query, when data is delivered, then initialize login first flow`() =
        testBlocking {
            setup()
            val verifiedLoginUrl = "$VERIFIED_LOGIN_URL?security_token=abc&redirect_to=stale"

            // WHEN
            givenWebViewData(verifiedLoginUrl = verifiedLoginUrl)

            // THEN
            val webViewUrl = requireNotNull(viewModel.currentState.webViewUrl).toHttpUrl()
            assertThat(webViewUrl.encodedPath).isEqualTo("/private-login/")
            assertThat(webViewUrl.queryParameter("security_token")).isEqualTo("abc")
            assertThat(webViewUrl.queryParameterValues("redirect_to"))
                .containsExactly(APPLICATION_PASSWORD_AUTHORIZATION_URL)
            assertThat(viewModel.currentState.applicationPasswordAuthorizationUrl)
                .isEqualTo(APPLICATION_PASSWORD_AUTHORIZATION_URL)
            assertThat(viewModel.currentState.authorizationRecoveryAttempted).isFalse()
            assertThat(viewModel.currentState.errorMessage).isEqualTo(ERROR_MESSAGE)
        }

    @Test
    fun `given no verified login URL, when web view data is delivered, then start at authorization page`() =
        testBlocking {
            setup()

            // WHEN
            givenWebViewData(verifiedLoginUrl = null)

            // THEN
            assertThat(viewModel.currentState.webViewUrl).isEqualTo(APPLICATION_PASSWORD_AUTHORIZATION_URL)
        }

    @Test
    fun `given custom login flow, when admin navigation repeats, then recover only once`() =
        testBlocking {
            setup()
            givenWebViewData()

            // WHEN
            val firstNavigationIntercepted = viewModel.onWebNavigationRequested(SITE_ADMIN_URL)
            val secondNavigationIntercepted = viewModel.onWebNavigationRequested("${SITE_ADMIN_URL}profile.php")

            // THEN
            assertThat(firstNavigationIntercepted).isTrue()
            assertThat(secondNavigationIntercepted).isFalse()
            assertThat(viewModel.currentState.webViewUrl).isEqualTo(APPLICATION_PASSWORD_AUTHORIZATION_URL)
            assertThat(viewModel.currentState.authorizationRecoveryAttempted).isTrue()
        }

    @Test
    fun `given custom login flow, when unrelated navigation is requested, then allow navigation`() =
        testBlocking {
            setup()
            givenWebViewData()
            val startUrl = viewModel.currentState.webViewUrl

            // WHEN
            val intercepted = viewModel.onWebNavigationRequested("$SITE_URL/shop/")

            // THEN
            assertThat(intercepted).isFalse()
            assertThat(viewModel.currentState.webViewUrl).isEqualTo(startUrl)
            assertThat(viewModel.currentState.authorizationRecoveryAttempted).isFalse()
        }

    @Test
    fun `given rewritten authorization page, when rewritten admin loads, then open authorization page`() =
        testBlocking {
            setup()
            givenWebViewData(
                applicationPasswordAuthorizationUrl = REWRITTEN_APPLICATION_PASSWORD_AUTHORIZATION_URL
            )

            // WHEN
            viewModel.onWebPageLoaded(REWRITTEN_ADMIN_URL)

            // THEN
            assertThat(viewModel.currentState.webViewUrl)
                .isEqualTo(REWRITTEN_APPLICATION_PASSWORD_AUTHORIZATION_URL)
            assertThat(viewModel.currentState.authorizationRecoveryAttempted).isTrue()
        }

    @Test
    fun `given custom login flow, when a page in the admin directory loads, then open authorization page`() =
        testBlocking {
            listOf(
                SITE_ADMIN_URL,
                "${SITE_ADMIN_URL}index.php?welcome=1",
                "${SITE_ADMIN_URL}admin.php?page=wc-admin",
                "${SITE_ADMIN_URL}profile.php"
            ).forEach { adminPageUrl ->
                setup()
                givenWebViewData()

                // WHEN
                viewModel.onWebPageLoaded(adminPageUrl)

                // THEN
                assertThat(viewModel.currentState.webViewUrl).isEqualTo(APPLICATION_PASSWORD_AUTHORIZATION_URL)
                assertThat(viewModel.currentState.authorizationRecoveryAttempted).isTrue()
            }
        }

    @Test
    fun `given login page in admin directory, when that page loads, then do not recover`() = testBlocking {
        setup()
        givenWebViewData(
            verifiedLoginUrl = REWRITTEN_LOGIN_URL,
            applicationPasswordAuthorizationUrl = REWRITTEN_APPLICATION_PASSWORD_AUTHORIZATION_URL
        )
        val startUrl = viewModel.currentState.webViewUrl

        // WHEN
        viewModel.onWebPageLoaded("$REWRITTEN_LOGIN_URL?loggedout=true")

        // THEN
        assertThat(viewModel.currentState.webViewUrl).isEqualTo(startUrl)
        assertThat(viewModel.currentState.authorizationRecoveryAttempted).isFalse()
    }

    @Test
    fun `given custom login flow, when landing is ineligible, then keep the flow start URL`() = testBlocking {
        listOf(
            "$SITE_URL/wp-adminsomething/" to APPLICATION_PASSWORD_AUTHORIZATION_URL,
            APPLICATION_PASSWORD_AUTHORIZATION_URL to APPLICATION_PASSWORD_AUTHORIZATION_URL,
            "https://other.example/wp-admin/" to APPLICATION_PASSWORD_AUTHORIZATION_URL,
            "http://site.example/wp-admin/" to APPLICATION_PASSWORD_AUTHORIZATION_URL,
            "https://site.example:8443/wp-admin/" to APPLICATION_PASSWORD_AUTHORIZATION_URL,
            SITE_ADMIN_URL to "not a URL",
            SITE_URL to "$SITE_URL/authorize-application.php"
        ).forEach { (loadedUrl, authorizationUrl) ->
            setup()
            givenWebViewData(applicationPasswordAuthorizationUrl = authorizationUrl)
            val startUrl = viewModel.currentState.webViewUrl

            // WHEN
            viewModel.onWebPageLoaded(loadedUrl)

            // THEN
            assertThat(viewModel.currentState.webViewUrl).isEqualTo(startUrl)
            assertThat(viewModel.currentState.authorizationRecoveryAttempted).isFalse()
        }
    }

    @Test
    fun `given direct authorization flow, when admin loads, then do not recover`() = testBlocking {
        setup()
        givenWebViewData(verifiedLoginUrl = null)

        // WHEN
        viewModel.onWebPageLoaded(SITE_ADMIN_URL)

        // THEN
        assertThat(viewModel.currentState.webViewUrl).isEqualTo(APPLICATION_PASSWORD_AUTHORIZATION_URL)
        assertThat(viewModel.currentState.authorizationRecoveryAttempted).isFalse()
    }

    @Test
    fun `given recovery was used, when fragment data is delivered again, then retain recovered state`() =
        testBlocking {
            setup()
            givenWebViewData()
            viewModel.onWebPageLoaded(SITE_ADMIN_URL)

            // WHEN
            viewModel.onWebViewDataAvailable(
                verifiedLoginUrl = "$SITE_URL/another-login/",
                applicationPasswordAuthorizationUrl = REWRITTEN_APPLICATION_PASSWORD_AUTHORIZATION_URL,
                errorMessage = "another error"
            )

            // THEN
            assertThat(viewModel.currentState.webViewUrl).isEqualTo(APPLICATION_PASSWORD_AUTHORIZATION_URL)
            assertThat(viewModel.currentState.applicationPasswordAuthorizationUrl)
                .isEqualTo(APPLICATION_PASSWORD_AUTHORIZATION_URL)
            assertThat(viewModel.currentState.authorizationRecoveryAttempted).isTrue()
            assertThat(viewModel.currentState.errorMessage).isEqualTo(ERROR_MESSAGE)
        }

    @Test
    fun `given recovered saved state, when recreated and data is redelivered, then retain recovered state`() =
        testBlocking {
            val savedState = SavedStateHandle()
            setup(savedState)
            givenWebViewData()
            viewModel.onWebPageLoaded(SITE_ADMIN_URL)
            advanceUntilIdle()
            val restoredState = SavedStateHandle(savedState.keys().associateWith { savedState.get<Any?>(it) })

            // WHEN
            setup(restoredState)
            givenWebViewData()

            // THEN
            assertThat(viewModel.currentState.webViewUrl).isEqualTo(APPLICATION_PASSWORD_AUTHORIZATION_URL)
            assertThat(viewModel.currentState.applicationPasswordAuthorizationUrl)
                .isEqualTo(APPLICATION_PASSWORD_AUTHORIZATION_URL)
            assertThat(viewModel.currentState.authorizationRecoveryAttempted).isTrue()
        }

    @Test
    fun `given authorization success, when callback loads, then exit with the callback URL`() = testBlocking {
        setup()
        givenWebViewData()
        val startUrl = viewModel.currentState.webViewUrl

        // WHEN
        val event = viewModel.event.runAndCaptureValues {
            viewModel.onWebPageLoaded(SUCCESS_URL)
        }.last()

        // THEN
        assertThat(event).isEqualTo(ExitWithResult(SUCCESS_URL))
        assertThat(viewModel.currentState.webViewUrl).isEqualTo(startUrl)
        assertThat(viewModel.currentState.authorizationRecoveryAttempted).isFalse()
    }

    private fun setup(savedStateHandle: SavedStateHandle = SavedStateHandle()) {
        viewModel = ApplicationPasswordTutorialViewModel(
            analyticsTracker = analyticsTracker,
            userAgent = userAgent,
            savedState = savedStateHandle
        )
    }

    private fun givenWebViewData(
        verifiedLoginUrl: String? = VERIFIED_LOGIN_URL,
        applicationPasswordAuthorizationUrl: String = APPLICATION_PASSWORD_AUTHORIZATION_URL
    ) {
        viewModel.onWebViewDataAvailable(
            verifiedLoginUrl = verifiedLoginUrl,
            applicationPasswordAuthorizationUrl = applicationPasswordAuthorizationUrl,
            errorMessage = ERROR_MESSAGE
        )
    }

    private val ApplicationPasswordTutorialViewModel.currentState
        get() = viewState.getOrAwaitValue()

    private companion object {
        const val SITE_URL = "https://site.example"
        const val SITE_ADMIN_URL = "$SITE_URL/wp-admin/"
        const val VERIFIED_LOGIN_URL = "$SITE_URL/private-login/"
        const val APPLICATION_PASSWORD_AUTHORIZATION_URL = "${SITE_ADMIN_URL}authorize-application.php" +
            "?app_name=woo_android&success_url=woocommerce://login"
        const val REWRITTEN_ADMIN_URL = "$SITE_URL/private-admin/"
        const val REWRITTEN_LOGIN_URL = "${REWRITTEN_ADMIN_URL}login.php"
        const val REWRITTEN_APPLICATION_PASSWORD_AUTHORIZATION_URL =
            "${REWRITTEN_ADMIN_URL}authorize-application.php" +
                "?app_name=woo_android&success_url=woocommerce://login"
        const val SUCCESS_URL = "woocommerce://login?user_login=merchant&password=application-password"
        const val ERROR_MESSAGE = "Native authentication failed"
    }
}
