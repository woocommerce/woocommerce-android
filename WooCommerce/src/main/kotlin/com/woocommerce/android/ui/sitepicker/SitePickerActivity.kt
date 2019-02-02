package com.woocommerce.android.ui.sitepicker

import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.support.v7.content.res.AppCompatResources
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.Toolbar
import android.text.TextUtils
import android.view.MenuItem
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
import com.woocommerce.android.ui.login.LoginActivity
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.ui.sitepicker.SitePickerAdapter.OnSiteClickListener
import com.woocommerce.android.util.ActivityUtils
import com.woocommerce.android.util.CrashlyticsUtils
import dagger.android.AndroidInjection
import kotlinx.android.synthetic.main.activity_site_picker.*
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.login.LoginMode
import org.wordpress.android.util.DisplayUtils
import javax.inject.Inject

// TODO: tracks events used below will need to be renamed

class SitePickerActivity : AppCompatActivity(), SitePickerContract.View, OnSiteClickListener {
    companion object {
        private const val STATE_KEY_SITE_ID_LIST = "key-supported-site-id-list"
        private const val KEY_CALLED_FROM_LOGIN = "called_from_login"
        const val REQUEST_CODE = 1000 // TODO: we need a separate object to define request codes app-wide

        fun showSitePickerFromLogin(context: Context) {
            val intent = Intent(context, SitePickerActivity::class.java)
            intent.putExtra(KEY_CALLED_FROM_LOGIN, true)
            context.startActivity(intent)
        }

        fun showSitePickerForResult(activity: Activity) {
            val intent = Intent(activity, SitePickerActivity::class.java)
            intent.putExtra(KEY_CALLED_FROM_LOGIN, false)
            activity.startActivityForResult(intent, REQUEST_CODE)
        }
    }

    @Inject lateinit var presenter: SitePickerContract.Presenter
    @Inject lateinit var selectedSite: SelectedSite

    private lateinit var siteAdapter: SitePickerAdapter

    private var progressDialog: ProgressDialog? = null
    private var calledFromLogin: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_site_picker)

        calledFromLogin = savedInstanceState?.getBoolean(KEY_CALLED_FROM_LOGIN)
                ?: intent.getBooleanExtra(KEY_CALLED_FROM_LOGIN, false)

        if (calledFromLogin) {
            toolbar.visibility = View.GONE
            ActivityUtils.setStatusBarColor(this, R.color.wc_grey_mid)
        } else {
            toolbar.visibility = View.VISIBLE
            setSupportActionBar(toolbar as Toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            title = getString(R.string.login_pick_store)
            site_list_label.visibility = View.GONE
        }

        presenter.takeView(this)

        sites_recycler.layoutManager = LinearLayoutManager(this)
        siteAdapter = SitePickerAdapter(this, this)
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

        if (calledFromLogin) {
            button_help.setOnClickListener {
                startActivity(HelpActivity.createIntent(this, Origin.LOGIN_EPILOGUE, null))
                AnalyticsTracker.track(Stat.LOGIN_EPILOGUE_HELP_BUTTON_TAPPED)
            }
        } else {
            button_help.visibility = View.GONE
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

        outState.putBoolean(KEY_CALLED_FROM_LOGIN, calledFromLogin)
    }

    override fun onBackPressed() {
        setResult(Activity.RESULT_CANCELED)
        AnalyticsTracker.trackBackPressed(this)
        finish()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }

        return false
    }

    /**
     * Show the current user info if this was called from login, otherwise hide user views
     */
    override fun showUserInfo() {
        if (calledFromLogin) {
            text_displayname.text = presenter.getUserDisplayName()
            text_username.text = String.format(getString(R.string.at_username), presenter.getUserName())

            GlideApp.with(this)
                    .load(presenter.getUserAvatarUrl())
                    .placeholder(R.drawable.ic_placeholder_gravatar_grey_lighten_20_100dp)
                    .circleCrop()
                    .into(image_avatar)
        } else {
            text_displayname.visibility = View.GONE
            text_username.visibility = View.GONE
            image_avatar.visibility = View.GONE
        }
    }

    override fun showStoreList(wcSites: List<SiteModel>) {
        progressDialog?.takeIf { it.isShowing }?.dismiss()

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
        button_continue.setOnClickListener {
            val site = presenter.getSiteBySiteId(siteAdapter.selectedSiteId)
            site?.let { it ->
                AnalyticsTracker.track(
                        Stat.LOGIN_EPILOGUE_STORE_PICKER_CONTINUE_TAPPED,
                        mapOf(AnalyticsTracker.KEY_SELECTED_STORE_ID to site.id)
                )
                progressDialog = ProgressDialog.show(this, null, getString(R.string.login_verifying_site))
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
        progressDialog?.dismiss()
        finishWithSite(site)
    }

    override fun siteVerificationFailed(site: SiteModel) {
        progressDialog?.dismiss()

        siteAdapter.selectedSiteId = 0
        button_continue.isEnabled = false

        WooUpgradeRequiredDialog().show(supportFragmentManager)
    }

    override fun siteVerificationError(site: SiteModel) {
        progressDialog?.dismiss()

        val siteName = if (!TextUtils.isEmpty(site.name)) site.name else getString(R.string.untitled)
        Snackbar.make(
                site_picker_root as ViewGroup,
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
    override fun didLogout() {
        setResult(Activity.RESULT_CANCELED)
        val intent = Intent(this, LoginActivity::class.java)
        LoginMode.WPCOM_LOGIN_ONLY.putInto(intent)
        startActivity(intent)
        finish()
    }

    /**
     * User has selected a site and it passed the verification process
     */
    private fun finishWithSite(site: SiteModel) {
        val isSameSite = selectedSite.getIfExists()?.let {
            it.siteId == site.siteId
        } ?: false

        if (isSameSite) {
            setResult(Activity.RESULT_CANCELED)
        } else {
            setResult(Activity.RESULT_OK)

            selectedSite.set(site)
            CrashlyticsUtils.initSite(site)

            // Now that the SelectedSite is set, register the device for WordPress.com Woo push notifications for this site
            FCMRegistrationIntentService.enqueueWork(this)

            // TODO: do we need to clear any existing data since site has changed?
        }

        // if we came here from login, start the main activity
        if (calledFromLogin) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        finish()
    }
}
