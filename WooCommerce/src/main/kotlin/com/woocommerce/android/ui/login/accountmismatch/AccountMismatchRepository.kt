package com.woocommerce.android.ui.login.accountmismatch

import com.woocommerce.android.OnChangedException
import com.woocommerce.android.ui.login.WPApiSiteRepository
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.jetpack.JetpackUser
import org.wordpress.android.fluxc.store.JetpackStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.login.util.SiteUtils
import javax.inject.Inject

class AccountMismatchRepository @Inject constructor(
    private val jetpackStore: JetpackStore,
    private val siteStore: SiteStore,
    private val wpApiSiteRepository: WPApiSiteRepository,
    private val dispatcher: Dispatcher
) {
    suspend fun getSiteByUrl(url: String): SiteModel? = withContext(Dispatchers.IO) {
        SiteUtils.getSiteByMatchingUrl(siteStore, url)
    }

    fun removeSiteFromDB(site: SiteModel) {
        dispatcher.dispatch(SiteActionBuilder.newRemoveSiteAction(site))
    }

    suspend fun fetchJetpackConnectionUrl(site: SiteModel): Result<String> {
        WooLog.d(WooLog.T.LOGIN, "Fetching Jetpack Connection URL")
        val result = jetpackStore.fetchJetpackConnectionUrl(
            site,
            useApplicationPasswords = false,
            autoRegisterSiteIfNeeded = true
        )
        return when {
            result.isError -> {
                WooLog.w(WooLog.T.LOGIN, "Fetching Jetpack Connection URL failed: ${result.error.message}")
                Result.failure(OnChangedException(result.error, result.error.message))
            }

            result.data.isNullOrEmpty() -> {
                WooLog.w(WooLog.T.LOGIN, "Fetching Jetpack Connection URL failed, result empty")
                Result.failure(IllegalStateException("Response Empty"))
            }

            else -> {
                WooLog.d(WooLog.T.LOGIN, "Jetpack connection URL fetched successfully")
                Result.success(result.data!!)
            }
        }
    }

    suspend fun fetchJetpackConnectedEmail(site: SiteModel): Result<String> {
        WooLog.d(WooLog.T.LOGIN, "Fetching email of Jetpack User")

        return fetchJetpackUser(site)
            .onFailure {
                WooLog.w(WooLog.T.LOGIN, "Fetching Jetpack User failed error: ${it.message}")
            }.mapCatching {
                val wpcomEmail = it?.wpcomEmail
                if (wpcomEmail.isNullOrEmpty()) {
                    WooLog.w(WooLog.T.LOGIN, "Cannot find Jetpack Email in response")
                    @Suppress("TooGenericExceptionThrown")
                    throw Exception("Email missing from response")
                } else {
                    WooLog.d(WooLog.T.LOGIN, "Jetpack User fetched successfully")
                    wpcomEmail
                }
            }
    }

    suspend fun checkJetpackConnection(
        siteUrl: String,
        username: String,
        password: String
    ): Result<JetpackConnectionStatus> {
        WooLog.d(WooLog.T.LOGIN, "Checking Jetpack Connection status for site $siteUrl")

        val site = wpApiSiteRepository.fetchSite(siteUrl, username, password).getOrElse {
            WooLog.w(WooLog.T.LOGIN, "Site fetch failed, error: ${it.message}")
            return Result.failure(it)
        }

        return fetchJetpackUser(site).onFailure {
            WooLog.w(WooLog.T.LOGIN, "Jetpack User fetch failed, error: ${it.message}")
        }.map {
            if (it?.isConnected != true) {
                WooLog.w(WooLog.T.LOGIN, "Account is not connected to a WPCom account")
                JetpackConnectionStatus.NotConnected
            } else {
                WooLog.d(WooLog.T.LOGIN, "Account is connected to different WPCom account: ${it.wpcomEmail}")
                JetpackConnectionStatus.ConnectedToDifferentAccount(it.wpcomEmail)
            }
        }
    }

    private suspend fun fetchJetpackUser(site: SiteModel): Result<JetpackUser?> {
        return jetpackStore.fetchJetpackConnectionData(site, useApplicationPasswords = false).let {
            if (it.isError) {
                Result.failure(OnChangedException(it.error, it.error.message))
            } else {
                Result.success(it.data?.currentUser)
            }
        }
    }

    /**
     * Represents Jetpack Connection status for the current wp-admin account
     */
    sealed interface JetpackConnectionStatus {
        data object NotConnected : JetpackConnectionStatus
        data class ConnectedToDifferentAccount(val wpcomEmail: String) : JetpackConnectionStatus
    }
}
