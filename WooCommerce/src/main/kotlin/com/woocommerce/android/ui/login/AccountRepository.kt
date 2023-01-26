package com.woocommerce.android.ui.login

import com.woocommerce.android.AppPrefs
import com.woocommerce.android.OnChangedException
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.di.AppCoroutineScope
import com.woocommerce.android.support.ZendeskHelper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T.LOGIN
import com.woocommerce.android.util.dispatchAndAwait
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.AccountActionBuilder
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged
import org.wordpress.android.fluxc.store.SiteStore
import javax.inject.Inject

class AccountRepository @Inject constructor(
    private val accountStore: AccountStore,
    private val siteStore: SiteStore,
    private val selectedSite: SelectedSite,
    private val dispatcher: Dispatcher,
    private val zendeskHelper: ZendeskHelper,
    private val prefs: AppPrefs,
    @AppCoroutineScope private val appCoroutineScope: CoroutineScope
) {

    fun getUserAccount(): AccountModel? = accountStore.account.takeIf { it.userId != 0L }

    suspend fun fetchUserAccount(): Result<Unit> {
        val event: OnAccountChanged = dispatcher.dispatchAndAwait(AccountActionBuilder.newFetchAccountAction())

        return when {
            event.isError -> Result.failure(OnChangedException(event.error))
            else -> Result.success(Unit)
        }
    }

    fun isUserLoggedIn(): Boolean {
        return accountStore.hasAccessToken() ||
            (selectedSite.exists() && selectedSite.get().origin != SiteModel.ORIGIN_WPCOM_REST)
    }

    suspend fun logout(): Boolean {
        return if (accountStore.hasAccessToken()) {
            // WordPress.com account logout
            val event: OnAccountChanged = dispatcher.dispatchAndAwait(AccountActionBuilder.newSignOutAction())
            if (event.isError) {
                WooLog.e(
                    LOGIN,
                    "Account error [type = ${event.causeOfChange}] : " +
                        "${event.error.type} > ${event.error.message}"
                )
                false
            } else {
                dispatcher.dispatch(SiteActionBuilder.newRemoveWpcomAndJetpackSitesAction())
                cleanup()
                true
            }
        } else {
            // Application passwords logout
            appCoroutineScope.launch {
                val result = siteStore.deleteApplicationPassword(selectedSite.get())
                if (result.isError) {
                    WooLog.e(
                        LOGIN,
                        "Error deleting application password: ${result.error.errorCode} > ${result.error.message}"
                    )
                } else {
                    WooLog.i(LOGIN, "Application password deleted")
                }
            }

            selectedSite.reset()
            cleanup()
            true
        }
    }

    private fun cleanup() {
        AnalyticsTracker.track(AnalyticsEvent.ACCOUNT_LOGOUT)

        // Reset analytics
        AnalyticsTracker.flush()
        AnalyticsTracker.clearAllData()
        zendeskHelper.reset()

        // Wipe user-specific preferences
        prefs.resetUserPreferences()
    }
}
