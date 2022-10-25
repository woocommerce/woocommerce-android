package com.woocommerce.android.ui.login.accountmismatch

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
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore.OnAuthenticationChanged
import org.wordpress.android.fluxc.store.AccountStore.OnDiscoveryResponse
import org.wordpress.android.fluxc.store.JetpackStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged
import org.wordpress.android.fluxc.store.SiteStore.RefreshSitesXMLRPCPayload
import org.wordpress.android.login.util.SiteUtils
import javax.inject.Inject

class AccountMismatchRepository @Inject constructor(
    private val jetpackStore: JetpackStore,
    private val siteStore: SiteStore,
    private val dispatcher: Dispatcher
) {
    suspend fun getSiteByUrl(url: String): SiteModel? = withContext(Dispatchers.IO) {
        SiteUtils.getSiteByMatchingUrl(siteStore, url)
    }

    suspend fun fetchJetpackConnectionUrl(site: SiteModel): Result<String> {
        WooLog.d(WooLog.T.LOGIN, "Fetching Jetpack Connection URL")
        val result = jetpackStore.fetchJetpackConnectionUrl(site, autoRegisterSiteIfNeeded = true)
        return when {
            result.isError -> {
                WooLog.w(WooLog.T.LOGIN, "Fetching Jetpack Connection URL failed: ${result.error.message}")
                Result.failure(OnChangedException(result.error, result.error.message))
            }
            else -> {
                WooLog.d(WooLog.T.LOGIN, "Jetpack connection URL fetched successfully")
                Result.success(result.url)
            }
        }
    }

    suspend fun fetchJetpackConnectedEmail(site: SiteModel): Result<String> {
        WooLog.d(WooLog.T.LOGIN, "Fetching email of Jetpack User")
        val result = jetpackStore.fetchJetpackUser(site)
        return when {
            result.isError -> {
                WooLog.w(WooLog.T.LOGIN, "Fetching Jetpack User failed error: $result.error.message")
                Result.failure(OnChangedException(result.error, result.error.message))
            }
            result.user?.wpcomEmail.isNullOrEmpty() -> {
                WooLog.w(WooLog.T.LOGIN, "Cannot find Jetpack Email in response")
                Result.failure(Exception("Email missing from response"))
            }
            else -> {
                WooLog.d(WooLog.T.LOGIN, "Jetpack User fetched successfully")
                Result.success(result.user!!.wpcomEmail)
            }
        }
    }

    suspend fun checkJetpackConnection(
        siteUrl: String,
        username: String,
        password: String
    ): Result<JetpackConnectionStatus> {
        WooLog.d(WooLog.T.LOGIN, "Checking Jetpack Connection status for site $siteUrl")

        return discoverXMLRPCAddress(siteUrl)
            .mapCatching { xmlrpcEndpoint ->
                val xmlrpcSite = fetchXMLRPCSite(siteUrl, xmlrpcEndpoint, username, password).getOrThrow()
                when {
                    xmlrpcSite.jetpackUserEmail.isNullOrEmpty() -> {
                        WooLog.d(WooLog.T.LOGIN, "Jetpack site is not connected to a WPCom account")
                        JetpackConnectionStatus.NotConnected
                    }
                    else -> {
                        WooLog.d(
                            tag = WooLog.T.LOGIN,
                            message = "Jetpack site is connected to different WPCom account:" +
                                " ${xmlrpcSite.jetpackUserEmail}"
                        )
                        JetpackConnectionStatus.ConnectedToDifferentAccount(xmlrpcSite.jetpackUserEmail)
                    }
                }
            }
    }

    /**
     * Represents Jetpack Connection status for the current wp-admin account
     */
    sealed interface JetpackConnectionStatus {
        object NotConnected : JetpackConnectionStatus
        data class ConnectedToDifferentAccount(val wpcomEmail: String) : JetpackConnectionStatus
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
        val siteTask = async {
            val selfHostedPayload = RefreshSitesXMLRPCPayload(
                username = username,
                password = password,
                url = xmlrpcEndpoint
            )
            dispatcher.dispatchAndAwait<RefreshSitesXMLRPCPayload, OnSiteChanged>(
                action = SiteActionBuilder.newFetchSitesXmlRpcAction(
                    selfHostedPayload
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

        if (event.isError) return@coroutineScope Result.failure(OnChangedException(event.error))

        return@coroutineScope fetchSiteOptions(siteUrl)
    }

    private suspend fun fetchSiteOptions(siteUrl: String): Result<SiteModel> {
        WooLog.d(WooLog.T.LOGIN, "Fetch XMLRPC site options")
        val site = getSiteByUrl(siteUrl) ?: run {
            WooLog.e(WooLog.T.LOGIN, "XMLRPC site null after fetching!!")
            return Result.failure(IllegalStateException("XMLRPC Site not found"))
        }

        val fetchSiteResult = siteStore.fetchSite(site)
        return when {
            fetchSiteResult.isError -> {
                WooLog.w(
                    tag = WooLog.T.LOGIN,
                    message = "XMLRPC site $siteUrl fetch failed, error: ${fetchSiteResult.error.message}"
                )
                Result.failure(OnChangedException(fetchSiteResult.error, fetchSiteResult.error.message))
            }
            else -> {
                WooLog.d(WooLog.T.LOGIN, "XMLRPC site $siteUrl fetch succeeded")
                Result.success(getSiteByUrl(siteUrl)!!)
            }
        }
    }
}
