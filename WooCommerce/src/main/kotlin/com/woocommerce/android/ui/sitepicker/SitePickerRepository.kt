package com.woocommerce.android.ui.sitepicker

import com.woocommerce.android.WooException
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
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
import kotlin.coroutines.resume

class SitePickerRepository @Inject constructor(
    private val siteStore: SiteStore,
    private val dispatcher: Dispatcher,
    private val accountStore: AccountStore,
    private val wooCommerceStore: WooCommerceStore
) {
    suspend fun getSites() = withContext(Dispatchers.IO) { siteStore.sites }

    fun getSiteBySiteUrl(url: String) = SiteUtils.getSiteByMatchingUrl(siteStore, url)

    fun getUserAccount() = accountStore.account

    fun isUserLoggedIn() = accountStore.hasAccessToken()

    suspend fun fetchWooCommerceSites() = wooCommerceStore.fetchWooCommerceSites()

    suspend fun fetchWooCommerceSite(siteModel: SiteModel): Result<SiteModel> {
        return wooCommerceStore.fetchWooCommerceSite(siteModel).let {
            when {
                it.isError -> {
                    WooLog.e(
                        WooLog.T.SITE_PICKER,
                        "Fetching site ${siteModel.siteId} failed, Error: ${it.error.type} ${it.error.message}"
                    )
                    Result.failure(WooException(it.error))
                }
                else -> Result.success(it.model!!)
            }
        }
    }

    suspend fun fetchSiteSettings(site: SiteModel) = wooCommerceStore.fetchSiteGeneralSettings(site)

    suspend fun fetchSiteProductSettings(site: SiteModel) = wooCommerceStore.fetchSiteProductSettings(site)

    suspend fun verifySiteWooAPIVersion(site: SiteModel) = wooCommerceStore.fetchSupportedApiVersion(site)

    suspend fun logout(): Boolean = suspendCancellableCoroutine { continuation ->
        val listener = object : Any() {
            @Suppress("unused")
            @Subscribe(threadMode = ThreadMode.MAIN)
            fun onAccountChanged(event: OnAccountChanged) {
                if (event.causeOfChange == AccountAction.SIGN_OUT) {
                    dispatcher.unregister(this)
                    if (!continuation.isActive) return

                    if (event.isError) {
                        WooLog.e(
                            WooLog.T.SITE_PICKER,
                            "Account error [type = ${event.causeOfChange}] : " +
                                "${event.error.type} > ${event.error.message}"
                        )
                        continuation.resume(false)
                    } else if (!isUserLoggedIn()) {
                        continuation.resume(true)
                    }
                }
            }
        }
        dispatcher.dispatch(AccountActionBuilder.newSignOutAction())
        dispatcher.dispatch(SiteActionBuilder.newRemoveWpcomAndJetpackSitesAction())

        continuation.invokeOnCancellation {
            dispatcher.unregister(listener)
        }
    }
}
