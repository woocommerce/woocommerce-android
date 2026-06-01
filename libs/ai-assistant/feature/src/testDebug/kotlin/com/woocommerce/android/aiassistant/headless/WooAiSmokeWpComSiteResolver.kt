package com.woocommerce.android.aiassistant.headless

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import java.net.URI
import javax.inject.Inject

internal class WooAiSmokeWpComSiteResolver internal constructor(
    private val lookup: SiteLookup,
) {
    @Inject
    constructor(siteStore: SiteStore) : this(FluxCSiteLookup(siteStore))

    suspend fun resolve(credentials: WooAiSmokeCredentialConfig): SiteModel {
        val sitesChanged = lookup.fetchSites()
        require(!sitesChanged.isError) {
            val error = sitesChanged.error
            "WPCOM_SITE_RESOLUTION_FAILED: /me/sites returned ${error?.type ?: "UNKNOWN"} " +
                error?.message.orEmpty()
        }

        val candidates = (sitesChanged.updatedSites + lookup.persistedWpComRestSites())
            .distinctBy { it.siteId to normalizedHost(it.url) }
        val matchingUrlSites = candidates.filter { normalizedHost(it.url) == normalizedHost(credentials.siteUrl) }
        val site = matchingUrlSites.firstOrNull { it.isValidWpComJetpackSite() }
            ?: rejectInvalidMatchOrMissing(credentials, matchingUrlSites, candidates)

        require(site.siteId > 0L) { "WPCOM_SITE_RESOLUTION_FAILED: resolved site id missing" }
        return site
    }

    private fun rejectInvalidMatchOrMissing(
        credentials: WooAiSmokeCredentialConfig,
        matchingUrlSites: List<SiteModel>,
        candidates: List<SiteModel>,
    ): Nothing {
        val invalidMatch = matchingUrlSites.firstOrNull()
        val message = when {
            invalidMatch == null -> notFoundMessage(credentials, candidates)
            invalidMatch.origin != SiteModel.ORIGIN_WPCOM_REST ->
                "WPCOM_SITE_INVALID_ORIGIN: resolved site is not ORIGIN_WPCOM_REST."
            !invalidMatch.isJetpackInstalled ->
                "WPCOM_SITE_NOT_JETPACK_INSTALLED: resolved site is not Jetpack installed."
            !invalidMatch.isJetpackConnected ->
                "WPCOM_SITE_NOT_JETPACK_CONNECTED: resolved site is not Jetpack connected."
            invalidMatch.siteId <= 0L -> "WPCOM_SITE_RESOLUTION_FAILED: resolved site id missing."
            else -> "WPCOM_SITE_RESOLUTION_FAILED: resolved site did not pass validation."
        }
        error(message)
    }

    private fun notFoundMessage(
        credentials: WooAiSmokeCredentialConfig,
        candidates: List<SiteModel>,
    ): String =
        "WPCOM_SITE_NOT_FOUND: ${credentials.siteUrl} was not found in /me/sites. " +
            "Candidates: ${candidates.toDiagnosticList()}"

    private fun SiteModel.isValidWpComJetpackSite(): Boolean =
        origin == SiteModel.ORIGIN_WPCOM_REST &&
            isJetpackInstalled &&
            isJetpackConnected &&
            siteId > 0L

    private fun normalizedHost(url: String): String =
        runCatching { url.trim().hostFromUri() }
            .getOrNull()
            .orEmpty()
            .removePrefix("www.")
            .lowercase()

    private fun String.hostFromUri(): String? {
        val parsed = URI(this).host
        return parsed ?: URI("https://$this").host
    }

    private fun List<SiteModel>.toDiagnosticList(): String =
        joinToString(
            prefix = "[",
            postfix = "]",
            limit = CANDIDATE_DIAGNOSTIC_LIMIT,
            truncated = ", ...",
        ) { site ->
            "{url=${site.url}, host=${normalizedHost(site.url)}, siteId=${site.siteId}, origin=${site.origin}, " +
                "jetpackInstalled=${site.isJetpackInstalled}, jetpackConnected=${site.isJetpackConnected}}"
        }

    internal interface SiteLookup {
        suspend fun fetchSites(): SiteStore.OnSiteChanged
        fun persistedWpComRestSites(): List<SiteModel>
    }

    private class FluxCSiteLookup(
        private val siteStore: SiteStore,
    ) : SiteLookup {
        override suspend fun fetchSites(): SiteStore.OnSiteChanged =
            siteStore.fetchSites(SiteStore.FetchSitesPayload())

        override fun persistedWpComRestSites(): List<SiteModel> =
            siteStore.sitesAccessedViaWPComRest
    }

    private companion object {
        const val CANDIDATE_DIAGNOSTIC_LIMIT = 20
    }
}
