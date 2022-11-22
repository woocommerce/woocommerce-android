package com.woocommerce.android.ui.login.jetpack

import com.woocommerce.android.OnChangedException
import com.woocommerce.android.WooException
import com.woocommerce.android.extensions.isNotNullOrEmpty
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.JetpackStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.login.util.SiteUtils
import javax.inject.Inject

class JetpackActivationRepository @Inject constructor(
    private val siteStore: SiteStore,
    private val jetpackStore: JetpackStore,
    private val wooCommerceStore: WooCommerceStore
) {
    suspend fun getSiteByUrl(url: String): SiteModel? = withContext(Dispatchers.IO) {
        SiteUtils.getSiteByMatchingUrl(siteStore, url)
            ?.takeIf {
                val hasCredentials = it.username.isNotNullOrEmpty() && it.password.isNotNullOrEmpty()
                if (!hasCredentials) {
                    WooLog.w(WooLog.T.LOGIN, "The found site for jetpack activation doesn't have credentials")
                }
                hasCredentials
            }
    }

    suspend fun fetchJetpackConnectionUrl(site: SiteModel): Result<String> {
        WooLog.d(WooLog.T.LOGIN, "Fetching Jetpack Connection URL")
        val result = jetpackStore.fetchJetpackConnectionUrl(site, autoRegisterSiteIfNeeded = true)
        return when {
            result.isError -> {
                WooLog.w(WooLog.T.LOGIN, "Fetching Jetpack Connection URL failed: ${result.error.message}")
                Result.failure(OnChangedException(result.error, result.error.message))
            }
            else -> {
                WooLog.d(WooLog.T.LOGIN, "Jetpack connection URL fetched successfully")
                Result.success(result.url)
            }
        }
    }

    suspend fun fetchJetpackConnectedEmail(site: SiteModel): Result<String> {
        WooLog.d(WooLog.T.LOGIN, "Fetching email of Jetpack User")
        val result = jetpackStore.fetchJetpackUser(site)
        return when {
            result.isError -> {
                WooLog.w(WooLog.T.LOGIN, "Fetching Jetpack User failed error: $result.error.message")
                Result.failure(OnChangedException(result.error, result.error.message))
            }
            result.user?.wpcomEmail.isNullOrEmpty() -> {
                WooLog.w(WooLog.T.LOGIN, "Cannot find Jetpack Email in response")
                Result.failure(Exception("Email missing from response"))
            }
            else -> {
                WooLog.d(WooLog.T.LOGIN, "Jetpack User fetched successfully")
                Result.success(result.user!!.wpcomEmail)
            }
        }
    }

    suspend fun checkSiteConnection(siteUrl: String): Result<Unit> {
        WooLog.d(WooLog.T.LOGIN, "Jetpack Activation: Fetch WooCommerce Stores to confirm Jetpack Connection")
        wooCommerceStore.fetchWooCommerceSites().let { result ->
            if (result.isError) {
                WooLog.d(
                    WooLog.T.LOGIN,
                    "Jetpack Activation: Fetching WooCommerce Stores failed: ${result.error.message}"
                )
                return Result.failure(WooException(result.error))
            }

            val site = withContext(Dispatchers.IO) {
                SiteUtils.getSiteByMatchingUrl(siteStore, siteUrl)
            }?.takeIf { it.siteId != 0L }

            return if (site == null) {
                WooLog.d(WooLog.T.LOGIN, "Jetpack Activation: Site $siteUrl is missing from account sites")
                Result.failure(IllegalStateException("Site missing"))
            } else Result.success(Unit)
        }
    }
}
