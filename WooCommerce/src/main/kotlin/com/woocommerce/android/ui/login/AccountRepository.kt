package com.woocommerce.android.ui.login

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.OnChangedException
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.datastore.DataStoreQualifier
import com.woocommerce.android.datastore.DataStoreType
import com.woocommerce.android.di.AppCoroutineScope
import com.woocommerce.android.notifications.push.PushNotificationRepository
import com.woocommerce.android.support.zendesk.ZendeskSettings
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.tools.SiteConnectionType
import com.woocommerce.android.ui.sitepicker.sitevisibility.VisibleWooSitesDataStore
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T.LOGIN
import com.woocommerce.android.util.dispatchAndAwait
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.AccountActionBuilder
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.account.CloseAccountStore
import org.wordpress.android.fluxc.store.account.CloseAccountStore.CloseAccountErrorType
import javax.inject.Inject

class AccountRepository @Inject constructor(
    private val accountStore: AccountStore,
    private val siteStore: SiteStore,
    private val closeAccountStore: CloseAccountStore,
    private val selectedSite: SelectedSite,
    private val dispatcher: Dispatcher,
    private val zendeskSettings: ZendeskSettings,
    private val prefs: AppPrefs,
    @AppCoroutineScope private val appCoroutineScope: CoroutineScope,
    private val siteVisibilityDataStore: VisibleWooSitesDataStore,
    private val dispatchers: CoroutineDispatchers,
    private val pushNotificationRepository: PushNotificationRepository,
    @DataStoreQualifier(DataStoreType.WOO_POS) private val posDataStore: DataStore<Preferences>
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
            (selectedSite.connectionType == SiteConnectionType.ApplicationPasswords)
    }

    suspend fun logout(): Boolean {
        if (!isUserLoggedIn()) return true

        pushNotificationRepository.unregisterDeviceFromPushNotifications()

        return if (accountStore.hasAccessToken()) logoutWpComAccount() else logoutApplicationPasswordAccount()
    }

    suspend fun closeAccount(): CloseAccountResult {
        val result = closeAccountStore.closeAccount()
        return if (result.isError) {
            when (result.error.type) {
                CloseAccountErrorType.EXISTING_ATOMIC_SITES -> CloseAccountResult.Error(hasActiveStores = true)
                CloseAccountErrorType.GENERIC_ERROR -> CloseAccountResult.Error(hasActiveStores = false)
            }
        } else {
            val event: OnAccountChanged = dispatcher.dispatchAndAwait(AccountActionBuilder.newSignOutAction())
            if (event.isError) {
                WooLog.d(LOGIN, "Error while trying to log out after successfully closing the account")
                CloseAccountResult.Error(hasActiveStores = false)
            } else {
                cleanup()
                CloseAccountResult.Success
            }
        }
    }

    private fun cleanup() {
        // Reset analytics
        AnalyticsTracker.flush()
        AnalyticsTracker.clearAllData()
        zendeskSettings.clearIdentity()

        // Wipe user-specific preferences and data stores
        appCoroutineScope.launch {
            prefs.resetUserPreferences()
            siteVisibilityDataStore.clearAll()
            posDataStore.edit { it.clear() }
        }

        selectedSite.reset()

        // Delete sites
        dispatcher.dispatch(SiteActionBuilder.newRemoveAllSitesAction())
    }

    private suspend fun logoutWpComAccount(): Boolean {
        val event: OnAccountChanged = dispatcher.dispatchAndAwait(AccountActionBuilder.newSignOutAction())
        if (event.isError) {
            WooLog.e(
                LOGIN,
                "Account error [type = ${event.causeOfChange}] : " +
                    "${event.error.type} > ${event.error.message}"
            )
            return false
        }

        deleteApplicationPasswordsOfUserSites()
        completeLogout()
        return true
    }

    private fun logoutApplicationPasswordAccount(): Boolean {
        deleteApplicationPassword(selectedSite.get())
        completeLogout()
        return true
    }

    private fun completeLogout() {
        AnalyticsTracker.track(AnalyticsEvent.ACCOUNT_LOGOUT)
        cleanup()
    }

    private suspend fun deleteApplicationPasswordsOfUserSites() {
        val sites = withContext(dispatchers.io) {
            siteStore.sitesAccessedViaWPComRest
        }

        // Start deleting passwords asynchronously in the background
        sites.forEach { site ->
            deleteApplicationPassword(site)
        }
    }

    private fun deleteApplicationPassword(site: SiteModel) {
        appCoroutineScope.launch {
            val result = siteStore.deleteApplicationPassword(site)
            val siteId = site.siteId.takeIf { it != 0L }
            if (result.isError) {
                WooLog.e(
                    LOGIN,
                    "Error deleting application password: ${siteId?.let { "Site Id: $it - " }.orEmpty()}" +
                        "${result.error.errorCode} > ${result.error.message}"
                )
            } else {
                WooLog.i(LOGIN, "Application password deleted${siteId?.let { " for site Id: $it" }.orEmpty()}")
            }
        }
    }

    sealed class CloseAccountResult {
        object Success : CloseAccountResult()
        data class Error(val hasActiveStores: Boolean) : CloseAccountResult()
    }
}
