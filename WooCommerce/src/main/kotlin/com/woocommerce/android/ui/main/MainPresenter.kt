@file:Suppress("DEPRECATION")

package com.woocommerce.android.ui.main

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.network.ConnectionChangeReceiver
import com.woocommerce.android.network.ConnectionChangeReceiver.ConnectionChangeEvent
import com.woocommerce.android.tools.ProductImageMap
import com.woocommerce.android.tools.SelectedSite.SelectedSiteChangedEvent
import com.woocommerce.android.ui.login.AccountRepository
import com.woocommerce.android.ui.payments.cardreader.ClearCardReaderDataAction
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.AccountAction
import org.wordpress.android.fluxc.generated.AccountActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore.AuthenticationErrorType
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged
import org.wordpress.android.fluxc.store.AccountStore.OnAuthenticationChanged
import org.wordpress.android.fluxc.store.AccountStore.UpdateTokenPayload
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

class MainPresenter @Inject constructor(
    private val dispatcher: Dispatcher,
    private val wooCommerceStore: WooCommerceStore,
    private val productImageMap: ProductImageMap,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val clearCardReaderDataAction: ClearCardReaderDataAction,
    private val accountRepository: AccountRepository,
    private val tracks: AnalyticsTrackerWrapper,
    private val observeProcessingOrdersCount: ObserveProcessingOrdersCount
) : MainContract.Presenter {
    private var mainView: MainContract.View? = null

    private var isHandlingMagicLink: Boolean = false
    private val userLogoutMutex = Mutex()
    private var isUserLoggingOut = false

    override fun takeView(view: MainContract.View) {
        mainView = view
        dispatcher.register(this)
        ConnectionChangeReceiver.getEventBus().register(this)

        coroutineScope.launch {
            observeProcessingOrdersCount.invoke().collect { count ->
                if (count != null && count > 0) {
                    mainView?.showOrderBadge(count)
                } else {
                    mainView?.hideOrderBadge()
                }
            }
        }
    }

    override fun dropView() {
        mainView = null
        dispatcher.unregister(this)
        ConnectionChangeReceiver.getEventBus().unregister(this)
    }

    override fun userIsLoggedIn(): Boolean = accountRepository.isUserLoggedIn()

    override fun storeMagicLinkToken(token: String) {
        isHandlingMagicLink = true
        // Save Token to the AccountStore. This will trigger an OnAuthenticationChanged.
        dispatcher.dispatch(AccountActionBuilder.newUpdateAccessTokenAction(UpdateTokenPayload(token)))
    }

    override fun hasMultipleStores() = wooCommerceStore.getWooCommerceSites().size > 0

    override fun selectedSiteChanged(site: SiteModel) {
        productImageMap.reset()

        coroutineScope.launch { clearCardReaderDataAction() }

        updateStatsWidgets()
    }

    override fun fetchSitesAfterDowngrade() {
        mainView?.showProgressDialog(R.string.loading_stores)
        coroutineScope.launch {
            wooCommerceStore.fetchWooCommerceSites()
            mainView?.hideProgressDialog()
            mainView?.updateSelectedSite()
        }
    }

    override fun isUserEligible() = appPrefsWrapper.isUserEligible()

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onAuthenticationChanged(event: OnAuthenticationChanged) {
        if (event.isError) {
            when (event.error.type) {
                AuthenticationErrorType.INVALID_TOKEN -> {
                    coroutineScope.launch {
                        userLogoutMutex.withLock {
                            if (!isUserLoggingOut) {
                                isUserLoggingOut = true
                                accountRepository.logout()
                                mainView?.restart()
                            }
                        }
                    }
                }
                else -> {
                    isHandlingMagicLink = false
                    return
                }
            }
        }

        if (userIsLoggedIn()) {
            // This means a login via magic link was performed, and the access token was just updated
            // In this case, we need to fetch account details and the site list, and finally notify the view
            // In all other login cases, this logic is handled by the login library
            mainView?.notifyTokenUpdated()
            dispatcher.dispatch(AccountActionBuilder.newFetchAccountAction())
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onAccountChanged(event: OnAccountChanged) {
        if (event.isError) {
            // TODO: Notify the user of the problem
            isHandlingMagicLink = false
            return
        }

        if (isHandlingMagicLink) {
            if (event.causeOfChange == AccountAction.FETCH_ACCOUNT) {
                // The user's account info has been fetched and stored - next, fetch the user's settings
                dispatcher.dispatch(AccountActionBuilder.newFetchSettingsAction())
            } else if (event.causeOfChange == AccountAction.FETCH_SETTINGS) {
                // The user's account settings have also been fetched and stored - now we can fetch the user's sites
                coroutineScope.launch {
                    val result = wooCommerceStore.fetchWooCommerceSites()
                    if (result.isError) {
                        // TODO: Notify the user of the problem
                    } else {
                        // Magic link login is now complete - notify the activity to set the selected site and proceed with loading UI
                        mainView?.updateSelectedSite()
                        isHandlingMagicLink = false
                    }
                }
            }
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: ConnectionChangeEvent) {
        mainView?.updateOfflineStatusBar(event.isConnected)
    }

    @Suppress("unused", "UNUSED_PARAMETER", "DEPRECATION")
    fun onEventMainThread(event: SelectedSiteChangedEvent) {
        updateStatsWidgets()
    }

    override fun updateStatsWidgets() {
        mainView?.updateStatsWidgets()
    }

    override fun onPlanUpgraded() {
        tracks.track(
            AnalyticsEvent.PLAN_UPGRADE_SUCCESS,
            mapOf(AnalyticsTracker.KEY_SOURCE to AnalyticsTracker.VALUE_BANNER)
        )
    }

    override fun onPlanUpgradeDismissed() {
        tracks.track(
            AnalyticsEvent.PLAN_UPGRADE_ABANDONED,
            mapOf(AnalyticsTracker.KEY_SOURCE to AnalyticsTracker.VALUE_BANNER)
        )
    }
}
