package com.woocommerce.android.aiassistant.auth

import com.woocommerce.android.aiassistant.core.auth.AssistantAuthException
import com.woocommerce.android.aiassistant.core.auth.JwtTokenProvider
import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.wordpress.android.fluxc.model.JWTToken
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.jetpackai.JetpackAIRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.jetpackai.JetpackAIRestClient.JetpackAIJWTTokenResponse
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [JwtTokenProvider] that mints and caches a Jetpack AI JWT for the currently
 * selected WP.com site.
 *
 * Behavior:
 *  - Caches one [JWTToken] in memory; reuses it until it expires or its
 *    embedded `blog_id` no longer matches the active site (e.g. after a site
 *    switch).
 *  - Single-flights concurrent callers via [Mutex] so a token storm produces
 *    one mint, not N.
 *  - Throws [AssistantAuthException] for every recoverable failure surface
 *    (no selected site, missing/invalid token, REST error). The chat layer
 *    converts that to `ChatStreamError.AUTH`.
 */
@Singleton
internal class WpComJetpackAiTokenProvider @Inject constructor(
    private val selectedSite: SelectedSite,
    private val restClient: JetpackAIRestClient,
) : JwtTokenProvider {

    private val mutex = Mutex()

    @Volatile
    private var cached: JWTToken? = null

    override suspend fun provide(): String = mutex.withLock {
        val site = selectedSite.getOrNull()
            ?: throw AssistantAuthException("No selected site; cannot mint Jetpack AI JWT")

        cached?.takeIf { it.matchesSite(site) }
            ?.validateOrThrow()
            ?.let { return@withLock it.value }

        val fresh = mintToken(site)
        cached = fresh
        fresh.value
    }

    override suspend fun invalidate() = mutex.withLock {
        cached = null
    }

    private suspend fun mintToken(site: SiteModel): JWTToken =
        when (val response = restClient.fetchJetpackAIJWTToken(site)) {
            is JetpackAIJWTTokenResponse.Success -> response.token.validateOrThrow()
                ?: throw AssistantAuthException("Jetpack AI JWT was returned already expired")
            is JetpackAIJWTTokenResponse.Error -> throw AssistantAuthException(
                "Failed to mint Jetpack AI JWT: ${response.type} ${response.message.orEmpty()}".trim()
            )
        }

    private fun JWTToken.validateOrThrow(): JWTToken? = runCatching {
        validateExpiryDate()
    }.getOrElse { throw AssistantAuthException("Invalid Jetpack AI JWT", it) }

    private fun JWTToken.matchesSite(site: SiteModel): Boolean = runCatching {
        getPayloadItem("blog_id")?.toLongOrNull() == site.siteId
    }.getOrElse { throw AssistantAuthException("Invalid Jetpack AI JWT", it) }
}
