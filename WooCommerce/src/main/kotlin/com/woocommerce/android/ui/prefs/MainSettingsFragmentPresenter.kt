package com.woocommerce.android.ui.prefs

import com.woocommerce.android.tools.SelectedSite
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.util.UrlUtils
import javax.inject.Inject

class MainSettingsFragmentPresenter @Inject constructor(
    private val selectedSite: SelectedSite,
    private val accountStore: AccountStore
) : MainSettingsFragmentContract.Presenter {
    private var appSettingsFragmentView: MainSettingsFragmentContract.View? = null

    override fun takeView(view: MainSettingsFragmentContract.View) {
        appSettingsFragmentView = view
    }

    override fun dropView() {
        appSettingsFragmentView = null
    }

    override fun getUserDisplayName(): String = accountStore.account.displayName
    override fun getStoreDomainName(): String = UrlUtils.getHost(selectedSite.get().url)
}
