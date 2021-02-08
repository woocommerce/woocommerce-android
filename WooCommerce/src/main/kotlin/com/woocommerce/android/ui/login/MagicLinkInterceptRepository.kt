package com.woocommerce.android.ui.login

import com.woocommerce.android.AppConstants
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.LOGIN_MAGIC_LINK_FETCH_ACCOUNT_SETTINGS_FAILED
import com.woocommerce.android.model.RequestResult
import com.woocommerce.android.model.RequestResult.ERROR
import com.woocommerce.android.model.RequestResult.NO_ACTION_NEEDED
import com.woocommerce.android.model.RequestResult.RETRY
import com.woocommerce.android.model.RequestResult.SUCCESS
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
import org.wordpress.android.fluxc.store.AccountStore.AccountErrorType
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged
import org.wordpress.android.fluxc.store.AccountStore.OnAuthenticationChanged
import org.wordpress.android.fluxc.store.AccountStore.UpdateTokenPayload
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged
import org.wordpress.android.login.LoginAnalyticsListener
import javax.inject.Inject
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

class MagicLinkInterceptRepository @Inject constructor(
    private val dispatcher: Dispatcher,
    private val accountStore: AccountStore,
    private val loginAnalyticsListener: LoginAnalyticsListener
) {
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
                    // Track magic link login success
                    loginAnalyticsListener.trackLoginMagicLinkSucceeded()

                    // fetch account details
                    fetchAccountInfo()
                } else ERROR
            }
        } else NO_ACTION_NEEDED
    }

    /**
     * Fetch the account, the account settings and site list from the API.
     *
     * Wait for all three requests to complete. If the fetch results in error for even one of the APIs
     * return [RequestResult.RETRY].
     *
     * @return the result of the fetch as a [RequestResult]
     */
    suspend fun fetchAccountInfo(): RequestResult {
        return coroutineScope {
            var fetchedAccount = false
            var fetchedAccountSettings = false
            var fetchedSites = false

            val fetchAccount = async {
                fetchedAccount = fetchAccount()
            }
            val fetchAccountSettings = async {
                fetchedAccountSettings = fetchAccountSettings()
            }
            val fetchSites = async {
                fetchedSites = fetchSites()
            }
            fetchAccount.await()
            fetchAccountSettings.await()
            fetchSites.await()

            if (fetchedAccount && fetchedAccountSettings && fetchedSites) SUCCESS else RETRY
        }
    }

    /**
     * Fires the request to update the magic link token to the FluxC cache
     *
     * @return the result of the action as a [Boolean]
     */
    private suspend fun storeMagicLinkToken(token: String): Boolean {
        return try {
            suspendCoroutineWithTimeout<Boolean>(AppConstants.REQUEST_TIMEOUT) {
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
            suspendCoroutineWithTimeout<Boolean>(AppConstants.REQUEST_TIMEOUT) {
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
            suspendCoroutineWithTimeout<Boolean>(AppConstants.REQUEST_TIMEOUT) {
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
            suspendCoroutineWithTimeout<Boolean>(AppConstants.REQUEST_TIMEOUT) {
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
            WooLog.e(LOGIN, "onAuthenticationChanged has error: ${event.error?.type} : ${event.error?.message}")
            AnalyticsTracker.track(Stat.LOGIN_MAGIC_LINK_UPDATE_TOKEN_FAILED, mapOf(
                    AnalyticsTracker.KEY_ERROR_CONTEXT to this::class.java.simpleName,
                    AnalyticsTracker.KEY_ERROR_TYPE to event.error?.type?.toString(),
                    AnalyticsTracker.KEY_ERROR_DESC to event.error?.message))

            continuationUpdateToken?.resume(false)
        } else {
            continuationUpdateToken?.resume(true)
        }
        continuationUpdateToken = null
    }

    @Suppress("unused")
    @Subscribe(threadMode = MAIN)
    fun onAccountChanged(event: OnAccountChanged) {
        if (event.isError) {
            WooLog.e(LOGIN, "onAccountChanged has error: ${event.error?.type} : ${event.error?.message}")

            val trackEvent = when {
                event.causeOfChange == AccountAction.FETCH_SETTINGS -> LOGIN_MAGIC_LINK_FETCH_ACCOUNT_SETTINGS_FAILED
                event.error?.type == AccountErrorType.ACCOUNT_FETCH_ERROR -> Stat.LOGIN_MAGIC_LINK_FETCH_ACCOUNT_FAILED
                else -> null
            }
            trackEvent?.let {
                AnalyticsTracker.track(it, mapOf(
                        AnalyticsTracker.KEY_ERROR_CONTEXT to this::class.java.simpleName,
                        AnalyticsTracker.KEY_ERROR_TYPE to event.error?.type?.toString(),
                        AnalyticsTracker.KEY_ERROR_DESC to event.error?.message))
            }
        } else {
            when {
                event.causeOfChange == AccountAction.FETCH_ACCOUNT -> {
                    // The user's account info has been fetched and stored - next, fetch the user's settings
                    AnalyticsTracker.track(Stat.LOGIN_MAGIC_LINK_FETCH_ACCOUNT_SUCCESS)
                    continuationFetchAccount?.resume(!event.isError)
                    continuationFetchAccount = null
                }
                event.causeOfChange == AccountAction.FETCH_SETTINGS -> {
                    // The user's account settings have also been fetched and stored - now we can fetch the user's sites
                    AnalyticsTracker.track(Stat.LOGIN_MAGIC_LINK_FETCH_ACCOUNT_SETTINGS_SUCCESS)
                    continuationFetchAccountSettings?.resume(!event.isError)
                    continuationFetchAccountSettings = null
                }
            }
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = MAIN)
    fun onSiteChanged(event: OnSiteChanged) {
        if (event.isError) {
            AnalyticsTracker.track(Stat.LOGIN_MAGIC_LINK_FETCH_SITES_FAILED, mapOf(
                    AnalyticsTracker.KEY_ERROR_CONTEXT to this::class.java.simpleName,
                    AnalyticsTracker.KEY_ERROR_TYPE to event.error?.type?.toString(),
                    AnalyticsTracker.KEY_ERROR_DESC to event.error?.message))

            continuationFetchSites?.resume(false)
        } else {
            AnalyticsTracker.track(Stat.LOGIN_MAGIC_LINK_FETCH_SITES_SUCCESS)
            continuationFetchSites?.resume(true)
        }
        isHandlingMagicLink = false
        continuationFetchSites = null
    }
}
