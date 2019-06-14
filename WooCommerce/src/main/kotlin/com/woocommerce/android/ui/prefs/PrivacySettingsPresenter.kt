package com.woocommerce.android.ui.prefs

import android.content.Context
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.SETTING_CHANGE
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.SETTING_CHANGE_FAILED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.SETTING_CHANGE_SUCCESS
import com.woocommerce.android.tools.SelectedSite
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.AccountAction.PUSH_SETTINGS
import org.wordpress.android.fluxc.generated.AccountActionBuilder
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.AccountStore.AccountErrorType.SETTINGS_POST_ERROR
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged
import org.wordpress.android.fluxc.store.AccountStore.PushAccountSettingsPayload
import javax.inject.Inject

class PrivacySettingsPresenter @Inject constructor(
    private val dispatcher: Dispatcher,
    private val accountStore: AccountStore,
    private val selectedSite: SelectedSite
) : PrivacySettingsContract.Presenter {
    companion object {
        private const val SETTING_TRACKS_OPT_OUT = "tracks_opt_out"
    }
    private var privacySettingsFragmentView: PrivacySettingsContract.View? = null

    override fun takeView(view: PrivacySettingsContract.View) {
        dispatcher.register(this)
        privacySettingsFragmentView = view
    }

    override fun dropView() {
        dispatcher.unregister(this)
        privacySettingsFragmentView = null
    }

    override fun getSendUsageStats() = !accountStore.account.tracksOptOut

    override fun setSendUsageStats(sendUsageStats: Boolean) {
        // note that we don't init/disable Crashlytics here because that requires the app to be restarted
        AnalyticsTracker.sendUsageStats = sendUsageStats

        // sync with wpcom if a token is available
        if (accountStore.hasAccessToken()) {
            AnalyticsTracker.track(SETTING_CHANGE, mapOf(
                    AnalyticsTracker.KEY_NAME to SETTING_TRACKS_OPT_OUT,
                    AnalyticsTracker.KEY_FROM to !sendUsageStats,
                    AnalyticsTracker.KEY_TO to sendUsageStats))

            val payload = PushAccountSettingsPayload().apply {
                params = mapOf(SETTING_TRACKS_OPT_OUT to !sendUsageStats)
            }
            dispatcher.dispatch(AccountActionBuilder.newPushSettingsAction(payload))
        }
    }

    override fun getCrashReportingEnabled() = AppPrefs.isCrashReportingEnabled()

    override fun setCrashReportingEnabled(context: Context, enabled: Boolean) {
        AppPrefs.setCrashReportingEnabled(enabled)
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onAccountChanged(event: OnAccountChanged) {
        if (event.isError) {
            when (event.error.type) {
                SETTINGS_POST_ERROR -> {
                    AnalyticsTracker.track(
                            SETTING_CHANGE_FAILED,
                            this::class.java.simpleName,
                            event.error.type.toString(),
                            event.error.message)
                }
                else -> {}
            }
        } else {
            when (event.causeOfChange) {
                PUSH_SETTINGS -> {
                    AnalyticsTracker.track(SETTING_CHANGE_SUCCESS)
                }
            }
        }
    }
}
