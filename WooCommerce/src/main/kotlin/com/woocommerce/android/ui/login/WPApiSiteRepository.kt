package com.woocommerce.android.ui.login

import com.woocommerce.android.OnChangedException
import com.woocommerce.android.applicationpasswords.ApplicationPasswordsNotifier
import com.woocommerce.android.ui.common.UserEligibilityFetcher
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.FetchWPAPISitePayload
import org.wordpress.android.login.util.SiteUtils
import javax.inject.Inject

class WPApiSiteRepository @Inject constructor(
    private val siteStore: SiteStore,
    private val userEligibilityFetcher: UserEligibilityFetcher,
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
    suspend fun checkIfUserIsEligible(): Result<Boolean> = coroutineScope {
        WooLog.d(WooLog.T.LOGIN, "Fetch user info to check if user is eligible for using the API")

        val applicationPasswordErrorTask = async {
            applicationPasswordsNotifier.passwordGenerationFailures.first()
        }

        return@coroutineScope userEligibilityFetcher.fetchUserInfo()
            .recoverCatching { exception ->
                @Suppress("MagicNumber")
                withTimeoutOrNull(100L) { applicationPasswordErrorTask.join() }

                if (applicationPasswordErrorTask.isCompleted) {
                    throw applicationPasswordErrorTask.getCompleted()
                } else {
                    applicationPasswordErrorTask.cancel()
                    throw exception
                }
            }
            .onSuccess { applicationPasswordErrorTask.cancel() }
            .map { it.isEligible }
    }

    suspend fun getSiteByUrl(siteUrl: String): SiteModel? = withContext(Dispatchers.IO) {
        SiteUtils.getSiteByMatchingUrl(siteStore, siteUrl)
    }
}
