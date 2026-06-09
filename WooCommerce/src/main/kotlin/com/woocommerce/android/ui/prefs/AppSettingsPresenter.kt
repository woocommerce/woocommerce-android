package com.woocommerce.android.ui.prefs

import com.woocommerce.android.ui.login.AccountRepository
import com.woocommerce.android.ui.payments.cardreader.ClearCardReaderDataAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.store.WpComPushNotificationStore
import javax.inject.Inject

class AppSettingsPresenter @Inject constructor(
    private val accountRepository: AccountRepository,
    @Suppress("unused") // We keep it here to make sure that the store is subscribed to the event bus
    private val wpComPushNotificationStore: WpComPushNotificationStore,
    private val clearCardReaderDataAction: ClearCardReaderDataAction
) : AppSettingsContract.Presenter {
    override val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var appSettingsView: AppSettingsContract.View? = null
    private var logoutState: LogoutState = LogoutState.IDLE

    override fun takeView(view: AppSettingsContract.View) {
        appSettingsView = view

        when (logoutState) {
            LogoutState.IN_PROGRESS -> view.showLogoutProgressDialog()
            LogoutState.PENDING_FINISH -> finishLogout()
            LogoutState.IDLE -> Unit
        }
    }

    override fun dropView(view: AppSettingsContract.View) {
        if (appSettingsView === view) {
            appSettingsView = null
        }
    }

    override fun logout() {
        if (logoutState == LogoutState.IN_PROGRESS) {
            appSettingsView?.showLogoutProgressDialog()
            return
        }

        if (logoutState == LogoutState.PENDING_FINISH) {
            finishLogout()
            return
        }

        logoutState = LogoutState.IN_PROGRESS
        appSettingsView?.showLogoutProgressDialog()

        coroutineScope.launch {
            val isLoggedOut = runCatching { accountRepository.logout() }.getOrDefault(false)
            if (isLoggedOut) {
                runCatching { clearCardReaderDataAction() }
                logoutState = LogoutState.PENDING_FINISH
                finishLogout()
            } else {
                logoutState = LogoutState.IDLE
                appSettingsView?.hideLogoutProgressDialog()
            }
        }
    }

    override fun userIsLoggedIn(): Boolean = accountRepository.isUserLoggedIn()

    override fun getAccountDisplayName(): String {
        return accountRepository.getUserAccount()?.displayName ?: ""
    }

    private fun finishLogout() {
        appSettingsView?.let {
            logoutState = LogoutState.IDLE
            it.finishLogout()
        }
    }

    private enum class LogoutState {
        IDLE,
        IN_PROGRESS,
        PENDING_FINISH,
    }
}
