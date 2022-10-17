package com.woocommerce.android.ui.prefs

import com.woocommerce.android.ui.login.AccountRepository
import com.woocommerce.android.ui.payments.cardreader.ClearCardReaderDataAction
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.NotificationActionBuilder
import org.wordpress.android.fluxc.store.NotificationStore
import org.wordpress.android.fluxc.store.NotificationStore.OnDeviceUnregistered
import javax.inject.Inject

class AppSettingsPresenter @Inject constructor(
    private val dispatcher: Dispatcher,
    private val accountRepository: AccountRepository,
    @Suppress("unused") // We keep it here to make sure that the store is subscribed to the event bus
    private val notificationStore: NotificationStore,
    private val clearCardReaderDataAction: ClearCardReaderDataAction
) : AppSettingsContract.Presenter {
    private var appSettingsView: AppSettingsContract.View? = null

    override fun takeView(view: AppSettingsContract.View) {
        dispatcher.register(this)
        appSettingsView = view
    }

    override fun dropView() {
        dispatcher.unregister(this)
        appSettingsView = null
    }

    override fun logout() {
        coroutineScope.launch { clearCardReaderDataAction() }
        // First unregister the device for push notifications
        dispatcher.dispatch(NotificationActionBuilder.newUnregisterDeviceAction())
    }

    override fun userIsLoggedIn(): Boolean = accountRepository.isUserLoggedIn()

    override fun getAccountDisplayName(): String {
        return accountRepository.getUserAccount()?.displayName ?: ""
    }

    @Suppress("unused", "UNUSED_PARAMETER")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onDeviceUnregistered(event: OnDeviceUnregistered) {
        // Now that we've unregistered the device, we can logout
        coroutineScope.launch {
            accountRepository.logout().let {
                if (it) {
                    appSettingsView?.finishLogout()
                }
            }
        }
    }
}
