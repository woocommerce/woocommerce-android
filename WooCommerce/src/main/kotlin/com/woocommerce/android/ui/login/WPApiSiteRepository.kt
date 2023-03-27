package com.woocommerce.android.ui.login

import com.woocommerce.android.OnChangedException
import com.woocommerce.android.R.string
import com.woocommerce.android.applicationpasswords.ApplicationPasswordsNotifier
import com.woocommerce.android.model.UiString
import com.woocommerce.android.model.UiString.UiStringRes
import com.woocommerce.android.model.UiString.UiStringText
import com.woocommerce.android.ui.common.UserEligibilityFetcher
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.CookieNonceAuthenticator
import org.wordpress.android.fluxc.network.rest.wpapi.Nonce
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.FetchWPAPISitePayload
import org.wordpress.android.login.util.SiteUtils
import java.net.CookieManager
import javax.inject.Inject

class WPApiSiteRepository @Inject constructor(
    private val siteStore: SiteStore,
    private val cookieNonceAuthenticator: CookieNonceAuthenticator,
    private val userEligibilityFetcher: UserEligibilityFetcher,
    private val applicationPasswordsNotifier: ApplicationPasswordsNotifier,
    private val cookieManager: CookieManager
) {
    /**
     * Handles authentication to the given [url] using wp-admin credentials.
     * After calling this, and if the authentication is successful, a [SiteModel] matching this site will be persisted
     * in the DB.
     */
    suspend fun loginAndFetchSite(url: String, username: String, password: String): Result<SiteModel> {
        WooLog.d(WooLog.T.LOGIN, "Authenticating in to site $url using site credentials")

        // Clear cookies to make sure the new credentials are correctly checked
        cookieManager.cookieStore.removeAll()

        val authenticationResult = cookieNonceAuthenticator.authenticate(
            siteUrl = url,
            username = username,
            password = password
        )

        return when (authenticationResult) {
            is CookieNonceAuthenticator.CookieNonceAuthenticationResult.Success -> {
                WooLog.d(WooLog.T.LOGIN, "Authentication succeeded")
                fetchSite(url, username, password)
            }

            is CookieNonceAuthenticator.CookieNonceAuthenticationResult.Error -> {
                WooLog.w(
                    tag = WooLog.T.LOGIN,
                    message = "Authentication failed, " +
                        "error: ${authenticationResult.type}, ${authenticationResult.message}"
                )
                Result.failure(authenticationResult.mapToException())
            }
        }
    }

    private suspend fun fetchSite(url: String, username: String?, password: String?): Result<SiteModel> {
        WooLog.d(WooLog.T.LOGIN, "Fetching site using WP REST API")

        return siteStore.fetchWPAPISite(
            FetchWPAPISitePayload(
                url = url,
                username = username,
                password = password
            )
        ).let { result ->
            when {
                result.isError -> {
                    WooLog.w(
                        tag = WooLog.T.LOGIN,
                        message = "Fetching site $url failed, error: ${result.error.type}, ${result.error.message}"
                    )

                    Result.failure(OnChangedException(result.error, message = result.error.message))
                }

                else -> {
                    WooLog.d(WooLog.T.LOGIN, "Site $url fetch succeeded")
                    val site = getSiteByUrl(url)!!
                    Result.success(site)
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun checkIfUserIsEligible(site: SiteModel): Result<Boolean> = coroutineScope {
        WooLog.d(WooLog.T.LOGIN, "Fetch user info to check if user is eligible for using the API")

        val applicationPasswordErrorTask = async {
            applicationPasswordsNotifier.passwordGenerationFailures.first()
        }

        return@coroutineScope userEligibilityFetcher.fetchUserInfo(site)
            .recoverCatching { exception ->
                // Wait for any potential application password generation error and report it as the cause of failure
                // We use the 100L waiting time just to make sure it's correctly reported before the network failure
                @Suppress("MagicNumber")
                withTimeoutOrNull(100L) { applicationPasswordErrorTask.join() }

                if (applicationPasswordErrorTask.isCompleted) {
                    throw applicationPasswordErrorTask.getCompleted()
                } else {
                    applicationPasswordErrorTask.cancel()
                    throw exception
                }
            }
            .onSuccess { applicationPasswordErrorTask.cancel() }
            .map { it.isEligible }
    }

    suspend fun getSiteByUrl(siteUrl: String): SiteModel? = withContext(Dispatchers.IO) {
        SiteUtils.getSiteByMatchingUrl(siteStore, siteUrl)
    }

    suspend fun getSiteByLocalId(id: Int): SiteModel? = withContext(Dispatchers.IO) {
        siteStore.getSiteByLocalId(id)
    }

    private fun CookieNonceAuthenticator.CookieNonceAuthenticationResult.Error.mapToException():
        CookieNonceAuthenticationException {
        val networkStatusCode = networkError?.volleyError?.networkResponse?.statusCode ?: run {
            if (type == Nonce.CookieNonceErrorType.NOT_AUTHENTICATED ||
                type == Nonce.CookieNonceErrorType.INVALID_RESPONSE
            ) {
                // If we don't have a network status code, and the error is either NOT_AUTHENTICATED or
                // INVALID_RESPONSE, we can assume the response was 200
                @Suppress("MagicNumber")
                200
            } else {
                null
            }
        }
        val errorMessage = when {
            type == Nonce.CookieNonceErrorType.NOT_AUTHENTICATED ->
                message?.let { UiStringText(it) } ?: UiStringRes(string.username_or_password_incorrect)

            type == Nonce.CookieNonceErrorType.INVALID_RESPONSE ->
                UiStringRes(string.login_site_credentials_invalid_response)

            type == Nonce.CookieNonceErrorType.CUSTOM_LOGIN_URL ->
                UiStringRes(string.login_site_credentials_custom_login_url)

            type == Nonce.CookieNonceErrorType.CUSTOM_ADMIN_URL ->
                UiStringRes(string.login_site_credentials_custom_admin_url)

            networkStatusCode != null ->
                UiStringRes(
                    string.login_site_credentials_http_error,
                    listOf(UiStringText(networkStatusCode.toString()))
                )

            else -> UiStringRes(string.error_generic)
        }
        return CookieNonceAuthenticationException(
            errorMessage,
            type.name,
            networkStatusCode
        )
    }

    data class CookieNonceAuthenticationException(
        val errorMessage: UiString,
        val errorType: String,
        val networkStatusCode: Int?
    ) : Exception((errorMessage as? UiStringText)?.text)
}
