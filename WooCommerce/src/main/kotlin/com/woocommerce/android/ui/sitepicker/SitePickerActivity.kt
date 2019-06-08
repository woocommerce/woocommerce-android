package com.woocommerce.android.ui.sitepicker

import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.di.GlideApp
import com.woocommerce.android.push.FCMRegistrationIntentService
import com.woocommerce.android.support.HelpActivity
import com.woocommerce.android.support.HelpActivity.Origin
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.login.LoginActivity
import com.woocommerce.android.ui.login.LoginEmailHelpDialogFragment
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.ui.sitepicker.SitePickerAdapter.OnSiteClickListener
import com.woocommerce.android.util.ActivityUtils
import com.woocommerce.android.util.CrashlyticsUtils
import com.woocommerce.android.widgets.SkeletonView
import dagger.android.AndroidInjection
import kotlinx.android.synthetic.main.activity_site_picker.*
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.login.LoginMode
import org.wordpress.android.util.DisplayUtils
import javax.inject.Inject

class SitePickerActivity : AppCompatActivity(), SitePickerContract.View, OnSiteClickListener,
        LoginEmailHelpDialogFragment.Listener
{
    companion object {
        private const val STATE_KEY_SITE_ID_LIST = "key-supported-site-id-list"
        private const val KEY_CALLED_FROM_LOGIN = "called_from_login"
        private const val KEY_LOGIN_SITE_URL = "login_site"

        fun showSitePickerFromLogin(context: Context) {
            val intent = Intent(context, SitePickerActivity::class.java)
            intent.putExtra(KEY_CALLED_FROM_LOGIN, true)
            context.startActivity(intent)
        }

        fun showSitePickerForResult(activity: Activity, requestCode: Int) {
            val intent = Intent(activity, SitePickerActivity::class.java)
            intent.putExtra(KEY_CALLED_FROM_LOGIN, false)
            activity.startActivityForResult(intent, requestCode)
        }
    }

    @Inject lateinit var presenter: SitePickerContract.Presenter
    @Inject lateinit var selectedSite: SelectedSite

    private lateinit var siteAdapter: SitePickerAdapter

    private var progressDialog: ProgressDialog? = null
    private var calledFromLogin: Boolean = false
    private var currentSite: SiteModel? = null
    private var skeletonView = SkeletonView()

    /**
     * Signin M1: The url the customer logged into the app with.
     */
    private var loginSiteUrl: String? = null

    /**
     * Signin M1: Don't display the sites for selection if this is true.
     */
    private var deferLoadingSitesIntoView: Boolean = false

    /**
     * Signin M1: Tracks whether or not there are stores for the user to view.
     * This controls the "view connected stores" button visibility.
     */
    private var hasConnectedStores: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_site_picker)

        currentSite = selectedSite.getIfExists()

        calledFromLogin = savedInstanceState?.getBoolean(KEY_CALLED_FROM_LOGIN)
                ?: intent.getBooleanExtra(KEY_CALLED_FROM_LOGIN, false)

        if (calledFromLogin) {
            toolbar.visibility = View.GONE
            ActivityUtils.setStatusBarColor(this, R.color.wc_grey_mid)
            button_help.setOnClickListener {
                startActivity(HelpActivity.createIntent(this, Origin.LOGIN_EPILOGUE, null))
                AnalyticsTracker.track(Stat.SITE_PICKER_HELP_BUTTON_TAPPED)
            }
        } else {
            site_picker_root.setBackgroundColor(
                    ContextCompat.getColor(this, R.color.white))

            toolbar.visibility = View.VISIBLE
            setSupportActionBar(toolbar as Toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)

            title = getString(R.string.site_picker_title)
            button_help.visibility = View.GONE
            site_list_label.visibility = View.GONE
            site_list_container.cardElevation = 0f
            (site_list_container.layoutParams as MarginLayoutParams).topMargin = 0
            (site_list_container.layoutParams as MarginLayoutParams).bottomMargin = 0
            sites_recycler.setPadding(
                    resources.getDimensionPixelSize(R.dimen.margin_extra_large),
                    resources.getDimensionPixelSize(R.dimen.margin_large),
                    resources.getDimensionPixelSize(R.dimen.margin_extra_large),
                    resources.getDimensionPixelSize(R.dimen.margin_large))
        }

        presenter.takeView(this)

        sites_recycler.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        siteAdapter = SitePickerAdapter(this, this)
        sites_recycler.adapter = siteAdapter

        showUserInfo()

        savedInstanceState?.let { bundle ->
            val sites = presenter.getSitesForLocalIds(bundle.getIntArray(STATE_KEY_SITE_ID_LIST))
            hasConnectedStores = sites.isNotEmpty()

            // Signin M1: If using new login M1 flow, we skip showing the store list.
            bundle.getString(KEY_LOGIN_SITE_URL)?.let { url ->
                processLoginSite(url)
                deferLoadingSitesIntoView = true
                return
            } ?: showStoreList(sites)
        } ?: run {
            // Signin M1: If using a url to login, try finding the site by this url
            AppPrefs.getLoginSiteAddress()?.let { url ->
                // Delete the login site address from AppPrefs
                AppPrefs.clearLoginSiteAddress()

                processLoginSite(url)
                deferLoadingSitesIntoView = true
            }

            // Signin M1: We still want the presenter to go out and fetch sites so we
            // know whether or not to show the "view connected stores" button.
            presenter.loadAndFetchSites()

            AnalyticsTracker.track(
                    Stat.SITE_PICKER_STORES_SHOWN,
                    mapOf(AnalyticsTracker.KEY_NUMBER_OF_STORES to presenter.getWooCommerceSites().size)
            )
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

        loginSiteUrl?.let { outState.putString(KEY_LOGIN_SITE_URL, it) }
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
            user_info_group.visibility = View.VISIBLE
            text_displayname.text = presenter.getUserDisplayName()

            presenter.getUserName()?.let { userName ->
                if (userName.isNotEmpty()) {
                    text_username.text = String.format(getString(R.string.at_username), userName)
                }
            }

            GlideApp.with(this)
                    .load(presenter.getUserAvatarUrl())
                    .placeholder(R.drawable.ic_placeholder_gravatar_grey_lighten_20_100dp)
                    .circleCrop()
                    .into(image_avatar)
        } else {
            user_info_group.visibility = View.GONE
        }
    }

    override fun showStoreList(wcSites: List<SiteModel>) {
        if (deferLoadingSitesIntoView) {
            if (wcSites.size > 0) {
                hasConnectedStores = true

                // Make "show connected stores" visible to the user
                button_continue.visibility = View.VISIBLE
            } else {
                hasConnectedStores = false

                // Hide "show connected stores"
                button_continue.visibility = View.GONE
            }
            return
        }

        progressDialog?.takeIf { it.isShowing }?.dismiss()
        site_picker_root.visibility = View.VISIBLE

        if (wcSites.isEmpty()) {
            showNoStoresView()
            return
        }

        no_stores_view.visibility = View.GONE
        site_list_container.visibility = View.VISIBLE

        site_list_label.text = if (wcSites.size == 1) {
            getString(R.string.login_connected_store)
        } else if (calledFromLogin) {
            getString(R.string.login_pick_store)
        } else {
            getString(R.string.site_picker_title)
        }

        siteAdapter.siteList = wcSites
        siteAdapter.selectedSiteId = selectedSite.getIfExists()?.siteId ?: wcSites[0].siteId

        button_continue.text = getString(R.string.continue_button)
        button_continue.isEnabled = true
        button_continue.setOnClickListener {
            presenter.getSiteBySiteId(siteAdapter.selectedSiteId)?.let { site -> siteSelected(site) }
        }
    }

    override fun onSiteClick(siteId: Long) {
        button_continue.isEnabled = true
    }

    override fun siteSelected(site: SiteModel) {
        // finish if user simply selected the current site
        currentSite?.let {
            if (site.siteId == it.siteId) {
                setResult(Activity.RESULT_CANCELED)
                finish()
                return
            }
        }

        AnalyticsTracker.track(
                Stat.SITE_PICKER_CONTINUE_TAPPED,
                mapOf(AnalyticsTracker.KEY_SELECTED_STORE_ID to site.id)
        )

        progressDialog = ProgressDialog.show(this, null, getString(R.string.login_verifying_site))
        presenter.verifySiteApiVersion(site)

        // Preemptively also update the site settings so we have them available sooner
        presenter.updateWooSiteSettings(site)
    }

    /**
     * User selected a site and it passed verification - make it the selected site and finish
     */
    override fun siteVerificationPassed(site: SiteModel) {
        progressDialog?.dismiss()

        selectedSite.set(site)
        CrashlyticsUtils.initSite(site)
        FCMRegistrationIntentService.enqueueWork(this)

        // if we came here from login, start the main activity
        if (calledFromLogin) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        setResult(Activity.RESULT_OK)
        finish()
    }

    override fun siteVerificationFailed(site: SiteModel) {
        progressDialog?.dismiss()

        // re-select the previous site, if there was one
        siteAdapter.selectedSiteId = currentSite?.siteId ?: 0L
        button_continue.isEnabled = siteAdapter.selectedSiteId != 0L

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

    override fun showNoStoresView() {
        if (deferLoadingSitesIntoView) {
            return
        }

        site_picker_root.visibility = View.VISIBLE
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

    override fun showSkeleton(show: Boolean) {
        if (deferLoadingSitesIntoView) {
            return
        }

        when (show) {
            true -> skeletonView.show(sites_recycler, R.layout.skeleton_site_picker, delayed = true)
            false -> skeletonView.hide()
        }
    }

    /**
     * called by the presenter after logout completes - this would occur if the user was logged out
     * as a result of having no stores, which would only happen during the login flow
     */
    override fun didLogout() {
        setResult(Activity.RESULT_CANCELED)
        val intent = Intent(this, LoginActivity::class.java)
        LoginMode.WOO_LOGIN_MODE.putInto(intent)
        startActivity(intent)
        finish()
    }

    // region SignIn M1
    /**
     * Signin M1: User logged in with a URL. Here we check that login url to see
     * if the site is (in this order):
     * - Connected to the same account the user logged in with
     * - Has WooCommerce installed
     */
    private fun processLoginSite(url: String) {
        // TODO tracks events

        loginSiteUrl = url

        selectedSite.getSiteModelByUrl(url)?.let { site ->
            if (!site.hasWooCommerce) {
                // Show not woo store message view.
                showSiteNotWooStore(site.url, site.name)
            } else {
                // We have a pre-validation woo store. Attempt to just
                // login with this store directly.
                siteSelected(site)
            }
        } ?: run {
            // The url doesn't match any sites for this account.
            showSiteNotConnectedView(url)
        }
    }

    /**
     * SignIn M1: Shows the list of sites connected to the logged
     * in user.
     */
    private fun showConnectedSites() {
        deferLoadingSitesIntoView = false
        presenter.loadSites()
    }

    /**
     * SignIn M1: The url the user submitted during login belongs
     * to a site that is not connected to the account the user logged
     * in with.
     */
    override fun showSiteNotConnectedView(url: String) {
        // TODO tracks events

        site_picker_root.visibility = View.VISIBLE
        no_stores_view.visibility = View.VISIBLE
        button_email_help.visibility = View.VISIBLE

        no_stores_view.text = getString(R.string.login_not_connected_to_account, url)

        button_email_help.setOnClickListener {
            LoginEmailHelpDialogFragment().show(supportFragmentManager, LoginEmailHelpDialogFragment.TAG)
        }

        with(button_try_another) {
            visibility = View.VISIBLE
            setOnClickListener { presenter.logout() }
        }

        with(button_continue) {
            text = getString(R.string.login_view_connected_stores)
            setOnClickListener { showConnectedSites() }
            visibility = if (hasConnectedStores) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }
    }

    /**
     * SignIn M1: The user the user submitted during login belongs
     * to a site that does not have WooCommerce installed.
     */
    override fun showSiteNotWooStore(url: String, name: String?) {
        // TODO tracks events

        site_picker_root.visibility = View.VISIBLE
        no_stores_view.visibility = View.VISIBLE

        val siteName = name.takeIf { !it.isNullOrEmpty() } ?: url
        no_stores_view.text = getString(R.string.login_not_woo_store, siteName)

        with(button_try_another) {
            visibility = View.VISIBLE
            setOnClickListener { presenter.logout() }
        }

        with(button_continue) {
            text = getString(R.string.login_view_connected_stores)
            setOnClickListener { showConnectedSites() }
            visibility = if (hasConnectedStores) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }
    }

    override fun onEmailNeedMoreHelpClicked() {
        // TODO tracks

        startActivity(HelpActivity.createIntent(this, Origin.LOGIN_CONNECTED_EMAIL_HELP, null))
    }
    // endregion
}
