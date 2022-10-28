package com.woocommerce.android.ui.login.storecreation

import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.login.util.SiteUtils
import javax.inject.Inject

class StoreCreationRepository @Inject constructor(
    private val selectedSite: SelectedSite,
    private val wooCommerceStore: WooCommerceStore,
    private val siteStore: SiteStore
) {
    companion object {
        private const val SITE_CHECK_DELAY = 5000L
    }

    fun selectSite(site: SiteModel) {
        selectedSite.set(site)
    }

    suspend fun fetchSitesAfterCreation(): Result<List<SiteModel>> {
        val result = withContext(Dispatchers.Default) {
            delay(SITE_CHECK_DELAY)
            wooCommerceStore.fetchWooCommerceSites()
        }
        return if (result.isError) Result.failure(Exception(result.error.message))
        else Result.success(result.model ?: emptyList())
    }

    fun getSiteBySiteUrl(url: String) = SiteUtils.getSiteByMatchingUrl(siteStore, url)
        .takeIf {
            // Take only sites returned from the WPCom /me/sites response
            it?.origin == SiteModel.ORIGIN_WPCOM_REST
        }
}
