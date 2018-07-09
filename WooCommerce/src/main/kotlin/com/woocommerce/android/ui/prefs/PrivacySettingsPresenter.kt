package com.woocommerce.android.ui.prefs

import com.woocommerce.android.analytics.AnalyticsTracker
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged
import javax.inject.Inject

class PrivacySettingsPresenter @Inject constructor(
    private val dispatcher: Dispatcher
) : PrivacySettingsContract.Presenter {
    private var privacySettingsFragmentView: PrivacySettingsContract.View? = null

    override fun takeView(view: PrivacySettingsContract.View) {
        dispatcher.register(this)
        privacySettingsFragmentView = view
    }

    override fun dropView() {
        dispatcher.unregister(this)
        privacySettingsFragmentView = null
    }

    override fun updateUsagePref(allowUsageTracking: Boolean) {
        AnalyticsTracker.sendUsageStats = allowUsageTracking
        // TODO: update in wp account
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onAccountChanged(event: OnAccountChanged) {
        // TODO ?
    }
}
