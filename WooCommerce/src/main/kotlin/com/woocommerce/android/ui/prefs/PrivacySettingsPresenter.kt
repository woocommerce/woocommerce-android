package com.woocommerce.android.ui.prefs

import com.woocommerce.android.analytics.AnalyticsTracker
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.AccountActionBuilder
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged
import org.wordpress.android.fluxc.store.AccountStore.PushAccountSettingsPayload
import javax.inject.Inject

class PrivacySettingsPresenter @Inject constructor(
    private val dispatcher: Dispatcher,
        private val accountStore: AccountStore
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

        // sync with wpcom if a token is available
        if (accountStore.hasAccessToken()) {
            accountStore.getAccount().setTracksOptOut(!allowUsageTracking)
            val payload = PushAccountSettingsPayload()
            payload.params = HashMap<String, Any>()
            payload.params.put("tracks_opt_out", !allowUsageTracking)
            dispatcher.dispatch(AccountActionBuilder.newPushSettingsAction(payload))
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onAccountChanged(event: OnAccountChanged) {
        if (event.isError) {
            // TODO ?
        }
    }
}
