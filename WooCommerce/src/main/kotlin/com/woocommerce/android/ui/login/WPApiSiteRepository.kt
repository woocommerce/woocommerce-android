package com.woocommerce.android.ui.login

import com.woocommerce.android.OnChangedException
import com.woocommerce.android.WooException
import com.woocommerce.android.applicationpasswords.ApplicationPasswordsNotifier
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.FetchWPAPISitePayload
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.login.util.SiteUtils
import javax.inject.Inject

class WPApiSiteRepository @Inject constructor(
    private val dispatcher: Dispatcher,
    private val siteStore: SiteStore,
    private val wooCommerceStore: WooCommerceStore,
    private val applicationPasswordsNotifier: ApplicationPasswordsNotifier
) {
    /**
     * Handles authentication to the given [url] using wp-admin credentials.
     * After calling this, and if the authentication is successful, a [SiteModel] matching this site will be persisted
     * in the DB.
     */
    suspend fun login(url: String, username: String, password: String): Result<SiteModel> {
        WooLog.d(WooLog.T.LOGIN, "Authenticating in to site $url using site credentials")

        return siteStore.fetchWPAPISite(
            FetchWPAPISitePayload(
                url = url,
                username = username,
                password = password
            )
        ).let { result ->
            when {
                result.isError -> {
                    WooLog.w(
                        tag = WooLog.T.LOGIN,
                        message = "Fetching site $url failed, error: ${result.error.type}, ${result.error.message}"
                    )

                    Result.failure(OnChangedException(result.error))
                }
                else -> {
                    WooLog.d(WooLog.T.LOGIN, "Site $url fetch succeeded")
                    val site = getSiteByUrl(url)!!
                    Result.success(site)
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun checkWooStatus(site: SiteModel): Result<Boolean> = coroutineScope {
        WooLog.d(WooLog.T.LOGIN, "Fetch site ${site.url} to check if Woo installed")

        val applicationPasswordErrorTask = async {
            applicationPasswordsNotifier.passwordGenerationFailures.first()
        }

        return@coroutineScope wooCommerceStore.fetchWooCommerceSite(site)
            .let {
                when {
                    it.isError -> {
                        // Wait a bit to make sure that if there is any Application Password failure it was processed
                        @Suppress("MagicNumber")
                        withTimeoutOrNull(100L) { applicationPasswordErrorTask.join() }

                        if (applicationPasswordErrorTask.isCompleted) {
                            Result.failure(applicationPasswordErrorTask.getCompleted())
                        } else {
                            // Make sure the task is cancelled
                            applicationPasswordErrorTask.cancel()
                            Result.failure(WooException(it.error))
                        }
                    }
                    else -> {
                        applicationPasswordErrorTask.cancel()
                        Result.success(it.model!!.hasWooCommerce)
                    }
                }
            }
    }

    suspend fun getSiteByUrl(siteUrl: String): SiteModel? = withContext(Dispatchers.IO) {
        SiteUtils.getSiteByMatchingUrl(siteStore, siteUrl)
    }
}
