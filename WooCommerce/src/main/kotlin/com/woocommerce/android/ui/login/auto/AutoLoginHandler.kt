package com.woocommerce.android.ui.login.auto

import android.net.Uri
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.login.WPApiSiteRepository
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class AutoLoginHandler @Inject constructor(
    private val wpApiSiteRepository: WPApiSiteRepository,
    private val selectedSite: SelectedSite
) {
    companion object {
        private const val PARAM_SITE = "site"
        private const val PARAM_USER = "user"
        private const val PARAM_PASSWORD = "password"
    }

    fun isAutoLoginUri(uri: Uri?): Boolean {
        return uri?.path == "/mobile/auto-login"
    }

    fun handleAutoLogin(
        uri: Uri,
        scope: CoroutineScope,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        scope.launch {
            val result = login(uri)
            when (result) {
                is AutoLoginResult.Success -> onSuccess()
                is AutoLoginResult.AlreadyLoggedIn -> onSuccess()
                is AutoLoginResult.Error -> onError(result.message)
            }
        }
    }

    private sealed class AutoLoginResult {
        data object Success : AutoLoginResult()
        data object AlreadyLoggedIn : AutoLoginResult()
        data class Error(val message: String) : AutoLoginResult()
    }

    private suspend fun login(uri: Uri): AutoLoginResult = withContext(Dispatchers.IO) {
        WooLog.d(WooLog.T.LOGIN, "AutoLoginHandler: Processing auto-login URI")

        if (selectedSite.exists()) {
            WooLog.d(WooLog.T.LOGIN, "AutoLoginHandler: User already logged in")
            return@withContext AutoLoginResult.AlreadyLoggedIn
        }

        val siteUrl = uri.getQueryParameter(PARAM_SITE)
        val username = uri.getQueryParameter(PARAM_USER)
        val password = uri.getQueryParameter(PARAM_PASSWORD)

        if (siteUrl.isNullOrBlank() || username.isNullOrBlank() || password.isNullOrBlank()) {
            WooLog.e(WooLog.T.LOGIN, "AutoLoginHandler: Missing required parameters")
            return@withContext AutoLoginResult.Error("Missing required parameters: site, user, or password")
        }

        WooLog.d(WooLog.T.LOGIN, "AutoLoginHandler: Fetching site $siteUrl")

        val fetchSiteResult = wpApiSiteRepository.fetchSite(
            url = siteUrl,
            username = username,
            password = password
        )

        fetchSiteResult.fold(
            onSuccess = { site ->
                if (!site.hasWooCommerce) {
                    WooLog.e(WooLog.T.LOGIN, "AutoLoginHandler: Site does not have WooCommerce")
                    return@withContext AutoLoginResult.Error("Site does not have WooCommerce installed")
                }

                WooLog.d(WooLog.T.LOGIN, "AutoLoginHandler: Saving application password")
                wpApiSiteRepository.saveApplicationPassword(site.id, username, password)

                WooLog.d(WooLog.T.LOGIN, "AutoLoginHandler: Checking user eligibility")
                val eligibilityResult = wpApiSiteRepository.checkIfUserIsEligible(site)

                eligibilityResult.fold(
                    onSuccess = { isEligible ->
                        if (!isEligible) {
                            WooLog.e(WooLog.T.LOGIN, "AutoLoginHandler: User is not eligible")
                            return@withContext AutoLoginResult.Error("User does not have required permissions")
                        }

                        WooLog.d(WooLog.T.LOGIN, "AutoLoginHandler: Setting selected site")
                        selectedSite.set(site)

                        WooLog.d(WooLog.T.LOGIN, "AutoLoginHandler: Login successful")
                        AutoLoginResult.Success
                    },
                    onFailure = { exception ->
                        WooLog.e(WooLog.T.LOGIN, "AutoLoginHandler: Failed to check user eligibility", exception)
                        AutoLoginResult.Error("Failed to verify user: ${exception.message}")
                    }
                )
            },
            onFailure = { exception ->
                WooLog.e(WooLog.T.LOGIN, "AutoLoginHandler: Failed to fetch site", exception)
                AutoLoginResult.Error("Failed to connect to site: ${exception.message}")
            }
        )
    }
}
