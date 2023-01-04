package com.woocommerce.android.ui.login

import com.woocommerce.android.OnChangedException
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.awaitAny
import com.woocommerce.android.util.awaitEvent
import com.woocommerce.android.util.dispatchAndAwait
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.AuthenticationActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore.OnAuthenticationChanged
import org.wordpress.android.fluxc.store.AccountStore.OnDiscoveryResponse
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.RefreshSitesXMLRPCPayload
import org.wordpress.android.login.util.SiteUtils
import javax.inject.Inject

class WPApiSiteRepository @Inject constructor(
    private val dispatcher: Dispatcher,
    private val siteStore: SiteStore
) {
    /**
     * Handles authentication to the given [url] using wp-admin credentials.
     * After calling this, and if the authentication is successful, a [SiteModel] matching this site will be persisted
     * in the DB.
     *
     * Note: this function uses XMLRPC behind the scenes
     */
    suspend fun login(url: String, username: String, password: String): Result<SiteModel> {
        WooLog.d(WooLog.T.LOGIN, "Authenticating in to site $url using site credentials")

        return discoverXMLRPCAddress(url)
            .mapCatching { xmlrpcEndpoint ->
                fetchXMLRPCSite(url, xmlrpcEndpoint, username, password).getOrThrow()
            }
    }

    private suspend fun discoverXMLRPCAddress(siteUrl: String): Result<String> {
        WooLog.d(WooLog.T.LOGIN, "Running discovery to fetch XMLRPC endpoint for site $siteUrl")

        val action = AuthenticationActionBuilder.newDiscoverEndpointAction(siteUrl)
        val event: OnDiscoveryResponse = dispatcher.dispatchAndAwait(action)

        return if (event.isError) {
            WooLog.w(WooLog.T.LOGIN, "XMLRPC Discovery failed, error: ${event.error}")
            Result.failure(OnChangedException(event.error, event.error.name))
        } else {
            WooLog.d(
                tag = WooLog.T.LOGIN,
                message = "XMLRPC Discovery succeeded, xmrlpc endpoint: ${event.xmlRpcEndpoint}"
            )
            Result.success(event.xmlRpcEndpoint)
        }
    }

    @Suppress("ReturnCount")
    private suspend fun fetchXMLRPCSite(
        siteUrl: String,
        xmlrpcEndpoint: String,
        username: String,
        password: String
    ): Result<SiteModel> = coroutineScope {
        val authenticationTask = async {
            dispatcher.awaitEvent<OnAuthenticationChanged>().also { event ->
                if (event.isError) {
                    WooLog.w(
                        tag = WooLog.T.LOGIN,
                        message = "Authenticating to XMLRPC site $xmlrpcEndpoint failed, " +
                            "error: ${event.error.message}"
                    )
                }
            }
        }
        val siteTask = async(Dispatchers.IO) {
            siteStore.fetchSitesXmlRpc(
                RefreshSitesXMLRPCPayload(
                    username = username,
                    password = password,
                    url = xmlrpcEndpoint
                )
            ).also { event ->
                if (event.isError) {
                    WooLog.w(
                        tag = WooLog.T.LOGIN,
                        message = "XMLRPC site $xmlrpcEndpoint fetch failed, error: ${event.error.message}"
                    )
                } else {
                    WooLog.d(WooLog.T.LOGIN, "XMLRPC site $xmlrpcEndpoint fetch succeeded")
                }
            }
        }

        val tasks = listOf(authenticationTask, siteTask)

        val event = tasks.awaitAny()

        return@coroutineScope when {
            event.isError -> Result.failure(OnChangedException(event.error))
            else -> {
                WooLog.d(WooLog.T.LOGIN, "XMLRPC site $siteUrl fetch succeeded")
                val site = withContext(Dispatchers.IO) { SiteUtils.getXMLRPCSiteByUrl(siteStore, siteUrl)!! }
                return@coroutineScope Result.success(site)
            }
        }
    }
}
