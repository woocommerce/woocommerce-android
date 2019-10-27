package com.woocommerce.android.ui.login

import com.woocommerce.android.ui.reviews.RequestResult
import com.woocommerce.android.ui.reviews.RequestResult.ERROR
import com.woocommerce.android.ui.reviews.RequestResult.NO_ACTION_NEEDED
import com.woocommerce.android.ui.reviews.RequestResult.SUCCESS
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T.LOGIN
import com.woocommerce.android.util.suspendCoroutineWithTimeout
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.AccountAction
import org.wordpress.android.fluxc.generated.AccountActionBuilder
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged
import org.wordpress.android.fluxc.store.AccountStore.OnAuthenticationChanged
import org.wordpress.android.fluxc.store.AccountStore.UpdateTokenPayload
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged
import javax.inject.Inject
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

class MagicLinkInterceptRepository @Inject constructor(
    private val dispatcher: Dispatcher,
    private val accountStore: AccountStore
) {
    companion object {
        private const val ACTION_TIMEOUT = 10L * 1000
    }

    private var isHandlingMagicLink: Boolean = false

    private var continuationUpdateToken: Continuation<Boolean>? = null
    private var continuationFetchAccount: Continuation<Boolean>? = null
    private var continuationFetchAccountSettings: Continuation<Boolean>? = null
    private var continuationFetchSites: Continuation<Boolean>? = null

    init {
        dispatcher.register(this)
    }

    fun onCleanup() {
        dispatcher.unregister(this)
    }

    fun userIsLoggedIn() = accountStore.hasAccessToken()

    /**
     * Update magic link auth token to the FluxC cache and verify if the user is logged in.
     * Only if the user is logged in, fetch the account, the account settings and site list from the API.
     *
     * Wait for all three requests to complete. If the fetch is already in progress
     * return [RequestResult.NO_ACTION_NEEDED].
     *
     * @param [authToken] - the magic link auth token needed to be updated to the local cache
     * @return the result of the fetch as a [RequestResult]
     */
    suspend fun updateMagicLinkAuthToken(authToken: String): RequestResult {
        return if (!isHandlingMagicLink) {
            isHandlingMagicLink = true

            coroutineScope {
                // Update the magic link token to the FluxC cache
                val authTokenUpdatedResult = storeMagicLinkToken(authToken)

                // This means a login via magic link was performed, and the access token was just updated
                // In this case, we need to fetch account details and the site list, and finally notify the view
                // In all other login cases, this logic is handled by the login library
                if (authTokenUpdatedResult && userIsLoggedIn()) {
                    var fetchedAccountSettingsAndSites = false

                    val fetchAccount = async {
                        fetchedAccountSettingsAndSites = fetchAccount()
                    }
                    val fetchAccountSettings = async {
                        fetchedAccountSettingsAndSites = fetchAccountSettings()
                    }
                    val fetchSites = async {
                        fetchedAccountSettingsAndSites = fetchSites()
                    }
                    fetchAccount.await()
                    fetchAccountSettings.await()
                    fetchSites.await()

                    if (fetchedAccountSettingsAndSites) SUCCESS else NO_ACTION_NEEDED
                } else ERROR
            }
        } else NO_ACTION_NEEDED
    }

    /**
     * Fires the request to update the magic link token to the FluxC cache
     *
     * @return the result of the action as a [Boolean]
     */
    private suspend fun storeMagicLinkToken(token: String): Boolean {
        return try {
            suspendCoroutineWithTimeout<Boolean>(ACTION_TIMEOUT) {
                continuationUpdateToken = it

                dispatcher.dispatch(AccountActionBuilder.newUpdateAccessTokenAction(UpdateTokenPayload(token)))
            } ?: false // request timed out
        } catch (e: CancellationException) {
            WooLog.e(LOGIN, "Exception encountered while updating magic link auth token", e)
            false
        }
    }

    /**
     * Fires the request to fetch the account info
     *
     * @return the result of the action as a [Boolean]
     */
    private suspend fun fetchAccount(): Boolean {
        return try {
            suspendCoroutineWithTimeout<Boolean>(ACTION_TIMEOUT) {
                continuationFetchAccount = it

                dispatcher.dispatch(AccountActionBuilder.newFetchAccountAction())
            } ?: false // request timed out
        } catch (e: CancellationException) {
            WooLog.e(LOGIN, "Exception encountered while fetching account info", e)
            false
        }
    }

    /**
     * Fires the request to fetch the account settings
     *
     * @return the result of the action as a [Boolean]
     */
    private suspend fun fetchAccountSettings(): Boolean {
        return try {
            suspendCoroutineWithTimeout<Boolean>(ACTION_TIMEOUT) {
                continuationFetchAccountSettings = it

                dispatcher.dispatch(AccountActionBuilder.newFetchSettingsAction())
            } ?: false // request timed out
        } catch (e: CancellationException) {
            WooLog.e(LOGIN, "Exception encountered while fetching account settings", e)
            false
        }
    }

    /**
     * Fires the request to fetch all sites fot the account
     *
     * @return the result of the action as a [Boolean]
     */
    private suspend fun fetchSites(): Boolean {
        return try {
            suspendCoroutineWithTimeout<Boolean>(ACTION_TIMEOUT) {
                continuationFetchSites = it

                dispatcher.dispatch(SiteActionBuilder.newFetchSitesAction())
            } ?: false // request timed out
        } catch (e: CancellationException) {
            WooLog.e(LOGIN, "Exception encountered while fetching sites", e)
            false
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = MAIN)
    fun onAuthenticationChanged(event: OnAuthenticationChanged) {
        if (event.isError) {
            continuationUpdateToken?.resume(false)
        } else {
            continuationUpdateToken?.resume(true)
        }
        continuationUpdateToken = null
    }

    @Suppress("unused")
    @Subscribe(threadMode = MAIN)
    fun onAccountChanged(event: OnAccountChanged) {
        when {
            event.causeOfChange == AccountAction.FETCH_ACCOUNT -> {
                // The user's account info has been fetched and stored - next, fetch the user's settings
                continuationFetchAccount?.resume(!event.isError)
                continuationFetchAccount = null
            }
            event.causeOfChange == AccountAction.FETCH_SETTINGS -> {
                // The user's account settings have also been fetched and stored - now we can fetch the user's sites
                continuationFetchAccountSettings?.resume(!event.isError)
                continuationFetchAccountSettings = null
            }
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = MAIN)
    fun onSiteChanged(event: OnSiteChanged) {
        if (event.isError) {
            continuationFetchSites?.resume(false)
        } else {
            continuationFetchSites?.resume(true)
        }
        isHandlingMagicLink = false
        continuationFetchSites = null
    }
}
