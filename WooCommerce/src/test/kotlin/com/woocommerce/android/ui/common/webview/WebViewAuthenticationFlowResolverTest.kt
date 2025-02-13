package com.woocommerce.android.ui.common.webview

import com.woocommerce.android.tools.SelectedSite
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.given
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore

class WebViewAuthenticationFlowResolverTest {
    private val selectedSite = mock<SelectedSite>()
    private val accountStore = mock<AccountStore>()

    private val sut = WebViewAuthenticationFlowResolver(
        selectedSite = selectedSite,
        accountStore = accountStore
    )

    @Test
    fun `given WPCom authenticated and a URL with a wordpress_com domain, when resolving the authentication flow, then it should return WPCom`() {
        givenWPComAuthenticated()
        val url = "https://wordpress.com/test"

        val result = sut.resolve(url)

        assertThat(result).isEqualTo(WebViewAuthenticationFlowResolver.WebViewAuthenticationFlow.WPCom)
    }

    @Test
    fun `given WPCom authenticated and a URL with jetpack_com, when resolving the authentication flow, then it should return WPCom`() {
        givenWPComAuthenticated()
        val url = "https://jetpack.com/test"

        val result = sut.resolve(url)

        assertThat(result).isEqualTo(WebViewAuthenticationFlowResolver.WebViewAuthenticationFlow.WPCom)
    }

    @Test
    fun `given WPCom authenticated and a URL with woocommerce_com domain, when resolving the authentication flow, then it should return WPCom`() {
        givenWPComAuthenticated()
        val url = "https://woocommerce.com/test"

        val result = sut.resolve(url)

        assertThat(result).isEqualTo(WebViewAuthenticationFlowResolver.WebViewAuthenticationFlow.WPCom)
    }

    @Test
    fun `given WPCom authenticated and an atomic site, when resolving the authentication flow, then it should return WPCom`() {
        givenWPComAuthenticated()
        givenSite {
            setIsWPComAtomic(true)
            url = "https://example.com"
        }
        val url = "https://example.com/test"

        val result = sut.resolve(url)

        assertThat(result).isEqualTo(WebViewAuthenticationFlowResolver.WebViewAuthenticationFlow.WPCom)
    }

    @Test
    fun `given WPCom authenticated and a Jetpack SSO site, when resolving the authentication flow, then it should return JetpackSSO`() {
        givenWPComAuthenticated()
        givenSite {
            jetpackModules = "sso"
            url = "https://example.com"
        }
        val url = "https://example.com/test"

        val result = sut.resolve(url)

        assertThat(result).isEqualTo(WebViewAuthenticationFlowResolver.WebViewAuthenticationFlow.JetpackSSO)
    }

    @Test
    fun `given WPCom authenticated and a URL with a non-supported domain, when resolving the authentication flow, then it should return None`() {
        givenWPComAuthenticated()
        val url = "https://example.com/test"

        val result = sut.resolve(url)

        assertThat(result).isEqualTo(WebViewAuthenticationFlowResolver.WebViewAuthenticationFlow.None)
    }

    @Test
    fun `given a site with username and password, when resolving the authentication flow for a URL part of the site, then it should return SiteCredentials`() {
        givenSite {
            username = "username"
            password = "password"
            url = "https://example.com"
        }
        val url = "https://example.com/test"

        val result = sut.resolve(url)

        assertThat(result).isEqualTo(WebViewAuthenticationFlowResolver.WebViewAuthenticationFlow.SiteCredentials)
    }

    @Test
    fun `given a site with username and password, when resolving the authentication flow for a URL not part of the site, then it should return None`() {
        givenSite {
            username = "username"
            password = "password"
            url = "https://example.com"
        }
        val url = "https://another.com/test"

        val result = sut.resolve(url)

        assertThat(result).isEqualTo(WebViewAuthenticationFlowResolver.WebViewAuthenticationFlow.None)
    }

    @Test
    fun `given a subdomain of a site, when checking if the URL is part of the site, then it should return true`() {
        val site = SiteModel().apply { url = "https://example.com" }
        val url = "https://sub.example.com/path"

        val result = with(sut) {
            url.isPartOf(site)
        }

        assertThat(result).isTrue()
    }

    @Test
    fun `given a URL without www and site URL with it, when checking if the URL is part of the site, then it should return true`() {
        val site = SiteModel().apply { url = "https://www.example.com" }
        val url = "https://example.com/path"

        val result = with(sut) {
            url.isPartOf(site)
        }

        assertThat(result).isTrue()
    }

    private fun givenWPComAuthenticated() {
        given(accountStore.accessToken).willReturn("token")
        given(accountStore.account).willReturn(AccountModel().apply { userName = "username" })
    }

    private fun givenSite(siteBuilder: SiteModel.() -> Unit) {
        given(selectedSite.getOrNull()).willReturn(SiteModel().apply(siteBuilder))
    }
}
