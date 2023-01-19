package com.woocommerce.android.ui.login

import com.woocommerce.android.OnChangedException
import com.woocommerce.android.WooException
import com.woocommerce.android.applicationpasswords.ApplicationPasswordsNotifier
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.awaitEvent
import com.woocommerce.android.util.dispatchAndAwait
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.AuthenticationActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore.OnAuthenticationChanged
import org.wordpress.android.fluxc.store.AccountStore.OnDiscoveryResponse
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.RefreshSitesXMLRPCPayload
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.login.util.SiteUtils
import javax.inject.Inject

class WPApiSiteRepository @Inject constructor(
    private val dispatcher: Dispatcher,
    private val siteStore: SiteStore,
    private val wooCommerceStore: WooCommerceStore,
    private val applicationPasswordsNotifier: ApplicationPasswordsNotifier
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

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun checkWooStatus(site: SiteModel): Result<Boolean> = coroutineScope {
        WooLog.d(WooLog.T.LOGIN, "Fetch site ${site.url} to check if Woo installed")

        val applicationPasswordErrorTask = async {
            applicationPasswordsNotifier.passwordGenerationFailures.first()
        }

        return@coroutineScope wooCommerceStore.fetchWooCommerceSite(site)
            .let {
                when {
                    it.isError -> {
                        // Wait a bit to make sure that if there is any Application Password failure it was processed
                        @Suppress("MagicNumber")
                        withTimeoutOrNull(100L) { applicationPasswordErrorTask.join() }

                        if (applicationPasswordErrorTask.isCompleted) {
                            Result.failure(applicationPasswordErrorTask.getCompleted())
                        } else {
                            // Make sure the task is cancelled
                            applicationPasswordErrorTask.cancel()
                            Result.failure(WooException(it.error))
                        }
                    }
                    else -> {
                        applicationPasswordErrorTask.cancel()
                        Result.success(it.model!!.hasWooCommerce)
                    }
                }
            }
    }

    suspend fun getSiteByUrl(siteUrl: String): SiteModel? = withContext(Dispatchers.IO) {
        SiteUtils.getXMLRPCSiteByUrl(siteStore, siteUrl)
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

        val fetchEvent = siteTask.await()

        val event = if (!fetchEvent.isError) {
            // In case of success, continue directly
            fetchEvent
        } else {
            // If there is an error, prefer passing the authentication error instead of the fetch error
            // This allows having a better error message in the UI
            val authenticationError = if (fetchEvent.isError) {
                @Suppress("MagicNumber")
                withTimeoutOrNull(100) {
                    authenticationTask.await()
                }
            } else null

            authenticationError ?: fetchEvent
        }

        // Make sure to cancel the task if no event was sent
        if (authenticationTask.isActive) authenticationTask.cancel()

        return@coroutineScope when {
            event.isError -> Result.failure(OnChangedException(event.error))
            else -> {
                WooLog.d(WooLog.T.LOGIN, "XMLRPC site $siteUrl fetch succeeded")
                val site = getSiteByUrl(siteUrl)!!
                return@coroutineScope Result.success(site)
            }
        }
    }
}
