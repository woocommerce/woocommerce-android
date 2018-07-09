package com.woocommerce.android.ui.prefs

import com.woocommerce.android.analytics.AnalyticsTracker
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged
import javax.inject.Inject

class PrivacySettingsFragmentPresenter @Inject constructor(
    private val dispatcher: Dispatcher
) : PrivacySettingsFragmentContract.Presenter {
    private var privacySettingsFragmentView: PrivacySettingsFragmentContract.View? = null

    override fun takeView(view: PrivacySettingsFragmentContract.View) {
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
