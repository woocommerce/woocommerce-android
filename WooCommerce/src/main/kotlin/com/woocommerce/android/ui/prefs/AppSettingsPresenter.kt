package com.woocommerce.android.ui.prefs

import com.woocommerce.android.ui.login.AccountRepository
import com.woocommerce.android.ui.payments.cardreader.ClearCardReaderDataAction
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.store.NotificationStore
import javax.inject.Inject

class AppSettingsPresenter @Inject constructor(
    private val accountRepository: AccountRepository,
    @Suppress("unused") // We keep it here to make sure that the store is subscribed to the event bus
    private val notificationStore: NotificationStore,
    private val clearCardReaderDataAction: ClearCardReaderDataAction
) : AppSettingsContract.Presenter {
    private var appSettingsView: AppSettingsContract.View? = null

    override fun takeView(view: AppSettingsContract.View) {
        appSettingsView = view
    }

    override fun dropView() {
        appSettingsView = null
    }

    override fun logout() {
        coroutineScope.launch {
            accountRepository.logout().let {
                if (it) {
                    clearCardReaderDataAction()
                    appSettingsView?.finishLogout()
                }
            }
        }
    }

    override fun userIsLoggedIn(): Boolean = accountRepository.isUserLoggedIn()

    override fun getAccountDisplayName(): String {
        return accountRepository.getUserAccount()?.displayName ?: ""
    }
}
