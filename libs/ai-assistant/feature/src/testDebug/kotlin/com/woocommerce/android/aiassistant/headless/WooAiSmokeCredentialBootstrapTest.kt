@file:Suppress("FunctionNaming")

package com.woocommerce.android.aiassistant.headless

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.fluxc.model.SiteModel
import java.io.File

class WooAiSmokeCredentialBootstrapTest {
    @Test
    fun `given resolved wpcom site, when building persisted site, then selected target keeps resolved id and jetpack state`() {
        val resolvedSite = SiteModel().apply {
            id = LOCAL_SITE_ID
            siteId = RESOLVED_SITE_ID
            name = "Resolved Store"
            url = "https://primary.example"
            origin = SiteModel.ORIGIN_WPCOM_REST
            setIsJetpackInstalled(true)
            setIsJetpackConnected(true)
        }

        val site = WooAiSmokeCredentialBootstrap.siteForPersistence(
            credentials = credentials(),
            resolvedSite = resolvedSite,
        )

        assertThat(site.id).isEqualTo(LOCAL_SITE_ID)
        assertThat(site.siteId).isEqualTo(RESOLVED_SITE_ID)
        assertThat(site.name).isEqualTo("Resolved Store")
        assertThat(site.url).isEqualTo("https://store.example")
        assertThat(site.wpApiRestUrl).isEqualTo("https://store.example/wp-json/")
        assertThat(site.username).isEqualTo("merchant@example.com")
        assertThat(site.origin).isEqualTo(SiteModel.ORIGIN_WPCOM_REST)
        assertThat(site.isJetpackInstalled).isTrue
        assertThat(site.isJetpackConnected).isTrue
        assertThat(site.getHasWooCommerce()).isTrue
    }

    @Test
    fun `when preflight report is created, then wpcom jetpack routing intent is visible`() {
        val report = WooAiSmokePreflightReport(
            localSiteIdPresent = true,
            remoteSiteIdMatched = true,
            urlHostMatched = true,
            usernameMatched = true,
            siteOrigin = SiteModel.ORIGIN_WPCOM_REST,
            jetpackConnected = true,
            jetpackInstalled = true,
            wpComAccessTokenPresent = true,
            toolRegistryClass = "WooCommerceToolRegistry",
            authProviderClass = "AccessTokenWpComOAuthTokenProvider",
            toolTransportIntent = WooAiSmokeCredentialBootstrap.TOOL_TRANSPORT_INTENT,
            safeToolResults = listOf(
                WooAiSmokePreflightToolResult("products_list", "SUCCESS"),
                WooAiSmokePreflightToolResult("orders_list", "SUCCESS"),
                WooAiSmokePreflightToolResult("orders_list_pending", "SUCCESS"),
                WooAiSmokePreflightToolResult("analytics_orders", "SUCCESS"),
            ),
        )

        assertThat(report.siteOrigin).isEqualTo(SiteModel.ORIGIN_WPCOM_REST)
        assertThat(report.jetpackConnected).isTrue
        assertThat(report.jetpackInstalled).isTrue
        assertThat(report.wpComAccessTokenPresent).isTrue
        assertThat(report.toolTransportIntent).isEqualTo("WPCOM_REST_JETPACK_TUNNEL")
        assertThat(report.safeToolResults.map { it.toolName })
            .containsExactly("products_list", "orders_list", "orders_list_pending", "analytics_orders")
    }

    private fun credentials() = WooAiSmokeCredentialConfig(
        siteUrl = "https://store.example",
        wpComUsername = "merchant@example.com",
        wpComPassword = "app password",
        storeLabel = "store",
        outputDirectory = File("build/woo-ai-smoke"),
        credentialSource = "test",
    )

    private companion object {
        const val LOCAL_SITE_ID = 42
        const val RESOLVED_SITE_ID = 2922L
    }
}
