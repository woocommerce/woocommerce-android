package com.woocommerce.android.ui.login.jetpack

import com.woocommerce.android.OnChangedException
import com.woocommerce.android.WooException
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.JetpackStore
import org.wordpress.android.fluxc.store.JetpackStore.JetpackConnectionUrlError
import org.wordpress.android.fluxc.store.JetpackStore.JetpackUserError
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.login.util.SiteUtils
import org.wordpress.android.util.UrlUtils
import javax.inject.Inject

class JetpackActivationRepository @Inject constructor(
    private val dispatcher: Dispatcher,
    private val siteStore: SiteStore,
    private val jetpackStore: JetpackStore,
    private val wooCommerceStore: WooCommerceStore,
    private val selectedSite: SelectedSite,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) {
    companion object {
        private const val DEFAULT_MAX_RETRY = 2
        private const val DELAY_BEFORE_RETRY = 500L
    }

    suspend fun getSiteByUrl(url: String): SiteModel? = withContext(Dispatchers.IO) {
        SiteUtils.getSiteByMatchingUrl(siteStore, url)
    }

    suspend fun fetchJetpackConnectionUrl(
        site: SiteModel,
        useApplicationPasswords: Boolean = false
    ): Result<String> = runWithRetry {
        WooLog.d(WooLog.T.LOGIN, "Fetching Jetpack Connection URL")
        val result = jetpackStore.fetchJetpackConnectionUrl(
            site,
            autoRegisterSiteIfNeeded = true,
            useApplicationPasswords = useApplicationPasswords
        )
        return@runWithRetry when {
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

    suspend fun fetchJetpackConnectedEmail(site: SiteModel): Result<String> = runWithRetry {
        WooLog.d(WooLog.T.LOGIN, "Fetching email of Jetpack User")
        val result = jetpackStore.fetchJetpackUser(site)
        return@runWithRetry when {
            result.isError -> {
                WooLog.w(WooLog.T.LOGIN, "Fetching Jetpack User failed error: $result.error.message")
                Result.failure(OnChangedException(result.error, result.error.message))
            }

            result.user?.wpcomEmail.isNullOrEmpty() -> {
                analyticsTrackerWrapper.track(
                    stat = AnalyticsEvent.LOGIN_JETPACK_SETUP_CANNOT_FIND_WPCOM_USER
                )
                WooLog.w(WooLog.T.LOGIN, "Cannot find Jetpack Email in response")
                Result.failure(JetpackMissingConnectionEmailException)
            }

            else -> {
                WooLog.d(WooLog.T.LOGIN, "Jetpack User fetched successfully")
                Result.success(result.user!!.wpcomEmail)
            }
        }
    }

    suspend fun fetchJetpackSite(siteUrl: String): Result<SiteModel> = runWithRetry {
        WooLog.d(WooLog.T.LOGIN, "Jetpack Activation: Fetch WooCommerce Stores to confirm Jetpack Connection")
        wooCommerceStore.fetchWooCommerceSites().let { result ->
            if (result.isError) {
                WooLog.d(
                    WooLog.T.LOGIN,
                    "Jetpack Activation: Fetching WooCommerce Stores failed: ${result.error.message}"
                )
                return@runWithRetry Result.failure(WooException(result.error))
            }

            val site = getJetpackSiteByUrl(siteUrl)

            return@runWithRetry if (site == null) {
                WooLog.d(WooLog.T.LOGIN, "Jetpack Activation: Site $siteUrl is missing from account sites")
                Result.failure(IllegalStateException("Site missing"))
            } else {
                if (!site.hasWooCommerce) {
                    // If the site doesn't have WooCommerce, let's do one additional fetch using `fetchSite`,
                    // this function will make sure to fetch data from the remote site, which might result in more
                    // accurate result
                    siteStore.fetchSite(site)
                }
                Result.success(site)
            }
        }
    }

    suspend fun getJetpackSiteByUrl(siteUrl: String): SiteModel? {
        val baseUrl = UrlUtils.removeScheme(siteUrl).trim('/')

        return withContext(Dispatchers.IO) {
            siteStore.getSitesAccessedViaWPComRestByNameOrUrlMatching(baseUrl)
        }.firstOrNull()
    }

    fun setSelectedSiteAndCleanOldSites(jetpackSite: SiteModel) {
        val baseUrl = UrlUtils.removeScheme(jetpackSite.url).trim('/')

        // Remove all previous entries that don't use WPCom REST API
        siteStore.getSitesByNameOrUrlMatching(baseUrl).forEach {
            if (it.origin != SiteModel.ORIGIN_WPCOM_REST) {
                dispatcher.dispatch(SiteActionBuilder.newRemoveSiteAction(it))
            } else {
                selectedSite.set(it)
            }
        }
    }

    @Suppress("ReturnCount", "MagicNumber")
    private suspend fun <T> runWithRetry(
        maxAttempts: Int = DEFAULT_MAX_RETRY,
        block: suspend () -> Result<T>
    ): Result<T> {
        fun Int?.is4xx() = this != null && this >= 400 && this <= 499

        var attempts = 0
        var lastError: Throwable? = null
        while (attempts < maxAttempts) {
            block().fold(
                onSuccess = {
                    return Result.success(it)
                },
                onFailure = {
                    if (it is OnChangedException) {
                        val errorCode = when (it.error) {
                            is JetpackConnectionUrlError -> it.error.errorCode
                            is JetpackUserError -> it.error.errorCode
                            else -> null
                        }
                        // Skip retrying on 4xx errors
                        if (errorCode.is4xx()) return Result.failure(it)
                    }
                    attempts++
                    if (attempts < maxAttempts) {
                        delay(DELAY_BEFORE_RETRY)
                    }
                    lastError = it
                }
            )
        }
        return Result.failure(lastError!!)
    }

    object JetpackMissingConnectionEmailException : RuntimeException("Email missing from response")
}
