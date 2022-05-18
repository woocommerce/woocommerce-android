package com.woocommerce.android.ui.sitepicker

import com.woocommerce.android.AppConstants
import com.woocommerce.android.util.ContinuationWrapper
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T.SITE_PICKER
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.AccountAction
import org.wordpress.android.fluxc.generated.AccountActionBuilder
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.login.util.SiteUtils
import javax.inject.Inject

class SitePickerRepository @Inject constructor(
    private val siteStore: SiteStore,
    private val dispatcher: Dispatcher,
    private val accountStore: AccountStore,
    private val wooCommerceStore: WooCommerceStore
) {
    private var continuationLogout = ContinuationWrapper<Boolean>(SITE_PICKER)

    init {
        dispatcher.register(this)
    }

    fun onCleanup() {
        dispatcher.unregister(this)
    }

    fun getWooCommerceSites() = wooCommerceStore.getWooCommerceSites()

    fun getSiteBySiteUrl(url: String) = SiteUtils.getSiteByMatchingUrl(siteStore, url)

    fun getUserAccount() = accountStore.account

    fun isUserLoggedIn() = accountStore.hasAccessToken()

    suspend fun fetchWooCommerceSites() = wooCommerceStore.fetchWooCommerceSites()

    suspend fun fetchWooCommerceSite(siteModel: SiteModel) = wooCommerceStore.fetchWooCommerceSite(siteModel)

    suspend fun fetchSiteSettings(site: SiteModel) = wooCommerceStore.fetchSiteGeneralSettings(site)

    suspend fun fetchSiteProductSettings(site: SiteModel) = wooCommerceStore.fetchSiteProductSettings(site)

    suspend fun verifySiteWooAPIVersion(site: SiteModel) = wooCommerceStore.fetchSupportedApiVersion(site)

    suspend fun logout(): Boolean? {
        val result = continuationLogout.callAndWaitUntilTimeout(AppConstants.REQUEST_TIMEOUT) {
            dispatcher.dispatch(AccountActionBuilder.newSignOutAction())
            dispatcher.dispatch(SiteActionBuilder.newRemoveWpcomAndJetpackSitesAction())
        }
        return when (result) {
            is ContinuationWrapper.ContinuationResult.Cancellation -> null
            is ContinuationWrapper.ContinuationResult.Success -> result.value
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onAccountChanged(event: OnAccountChanged) {
        if (event.causeOfChange == AccountAction.SIGN_OUT) {
            if (event.isError) {
                WooLog.e(
                    SITE_PICKER,
                    "Account error [type = ${event.causeOfChange}] : " +
                        "${event.error.type} > ${event.error.message}"
                )
                continuationLogout.continueWith(false)
            } else if (!isUserLoggedIn()) {
                continuationLogout.continueWith(true)
            }
        }
    }
}
