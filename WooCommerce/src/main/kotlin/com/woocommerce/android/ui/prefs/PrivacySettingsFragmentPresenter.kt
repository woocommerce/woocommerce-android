package com.woocommerce.android.ui.prefs

import com.woocommerce.android.analytics.AnalyticsTracker
import kotlinx.android.synthetic.main.fragment_settings_privacy.*
import org.wordpress.android.fluxc.Dispatcher
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
}
