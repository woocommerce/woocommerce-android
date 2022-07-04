package com.woocommerce.android.ui.main

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.extensions.NotificationReceivedEvent
import com.woocommerce.android.network.ConnectionChangeReceiver
import com.woocommerce.android.network.ConnectionChangeReceiver.ConnectionChangeEvent
import com.woocommerce.android.push.NotificationChannelType.NEW_ORDER
import com.woocommerce.android.tools.ProductImageMap
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.tools.SelectedSite.SelectedSiteChangedEvent
import com.woocommerce.android.ui.cardreader.ClearCardReaderDataAction
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.AccountAction
import org.wordpress.android.fluxc.action.WCOrderAction.FETCH_ORDERS
import org.wordpress.android.fluxc.action.WCOrderAction.FETCH_ORDERS_COUNT
import org.wordpress.android.fluxc.action.WCOrderAction.UPDATE_ORDER_STATUS
import org.wordpress.android.fluxc.generated.AccountActionBuilder
import org.wordpress.android.fluxc.generated.WCOrderActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus.PROCESSING
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged
import org.wordpress.android.fluxc.store.AccountStore.OnAuthenticationChanged
import org.wordpress.android.fluxc.store.AccountStore.UpdateTokenPayload
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderStatusOptionsPayload
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrdersCountPayload
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderChanged
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

class MainPresenter @Inject constructor(
    private val dispatcher: Dispatcher,
    private val accountStore: AccountStore,
    private val wooCommerceStore: WooCommerceStore,
    private val selectedSite: SelectedSite,
    private val productImageMap: ProductImageMap,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val wcOrderStore: WCOrderStore,
    private val clearCardReaderDataAction: ClearCardReaderDataAction
) : MainContract.Presenter {
    private var mainView: MainContract.View? = null

    private var isHandlingMagicLink: Boolean = false
    private var pendingUnfilledOrderCountCheck: Boolean = false

    override fun takeView(view: MainContract.View) {
        mainView = view
        dispatcher.register(this)
        ConnectionChangeReceiver.getEventBus().register(this)

        coroutineScope.launch {
            selectedSite.getIfExists()?.let { siteModel ->
                wcOrderStore.observeOrderCountForSite(
                    siteModel, listOf(PROCESSING.value)
                ).collect { count ->
                    AnalyticsTracker.track(
                        AnalyticsEvent.UNFULFILLED_ORDERS_LOADED,
                        mapOf(AnalyticsTracker.KEY_HAS_UNFULFILLED_ORDERS to count)
                    )

                    if (count > 0) {
                        mainView?.showOrderBadge(count)
                    } else {
                        mainView?.hideOrderBadge()
                    }
                }
            }
        }
    }

    override fun dropView() {
        mainView = null
        dispatcher.unregister(this)
        ConnectionChangeReceiver.getEventBus().unregister(this)
    }

    override fun userIsLoggedIn(): Boolean {
        return accountStore.hasAccessToken()
    }

    override fun storeMagicLinkToken(token: String) {
        isHandlingMagicLink = true
        // Save Token to the AccountStore. This will trigger an OnAuthenticationChanged.
        dispatcher.dispatch(AccountActionBuilder.newUpdateAccessTokenAction(UpdateTokenPayload(token)))
    }

    override fun hasMultipleStores() = wooCommerceStore.getWooCommerceSites().size > 0

    override fun selectedSiteChanged(site: SiteModel) {
        productImageMap.reset()

        // Fetch a fresh list of order status options
        dispatcher.dispatch(
            WCOrderActionBuilder
                .newFetchOrderStatusOptionsAction(FetchOrderStatusOptionsPayload(site))
        )
        coroutineScope.launch { clearCardReaderDataAction() }
    }

    override fun fetchUnfilledOrderCount() {
        if (selectedSite.exists()) {
            pendingUnfilledOrderCountCheck = false
            val payload = FetchOrdersCountPayload(selectedSite.get(), PROCESSING.value)
            dispatcher.dispatch(WCOrderActionBuilder.newFetchOrdersCountAction(payload))
        } else {
            pendingUnfilledOrderCountCheck = true
        }
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
            // TODO Handle AuthenticationErrorType.INVALID_TOKEN
            isHandlingMagicLink = false
            return
        }

        if (userIsLoggedIn()) {
            // This means a login via magic link was performed, and the access token was just updated
            // In this case, we need to fetch account details and the site list, and finally notify the view
            // In all other login cases, this logic is handled by the login library
            mainView?.notifyTokenUpdated()
            dispatcher.dispatch(AccountActionBuilder.newFetchAccountAction())
        } else {
            mainView?.showLoginScreen()
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
    fun onOrderChanged(event: OnOrderChanged) {
        when (event.causeOfChange) {
            FETCH_ORDERS_COUNT -> {
                if (event.isError) {
                    WooLog.e(
                        WooLog.T.ORDERS,
                        "Error fetching a count of orders waiting to be fulfilled: ${event.error.message}"
                    )
                    mainView?.hideOrderBadge()
                    return
                }
            }
            FETCH_ORDERS, UPDATE_ORDER_STATUS -> {
                // we just fetched the order list or an order's status changed, so re-check the unfilled orders count
                WooLog.d(WooLog.T.ORDERS, "Order status changed, re-checking unfilled orders count")
                fetchUnfilledOrderCount()
            }
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: ConnectionChangeEvent) {
        mainView?.updateOfflineStatusBar(event.isConnected)
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: NotificationReceivedEvent) {
        // a new order notification came in so update the unfilled order count
        if (event.channel == NEW_ORDER) {
            fetchUnfilledOrderCount()
        }
    }

    fun onEventMainThread(event: SelectedSiteChangedEvent) {
        if (pendingUnfilledOrderCountCheck) {
            fetchUnfilledOrderCount()
        }
    }
}
