package com.woocommerce.android.ui.login

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.support.v7.content.res.AppCompatResources
import android.support.v7.widget.LinearLayoutManager
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.di.GlideApp
import com.woocommerce.android.push.FCMRegistrationIntentService
import com.woocommerce.android.support.HelpActivity
import com.woocommerce.android.support.HelpActivity.Origin
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.login.adapter.SiteListAdapter
import com.woocommerce.android.ui.login.adapter.SiteListAdapter.OnSiteClickListener
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.util.ActivityUtils
import com.woocommerce.android.util.CrashlyticsUtils
import dagger.android.AndroidInjection
import kotlinx.android.synthetic.main.activity_login_epilogue.*
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.login.LoginMode
import org.wordpress.android.util.DisplayUtils
import javax.inject.Inject

class LoginEpilogueActivity : AppCompatActivity(), LoginEpilogueContract.View, OnSiteClickListener {
    companion object {
        private const val STATE_KEY_SITE_ID_LIST = "key-supported-site-id-list"
    }

    @Inject lateinit var presenter: LoginEpilogueContract.Presenter
    @Inject lateinit var selectedSite: SelectedSite

    private lateinit var siteAdapter: SiteListAdapter

    private var loginProgressDialog: ProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_epilogue)

        ActivityUtils.setStatusBarColor(this, R.color.wc_grey_mid)
        presenter.takeView(this)

        sites_recycler.layoutManager = LinearLayoutManager(this)
        siteAdapter = SiteListAdapter(this, this)
        sites_recycler.adapter = siteAdapter

        showUserInfo()

        savedInstanceState?.let { bundle ->
            val sites = presenter.getSitesForLocalIds(bundle.getIntArray(STATE_KEY_SITE_ID_LIST))
            showStoreList(sites)
        } ?: run {
            site_list_container.visibility = View.GONE
            presenter.loadSites()

            AnalyticsTracker.track(
                    Stat.LOGIN_EPILOGUE_STORES_SHOWN,
                    mapOf(AnalyticsTracker.KEY_NUMBER_OF_STORES to presenter.getWooCommerceSites().size))
        }

        button_help.setOnClickListener {
            startActivity(HelpActivity.createIntent(this, Origin.LOGIN_EPILOGUE, null))
            AnalyticsTracker.track(Stat.LOGIN_EPILOGUE_HELP_BUTTON_TAPPED)
        }
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onDestroy() {
        presenter.dropView()
        super.onDestroy()
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        val sitesList = siteAdapter.siteList.map { it.id }

        outState.putIntArray(STATE_KEY_SITE_ID_LIST, sitesList.toIntArray())
    }

    override fun onBackPressed() {
        AnalyticsTracker.trackBackPressed(this)

        finish()
    }

    override fun showUserInfo() {
        text_displayname.text = presenter.getUserDisplayName()
        text_username.text = String.format(getString(R.string.at_username), presenter.getUserName())

        GlideApp.with(this)
                .load(presenter.getUserAvatarUrl())
                .placeholder(R.drawable.ic_placeholder_gravatar_grey_lighten_20_100dp)
                .circleCrop()
                .into(image_avatar)
    }

    override fun showStoreList(wcSites: List<SiteModel>) {
        loginProgressDialog?.takeIf { it.isShowing }?.dismiss()

        if (wcSites.isEmpty()) {
            showNoStoresView()
            return
        }

        site_list_container.visibility = View.VISIBLE
        site_list_label.text = if (wcSites.size == 1)
            getString(R.string.login_connected_store)
        else
            getString(R.string.login_pick_store)

        siteAdapter.siteList = wcSites

        if (selectedSite.exists()) {
            siteAdapter.selectedSiteId = selectedSite.get().siteId
        } else {
            siteAdapter.selectedSiteId = wcSites[0].siteId
        }

        button_continue.text = getString(R.string.continue_button)
        button_continue.isEnabled = true
        button_continue.setOnClickListener { _ ->
            val site = presenter.getSiteBySiteId(siteAdapter.selectedSiteId)
            site?.let { it ->
                AnalyticsTracker.track(
                        Stat.LOGIN_EPILOGUE_STORE_PICKER_CONTINUE_TAPPED,
                        mapOf(AnalyticsTracker.KEY_SELECTED_STORE_ID to site.id))
                loginProgressDialog = ProgressDialog.show(this, null, getString(R.string.login_verifying_site))
                presenter.verifySiteApiVersion(it)
                // Preemptively also update the site settings so we have them available sooner
                presenter.updateWooSiteSettings(it)
            }
        }
    }

    override fun onSiteClick(siteId: Long) {
        button_continue.isEnabled = true
    }

    override fun siteVerificationPassed(site: SiteModel) {
        loginProgressDialog?.dismiss()
        selectedSite.set(site)
        CrashlyticsUtils.initSite(site)
        finishEpilogue()
    }

    override fun siteVerificationFailed(site: SiteModel) {
        loginProgressDialog?.dismiss()

        siteAdapter.selectedSiteId = 0
        button_continue.isEnabled = false

        val ft = supportFragmentManager.beginTransaction()
        val prev = supportFragmentManager.findFragmentByTag(WooUpgradeRequiredDialog.TAG)
        if (prev != null) {
            ft.remove(prev)
        }
        ft.addToBackStack(null)
        val dialogFragment = WooUpgradeRequiredDialog()
        dialogFragment.show(ft, WooUpgradeRequiredDialog.TAG)
    }

    override fun siteVerificationError(site: SiteModel) {
        loginProgressDialog?.dismiss()

        val siteName = if (!TextUtils.isEmpty(site.name)) site.name else getString(R.string.untitled)
        Snackbar.make(
                login_root as ViewGroup,
                getString(R.string.login_verifying_site_error, siteName),
                Snackbar.LENGTH_LONG
        ).show()
    }

    private fun showNoStoresView() {
        site_list_container.visibility = View.GONE
        no_stores_view.visibility = View.VISIBLE

        val noStoresImage =
                if (DisplayUtils.isLandscape(this)) null
                else AppCompatResources.getDrawable(this, R.drawable.ic_woo_no_store)
        no_stores_view.setCompoundDrawablesWithIntrinsicBounds(null, noStoresImage, null, null)

        button_continue.text = getString(R.string.login_with_a_different_account)
        button_continue.isEnabled = true
        button_continue.setOnClickListener {
            presenter.logout()
        }
    }

    /**
     * called by the presenter after logout completes
     */
    override fun cancel() {
        showLoginActivityAndFinish()
    }

    private fun showLoginActivityAndFinish() {
        val intent = Intent(this, LoginActivity::class.java)
        LoginMode.WPCOM_LOGIN_ONLY.putInto(intent)
        startActivity(intent)
        finish()
    }

    private fun finishEpilogue() {
        // Now that the SelectedSite is set, register the device for WordPress.com Woo push notifications for this site
        FCMRegistrationIntentService.enqueueWork(this)

        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
