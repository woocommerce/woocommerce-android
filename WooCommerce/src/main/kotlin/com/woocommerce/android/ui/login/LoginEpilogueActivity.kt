package com.woocommerce.android.ui.login

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.app.AppCompatDelegate
import android.support.v7.widget.LinearLayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.di.GlideApp
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.ActivityUtils
import dagger.android.AndroidInjection
import kotlinx.android.synthetic.main.activity_login_epilogue.*
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.ToastUtils.Duration
import javax.inject.Inject

class LoginEpilogueActivity : AppCompatActivity(), LoginEpilogueContract.View {
    companion object {
        init {
            AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        }
    }

    @Inject lateinit var presenter: LoginEpilogueContract.Presenter
    @Inject lateinit var accountStore: AccountStore
    @Inject lateinit var selectedSite: SelectedSite

    private lateinit var siteAdapter : SitePickerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_epilogue)

        ActivityUtils.setStatusBarColor(this, R.color.grey_lighten_20)
        presenter.takeView(this)

        recycler.layoutManager = LinearLayoutManager(this)
        siteAdapter = SitePickerAdapter(this)
        recycler.adapter = siteAdapter

        showUserInfo()
        showSiteList()
    }

    override fun showUserInfo() {
        text_displayname.text = accountStore.account?.displayName
        text_username.text = String.format(getString(R.string.at_username), accountStore.account?.userName)

        GlideApp.with(this)
                .load(accountStore.account?.avatarUrl)
                .placeholder(R.drawable.ic_placeholder_gravatar_grey_lighten_20_100dp)
                .circleCrop()
                .into(findViewById(R.id.image_avatar))
    }

    override fun showSiteList() {
        val wcSites = presenter.getWooCommerceSites()
        if (wcSites.isEmpty()) {
            // TODO
            ToastUtils.showToast(this, R.string.no_woocommerce_sites, Duration.LONG)
            return
        }

        if (!selectedSite.isSet()) {
            selectedSite.set(wcSites[0])
        }

        text_list_label.text = if (wcSites.size == 1)
            getString(R.string.login_connected_store)
        else
            getString(R.string.login_pick_store)
        siteAdapter.setSites(selectedSite.get().siteId, wcSites)
    }
}
