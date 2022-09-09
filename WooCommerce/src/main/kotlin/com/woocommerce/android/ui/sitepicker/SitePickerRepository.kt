package com.woocommerce.android.ui.sitepicker

import com.woocommerce.android.WooException
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.ConnectSiteInfoPayload
import org.wordpress.android.fluxc.store.SiteStore.OnConnectSiteInfoChecked
import org.wordpress.android.fluxc.store.SiteStore.SiteErrorType
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.login.util.SiteUtils
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.API
import javax.inject.Inject
import kotlin.coroutines.resume

class SitePickerRepository @Inject constructor(
    private val siteStore: SiteStore,
    private val dispatcher: Dispatcher,
    private val wooCommerceStore: WooCommerceStore
) {
    suspend fun getSites() = withContext(Dispatchers.IO) { siteStore.sites }

    fun getSiteBySiteUrl(url: String) = SiteUtils.getSiteByMatchingUrl(siteStore, url)
        .takeIf {
            // Take only sites returned from the WPCom /me/sites response
            it?.origin == SiteModel.ORIGIN_WPCOM_REST
        }

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

    suspend fun verifySiteWooAPIVersion(site: SiteModel, overrideRetryPolicy: Boolean) =
        wooCommerceStore.fetchSupportedApiVersion(site, overrideRetryPolicy)

    suspend fun fetchSiteInfo(siteAddress: String) =
        suspendCancellableCoroutine<Result<ConnectSiteInfoPayload>> { continuation ->
            val listener = object : Any() {
                @Subscribe(threadMode = MAIN)
                fun onFetchedConnectSiteInfo(event: OnConnectSiteInfoChecked) {
                    dispatcher.unregister(this)
                    if (!continuation.isActive) return

                    if (event.isError) {
                        AppLog.e(API, "onFetchedConnectSiteInfo has error: " + event.error.message)
                        continuation.resume(
                            Result.failure(FetchSiteInfoException(event.error.type, event.error.message))
                        )
                    } else {
                        continuation.resume(Result.success(event.info))
                    }
                }
            }
            dispatcher.register(listener)
            dispatcher.dispatch(SiteActionBuilder.newFetchConnectSiteInfoAction(siteAddress))

            continuation.invokeOnCancellation {
                dispatcher.unregister(listener)
            }
        }

    class FetchSiteInfoException(val type: SiteErrorType, message: String?) : Exception(message)
}
