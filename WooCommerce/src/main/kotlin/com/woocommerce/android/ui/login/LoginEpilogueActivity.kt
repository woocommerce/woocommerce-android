package com.woocommerce.android.ui.login

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.app.AppCompatDelegate
import android.support.v7.widget.LinearLayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.di.GlideApp
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.login.SitePickerAdapter.OnSiteClickListener
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.util.ActivityUtils
import dagger.android.AndroidInjection
import kotlinx.android.synthetic.main.activity_login_epilogue.*
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.ToastUtils.Duration
import javax.inject.Inject

class LoginEpilogueActivity : AppCompatActivity(), LoginEpilogueContract.View, OnSiteClickListener {
    companion object {
        init {
            AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        }
    }

    @Inject lateinit var presenter: LoginEpilogueContract.Presenter
    @Inject lateinit var accountStore: AccountStore
    @Inject lateinit var siteStore: SiteStore
    @Inject lateinit var selectedSite: SelectedSite

    private lateinit var siteAdapter : SitePickerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_epilogue)

        ActivityUtils.setStatusBarColor(this, R.color.wc_grey_status)
        presenter.takeView(this)

        recycler.layoutManager = LinearLayoutManager(this)
        siteAdapter = SitePickerAdapter(this, this)
        recycler.adapter = siteAdapter

        showUserInfo()
        showSiteList()

        button_continue.setOnClickListener {
            showMainActivityAndFinish()
        }
    }

    private fun showMainActivityAndFinish() {
        if (selectedSite.isSet()) {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            setResult(Activity.RESULT_OK)
            finish()
        }
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

    override fun onSiteClick(siteId: Long) {
        val site = siteStore.getSiteBySiteId(siteId)
        if (site != null) {
            selectedSite.set(site)
        }
    }
}
