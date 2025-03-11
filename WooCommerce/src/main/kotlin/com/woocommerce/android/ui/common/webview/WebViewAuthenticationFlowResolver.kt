package com.woocommerce.android.ui.common.webview

import androidx.annotation.VisibleForTesting
import com.woocommerce.android.extensions.isNotNullOrEmpty
import com.woocommerce.android.tools.SelectedSite
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import javax.inject.Inject

class WebViewAuthenticationFlowResolver @Inject constructor(
    private val selectedSite: SelectedSite,
    private val accountStore: AccountStore
) {
    // A list of domains that we know that wordpress.com supports redirecting to
    private val wpComAuthAcceptedDomains
        get() = listOf("wordpress.com", "wp.com", "jetpack.com", "jetpack.wordpress.com", "woocommerce.com")

    fun resolve(url: String): WebViewAuthenticationFlow {
        val currentSite = selectedSite.getOrNull()
        val urlDomain = url.findDomain()
        val isWPComAuthenticated = accountStore.accessToken.isNotNullOrEmpty() &&
            accountStore.account.userName.isNotNullOrEmpty()

        return when {
            isWPComAuthenticated && wpComAuthAcceptedDomains.any { it == urlDomain } -> {
                WebViewAuthenticationFlow.WPCom
            }

            isWPComAuthenticated && currentSite?.supportsJetpackSSO() == true && url.isPartOf(currentSite) -> {
                WebViewAuthenticationFlow.JetpackSSO
            }

            currentSite?.hasCredentials() == true && url.isPartOf(currentSite) -> {
                WebViewAuthenticationFlow.SiteCredentials
            }

            else -> {
                WebViewAuthenticationFlow.None
            }
        }
    }

    @VisibleForTesting
    fun String.isPartOf(site: SiteModel): Boolean {
        // This is a simple check, so it could miss some edge cases, but it should be good enough for our use-case
        // We are using contains instead of equals to account for potential subdomains
        return findDomain().contains(site.url.findDomain())
    }

    private fun String.findDomain(): String {
        val urlWithScheme = if (contains("://")) this else "http://$this"
        return urlWithScheme.toHttpUrl().host.substringAfter("www.")
    }

    private fun SiteModel.supportsJetpackSSO(): Boolean {
        return jetpackModules?.contains("sso") == true
    }

    private fun SiteModel.hasCredentials(): Boolean {
        return username.isNotNullOrEmpty() && password.isNotNullOrEmpty()
    }

    enum class WebViewAuthenticationFlow {
        WPCom, JetpackSSO, SiteCredentials, None
    }
}
