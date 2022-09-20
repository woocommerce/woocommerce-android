package com.woocommerce.android.ui.login.accountmismatch

import com.woocommerce.android.OnChangedException
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.AuthenticationActionBuilder
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore.OnDiscoveryResponse
import org.wordpress.android.fluxc.store.JetpackStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged
import org.wordpress.android.fluxc.store.SiteStore.RefreshSitesXMLRPCPayload
import org.wordpress.android.login.util.SiteUtils
import javax.inject.Inject
import kotlin.coroutines.resume


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
        val result = jetpackStore.fetchJetpackConnectionUrl(site)
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
    ): Result<JetpackAccountConnectionStatus> {
        WooLog.d(WooLog.T.LOGIN, "Checking Jetpack Connection status for site $siteUrl")

        return discoverXMlRPCAddress(siteUrl)
            .map { xmlrpcEndpoint ->
                fetchXMLRPCSite(xmlrpcEndpoint, username, password)
            }.mapCatching {
                withContext(Dispatchers.IO) {
                    val xmlrpcSite = SiteUtils.getXMLRPCSiteByUrl(siteStore, siteUrl)
                    when {
                        xmlrpcSite == null -> {
                            WooLog.e(WooLog.T.LOGIN, "XMLRPC site null after fetching!!")
                            throw IllegalStateException("XMLRPC Site not found")
                        }
                        xmlrpcSite.jetpackUserEmail.isNullOrEmpty() -> {
                            WooLog.d(WooLog.T.LOGIN, "Jetpack site is not connected to a WPCom account")
                            JetpackAccountConnectionStatus.JetpackNotConnected
                        }
                        else -> {
                            WooLog.d(
                                tag = WooLog.T.LOGIN,
                                message = "Jetpack site is connected to different WPCom account:" +
                                    " ${xmlrpcSite.jetpackUserEmail}"
                            )
                            JetpackAccountConnectionStatus.JetpackConnectedToAccount(xmlrpcSite.jetpackUserEmail)
                        }
                    }
                }
            }
    }

    sealed interface JetpackAccountConnectionStatus {
        object JetpackNotConnected : JetpackAccountConnectionStatus
        data class JetpackConnectedToAccount(val wpcomEmail: String) : JetpackAccountConnectionStatus
    }

    private suspend fun discoverXMlRPCAddress(siteUrl: String): Result<String> = suspendCancellableCoroutine { cont ->
        val listener = object : Any() {
            @Subscribe(threadMode = MAIN)
            @Suppress("unused")
            fun onDiscoverySucceeded(event: OnDiscoveryResponse) {
                dispatcher.unregister(this)
                if (event.isError) {
                    WooLog.w(WooLog.T.LOGIN, "XMLRPC Discovery failed, error: ${event.error}")
                    cont.resume(Result.failure(OnChangedException(event.error, event.error.name)))
                } else {
                    WooLog.d(
                        tag = WooLog.T.LOGIN,
                        message = "XMLRPC Discovery succeeded, xmrlpc endpoint: ${event.xmlRpcEndpoint}"
                    )
                    cont.resume(Result.success(event.xmlRpcEndpoint))
                }
            }
        }

        WooLog.d(WooLog.T.LOGIN, "Running discovery to fetch XMLRPC endpoint for site $siteUrl")
        dispatcher.register(listener)
        dispatcher.dispatch(AuthenticationActionBuilder.newDiscoverEndpointAction(siteUrl))

        cont.invokeOnCancellation {
            dispatcher.unregister(listener)
        }
    }

    private suspend fun fetchXMLRPCSite(xmlrpcEndpoint: String, username: String, password: String): Result<Unit> =
        suspendCancellableCoroutine { cont ->
            val listener = object : Any() {
                @Suppress("unused")
                @Subscribe(threadMode = MAIN)
                fun onSiteChanged(event: OnSiteChanged) {
                    dispatcher.unregister(this)
                    if (event.isError) {
                        WooLog.w(
                            tag = WooLog.T.LOGIN,
                            message = "XMLRPC site $xmlrpcEndpoint fetch failed, error: ${event.error.message}"
                        )
                        cont.resume(Result.failure(OnChangedException(event.error, event.error.message)))
                    } else {
                        WooLog.d(WooLog.T.LOGIN, "XMLRPC site $xmlrpcEndpoint fetch succeeded")
                        cont.resume(Result.success(Unit))
                    }
                }
            }

            WooLog.d(WooLog.T.LOGIN, "Fetch XMLRPC site, url: $xmlrpcEndpoint")
            dispatcher.register(listener)
            val selfHostedPayload = RefreshSitesXMLRPCPayload(
                username = username,
                password = password,
                url = xmlrpcEndpoint
            )
            dispatcher.dispatch(SiteActionBuilder.newFetchSitesXmlRpcAction(selfHostedPayload))

            cont.invokeOnCancellation {
                dispatcher.unregister(listener)
            }
        }
}
