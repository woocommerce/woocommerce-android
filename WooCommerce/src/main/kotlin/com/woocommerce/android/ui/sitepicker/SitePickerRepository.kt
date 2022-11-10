package com.woocommerce.android.ui.sitepicker

import com.woocommerce.android.WooException
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.dispatchAndAwait
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

class SitePickerRepository @Inject constructor(
    private val siteStore: SiteStore,
    private val dispatcher: Dispatcher,
    private val wooCommerceStore: WooCommerceStore
) {
    suspend fun getSites() = withContext(Dispatchers.IO) { siteStore.sites }
        .filter {
            // Take only sites returned from the WPCom /me/sites response
            it.origin == SiteModel.ORIGIN_WPCOM_REST
        }

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

    // overrideRetryPolicy = true, will set the JetPackTunnel timeout policy to 15s rather than default 30s
    suspend fun verifySiteWooAPIVersion(site: SiteModel) =
        wooCommerceStore.fetchSupportedApiVersion(site, overrideRetryPolicy = true)

    suspend fun fetchSiteInfo(siteAddress: String): Result<ConnectSiteInfoPayload> {
        val action = SiteActionBuilder.newFetchConnectSiteInfoAction(siteAddress)
        val event: OnConnectSiteInfoChecked = dispatcher.dispatchAndAwait(action)

        return if (event.isError) {
            AppLog.e(API, "onFetchedConnectSiteInfo has error: " + event.error.message)
            Result.failure(FetchSiteInfoException(event.error.type, event.error.message))
        } else {
            Result.success(event.info)
        }
    }

    class FetchSiteInfoException(val type: SiteErrorType, message: String?) : Exception(message)
}
