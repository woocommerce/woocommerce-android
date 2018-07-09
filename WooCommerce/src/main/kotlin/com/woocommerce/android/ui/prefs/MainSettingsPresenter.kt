package com.woocommerce.android.ui.prefs

import com.woocommerce.android.tools.SelectedSite
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.util.UrlUtils
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
    override fun getStoreDomainName(): String = UrlUtils.getHost(selectedSite.get().url)
}
