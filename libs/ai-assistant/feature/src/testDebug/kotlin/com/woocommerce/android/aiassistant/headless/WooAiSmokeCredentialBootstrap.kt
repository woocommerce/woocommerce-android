package com.woocommerce.android.aiassistant.headless

import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.core.chat.ToolRegistry
import com.woocommerce.android.aiassistant.core.chat.ToolResult
import com.woocommerce.android.aiassistant.tools.WooCommerceToolRegistry
import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import java.net.URI
import java.time.LocalDate
import kotlin.time.Duration.Companion.seconds

internal class WooAiSmokeCredentialBootstrap(
    private val siteStore: SiteStore,
    private val selectedSite: SelectedSite,
    private val toolRegistry: ToolRegistry,
) {
    @Suppress("LongMethod")
    suspend fun bootstrap(
        credentials: WooAiSmokeCredentialConfig,
        resolvedSite: SiteModel,
        wpComAccessTokenPresent: Boolean,
    ): WooAiSmokeBootstrapResult {
        val site = persistedSite(credentials, resolvedSite)

        require(site.id > 0) { "SITE_FETCH_FAILED: persisted local site id missing" }
        require(normalizedHost(site.url) == normalizedHost(credentials.siteUrl)) {
            "SITE_FETCH_FAILED: fetched site host did not match credential host"
        }

        site.siteId = resolvedSite.siteId
        site.url = credentials.siteUrl
        site.username = credentials.wpComUsername
        site.setHasWooCommerce(true)

        selectedSite.set(site)
        val selected = selectedSite.get()
        require(selected.id == site.id) { "SELECTED_SITE_LOCAL_ID_MISMATCH" }
        require(selected.siteId == resolvedSite.siteId) { "SELECTED_SITE_REMOTE_ID_MISMATCH" }
        require(selected.url == site.url) { "SELECTED_SITE_URL_MISMATCH" }
        require(selected.username == credentials.wpComUsername) { "SELECTED_SITE_USERNAME_MISMATCH" }

        require(toolRegistry is WooCommerceToolRegistry) {
            "Expected WooCommerceToolRegistry, found ${toolRegistry::class.java.simpleName}"
        }
        require(toolRegistry.descriptors().size == EXPECTED_TOOL_CATALOG_SIZE) {
            "Expected $EXPECTED_TOOL_CATALOG_SIZE AI tools, found ${toolRegistry.descriptors().size}"
        }

        val productResult = executePreflight("preflight-products-list", "products_list", productsListArguments())
        val ordersResult = executePreflight("preflight-orders-list", "orders_list", ordersListArguments())
        val pendingOrdersResult = executePreflight(
            "preflight-orders-pending-list",
            "orders_list",
            pendingOrdersListArguments(),
        )
        val analyticsResult = executePreflight("preflight-analytics-orders", "analytics_orders", analyticsArguments())

        return WooAiSmokeBootstrapResult(
            site = selected,
            preflight = WooAiSmokePreflightReport(
                localSiteIdPresent = selected.id > 0,
                remoteSiteIdMatched = selected.siteId == resolvedSite.siteId,
                urlHostMatched = normalizedHost(selected.url) == normalizedHost(credentials.siteUrl),
                usernameMatched = selected.username == credentials.wpComUsername,
                siteOrigin = selected.origin,
                jetpackConnected = selected.isJetpackConnected,
                jetpackInstalled = selected.isJetpackInstalled,
                wpComAccessTokenPresent = wpComAccessTokenPresent,
                toolRegistryClass = toolRegistry::class.java.simpleName,
                authProviderClass = "AccessTokenWpComOAuthTokenProvider",
                toolTransportIntent = TOOL_TRANSPORT_INTENT,
                safeToolResults = listOf(
                    WooAiSmokePreflightToolResult("products_list", productResult.kindName()),
                    WooAiSmokePreflightToolResult("orders_list", ordersResult.kindName()),
                    WooAiSmokePreflightToolResult("orders_list_pending", pendingOrdersResult.kindName()),
                    WooAiSmokePreflightToolResult("analytics_orders", analyticsResult.kindName()),
                ),
            ),
        )
    }

    private fun persistedSite(
        credentials: WooAiSmokeCredentialConfig,
        resolvedSite: SiteModel,
    ): SiteModel {
        val site = siteForPersistence(credentials, resolvedSite)
        siteStore.onAction(SiteActionBuilder.newUpdateSiteAction(site))
        return siteStore.getSiteBySiteId(resolvedSite.siteId)
            ?: error("SITE_FETCH_FAILED: persisted local site id missing")
    }

    private suspend fun executePreflight(
        id: String,
        name: String,
        arguments: JsonObject,
    ): ToolResult {
        val result = try {
            withTimeout(PREFLIGHT_TIMEOUT) {
                toolRegistry.execute(ToolCall(id = id, name = name, arguments = arguments))
            }
        } catch (_: TimeoutCancellationException) {
            error("PREFLIGHT_TIMEOUT: $name")
        }
        require(result is ToolResult.Success) {
            "PREFLIGHT_FAILED: $name returned ${result.kindName()}"
        }
        return result
    }

    private fun productsListArguments() = buildJsonObject {
        put("search", "Cappuccino")
        put("per_page", 1)
    }

    private fun ordersListArguments() = buildJsonObject {
        put("status", "any")
        put("per_page", 1)
    }

    private fun pendingOrdersListArguments() = buildJsonObject {
        put("status", "pending")
        put("per_page", 1)
    }

    private fun analyticsArguments(): JsonObject {
        val today = LocalDate.now()
        return buildJsonObject {
            put("after", today.withDayOfMonth(1).toString())
            put("before", today.toString())
            put("interval", "day")
        }
    }

    private fun ToolResult.kindName(): String = when (this) {
        is ToolResult.Success -> "SUCCESS"
        is ToolResult.ValidationError -> "VALIDATION_ERROR"
        is ToolResult.TransportError -> "TRANSPORT_ERROR"
        is ToolResult.RejectedBySafety -> "REJECTED_BY_SAFETY"
    }

    private fun normalizedHost(url: String): String =
        URI(url.trim()).host.orEmpty().removePrefix("www.").lowercase()

    internal companion object {
        fun siteForPersistence(
            credentials: WooAiSmokeCredentialConfig,
            resolvedSite: SiteModel,
        ): SiteModel = resolvedSite.apply {
            siteId = resolvedSite.siteId
            url = credentials.siteUrl
            wpApiRestUrl = credentials.siteUrl.trimEnd('/') + "/wp-json/"
            username = credentials.wpComUsername
            origin = SiteModel.ORIGIN_WPCOM_REST
            setIsJetpackInstalled(true)
            setIsJetpackConnected(true)
            setHasWooCommerce(true)
        }

        private const val EXPECTED_TOOL_CATALOG_SIZE = 13
        internal const val TOOL_TRANSPORT_INTENT = "WPCOM_REST_JETPACK_TUNNEL"
        private val PREFLIGHT_TIMEOUT = 45.seconds
    }
}

internal data class WooAiSmokeBootstrapResult(
    val site: SiteModel,
    val preflight: WooAiSmokePreflightReport,
)

@Serializable
data class WooAiSmokePreflightReport(
    val localSiteIdPresent: Boolean,
    val remoteSiteIdMatched: Boolean,
    val urlHostMatched: Boolean,
    val usernameMatched: Boolean,
    val siteOrigin: Int,
    val jetpackConnected: Boolean,
    val jetpackInstalled: Boolean,
    val wpComAccessTokenPresent: Boolean,
    val toolRegistryClass: String,
    val authProviderClass: String,
    val toolTransportIntent: String,
    val safeToolResults: List<WooAiSmokePreflightToolResult>,
)

@Serializable
data class WooAiSmokePreflightToolResult(
    val toolName: String,
    val resultKind: String,
)
