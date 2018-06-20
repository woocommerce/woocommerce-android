package com.woocommerce.android.ui.login

import android.app.Activity
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import com.woocommerce.android.R
import com.woocommerce.android.di.GlideApp
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.login.SitePickerAdapter.OnSiteClickListener
import com.woocommerce.android.util.ActivityUtils
import dagger.android.AndroidInjection
import kotlinx.android.synthetic.main.activity_login_epilogue.*
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import javax.inject.Inject

class LoginEpilogueActivity : AppCompatActivity(), LoginEpilogueContract.View, OnSiteClickListener {
    @Inject lateinit var presenter: LoginEpilogueContract.Presenter
    @Inject lateinit var accountStore: AccountStore
    @Inject lateinit var siteStore: SiteStore
    @Inject lateinit var selectedSite: SelectedSite

    private lateinit var siteAdapter: SitePickerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_epilogue)

        ActivityUtils.setStatusBarColor(this, R.color.wc_grey_mid)
        presenter.takeView(this)

        recycler.layoutManager = LinearLayoutManager(this)
        siteAdapter = SitePickerAdapter(this, this)
        recycler.adapter = siteAdapter

        showUserInfo()
        showStoreList()
    }

    override fun onBackPressed() {
        setResult(if (selectedSite.isSet()) RESULT_OK else RESULT_CANCELED)
        super.onBackPressed()
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

    override fun showStoreList() {
        val wcSites = presenter.getWooCommerceSites()
        if (wcSites.isEmpty()) {
            showNoStoresView()
            return
        }

        if (!selectedSite.isSet()) {
            selectedSite.set(wcSites[0])
        }

        text_list_label.text = if (wcSites.size == 1)
            getString(R.string.login_connected_store)
        else
            getString(R.string.login_pick_store)

        siteAdapter.selectedSiteId = selectedSite.get().siteId
        siteAdapter.siteList = wcSites

        button_continue.setOnClickListener {
            setResult(Activity.RESULT_OK)
            finish()
        }
    }

    override fun onSiteClick(siteId: Long) {
        val site = siteStore.getSiteBySiteId(siteId)
        if (site != null) {
            selectedSite.set(site)
        }
    }

    private fun showNoStoresView() {
        frame_list_container.visibility = View.GONE
        no_stores_view.visibility = View.VISIBLE
        button_continue.text = getString(R.string.login_with_a_different_account)
        button_continue.setOnClickListener {
            presenter.logout()
        }
    }

    override fun cancel() {
        setResult(Activity.RESULT_CANCELED)
        finish()
    }
}
