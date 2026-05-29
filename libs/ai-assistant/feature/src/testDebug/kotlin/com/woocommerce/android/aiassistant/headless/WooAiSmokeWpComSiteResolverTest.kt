@file:Suppress("FunctionNaming")

package com.woocommerce.android.aiassistant.headless

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import java.io.File

class WooAiSmokeWpComSiteResolverTest {
    @Test
    fun `given wpcom rest jetpack site from me sites, when resolving, then selected site is returned`() = runTest {
        val site = site(url = "https://store.example", siteId = RESOLVED_SITE_ID)
        val resolver = WooAiSmokeWpComSiteResolver(FakeSiteLookup(updatedSites = listOf(site)))

        val resolved = resolver.resolve(credentials())

        assertThat(resolved.siteId).isEqualTo(RESOLVED_SITE_ID)
        assertThat(resolved.origin).isEqualTo(SiteModel.ORIGIN_WPCOM_REST)
        assertThat(resolved.isJetpackConnected).isTrue
    }

    @Test
    fun `given same host url variants from me sites, when resolving, then scheme differences still match`() =
        runTest {
            val variants = listOf(
                UrlVariant(
                    configuredUrl = "store.example",
                    returnedUrl = "https://www.store.example/wp-admin",
                    siteId = RESOLVED_SITE_ID,
                ),
                UrlVariant(
                    configuredUrl = "https://store.example",
                    returnedUrl = "store.example",
                    siteId = SCHEMELESS_RETURNED_SITE_ID,
                ),
            )

            variants.forEach { variant ->
                val resolver = WooAiSmokeWpComSiteResolver(
                    FakeSiteLookup(updatedSites = listOf(site(url = variant.returnedUrl, siteId = variant.siteId)))
                )

                val resolved = resolver.resolve(credentials(siteUrl = variant.configuredUrl))

                assertThat(resolved.siteId).isEqualTo(variant.siteId)
            }
        }

    @Test
    fun `given matching site is not wpcom rest jetpack, when resolving, then resolver rejects it`() = runTest {
        val invalidSites = listOf(
            site(origin = SiteModel.ORIGIN_WPAPI) to "WPCOM_SITE_INVALID_ORIGIN",
            site(jetpackConnected = false) to "WPCOM_SITE_NOT_JETPACK_CONNECTED",
        )

        invalidSites.forEach { (invalidSite, expectedError) ->
            val resolver = WooAiSmokeWpComSiteResolver(FakeSiteLookup(updatedSites = listOf(invalidSite)))

            val error = runCatching { resolver.resolve(credentials()) }.exceptionOrNull()

            assertThat(error).hasMessageContaining(expectedError)
        }
    }

    @Test
    fun `given configured site is absent from me sites, when resolving, then failure lists candidates`() = runTest {
        val resolver = WooAiSmokeWpComSiteResolver(
            FakeSiteLookup(updatedSites = listOf(site(url = "https://other.example", siteId = 101L)))
        )

        val error = runCatching { resolver.resolve(credentials()) }.exceptionOrNull()

        assertThat(error)
            .hasMessageContaining("WPCOM_SITE_NOT_FOUND")
            .hasMessageContaining("Candidates:")
            .hasMessageContaining("host=other.example")
            .hasMessageContaining("siteId=101")
            .hasMessageContaining("jetpackConnected=true")
    }

    private fun credentials(
        siteUrl: String = "https://store.example",
    ) = WooAiSmokeCredentialConfig(
        siteUrl = siteUrl,
        wpComUsername = "merchant@example.com",
        wpComPassword = "app password",
        storeLabel = "store",
        outputDirectory = File("build/woo-ai-smoke"),
        credentialSource = "test",
    )

    private fun site(
        url: String = "https://store.example",
        siteId: Long = RESOLVED_SITE_ID,
        origin: Int = SiteModel.ORIGIN_WPCOM_REST,
        jetpackConnected: Boolean = true,
        jetpackInstalled: Boolean = true,
    ) = SiteModel().apply {
        this.siteId = siteId
        this.url = url
        this.origin = origin
        setIsJetpackConnected(jetpackConnected)
        setIsJetpackInstalled(jetpackInstalled)
    }

    private class FakeSiteLookup(
        private val updatedSites: List<SiteModel> = emptyList(),
        private val persistedSites: List<SiteModel> = emptyList(),
    ) : WooAiSmokeWpComSiteResolver.SiteLookup {
        override suspend fun fetchSites(): SiteStore.OnSiteChanged =
            SiteStore.OnSiteChanged(updatedSites = updatedSites)

        override fun persistedWpComRestSites(): List<SiteModel> = persistedSites
    }

    private data class UrlVariant(
        val configuredUrl: String,
        val returnedUrl: String,
        val siteId: Long,
    )

    private companion object {
        const val RESOLVED_SITE_ID = 2922L
        const val SCHEMELESS_RETURNED_SITE_ID = 2923L
    }
}
