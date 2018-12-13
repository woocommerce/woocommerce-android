package com.woocommerce.android.ui.prefs

import android.content.Context
import com.woocommerce.android.push.NotificationHandler
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.StringUtils
import org.wordpress.android.fluxc.store.AccountStore
import javax.inject.Inject

class MainSettingsPresenter @Inject constructor(
    private val selectedSite: SelectedSite,
    private val accountStore: AccountStore
) : MainSettingsContract.Presenter {
    private var appSettingsFragmentView: MainSettingsContract.View? = null

    override fun takeView(view: MainSettingsContract.View) {
        appSettingsFragmentView = view
    }

    override fun dropView() {
        appSettingsFragmentView = null
    }

    override fun getUserDisplayName(): String = accountStore.account.displayName
    override fun getStoreDomainName(): String = StringUtils.getSiteDomainAndPath(selectedSite.get())

    override fun testNotif(context: Context) {
        NotificationHandler.testNotification(context, "title", "message", accountStore.account)
    }
}
